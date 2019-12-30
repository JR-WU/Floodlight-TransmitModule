/*
    Author: Vincent Wu
    Time: 2019/12/25
    Project: Video Streaming in UESTC
    Merry Chrismas!
*/

#include <sys/time.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "HTTP_assist.h"
#include <pthread.h>


#define IPSTR "192.168.2.154" //历史视频流服务器的ip地址
#define PORT 10008				   //历史视频流服务器的port
#define BUFSIZE 1024
#define Length_size 6
#define HTTP_GET1 "GET %s HTTP/1.1\r\nHOST: %s\r\nAccept: */*\r\nAccept-Language: zh_CN\r\nUser-Agent: HTTP_UESTC\r\nRange: bytes=0-\r\n\r\n"
#define HTTP_GET2 "GET %s%s HTTP/1.1\r\nHOST: %s\r\nCache-Control: no-cache\r\nUser-Agent: HTTP_UESTC\r\n\r\n"
#define BlockTime 600 // 10 minutes
#define RecevieController 11222

int Buffsize = 0;
//pthread_t Send_pt;
pthread_t pthid[10000];
char recv_buf[20000] = {'\0'};
char *dst_ip = NULL;
int dst_port = 0;
char ReadyForSend[1000] = {'\0'};

struct ParameterToThread
{
    char* dst;
    int port_dst;
    char* url;
};

struct timeval tv_timeout;

//创建一个结构体，用来存储HTTP URL，应为存在不确定性，感觉至少得180.


int  PhraseString(char* buf, int* num)//解析函数，所有值全存在全局变量里
{
	if(buf[0] == '#' && buf[Buffsize-1] == '#')//如果所取字符串前后均为#号，则进行下一步
	{
		//以逗号分割字符串
		memset(ReadyForSend,'\0',sizeof(ReadyForSend));
		dst_ip = NULL;
		dst_port = 0;
        char *revbuf[3] = {NULL};
		split((buf+1),",",revbuf, num);
		dst_ip = revbuf[0];//取出目的ip
		dst_port = atoi_define(revbuf[1]);//取出目的port
        int copylength = Buffsize-strlen(revbuf[0])-strlen(revbuf[1])-4;
		strncpy(ReadyForSend,revbuf[2], copylength);//取出待发送的url,only one url to send to get all urls.
		return 1;
	}
	else{
		printf("Data String is wrong! Please connect the controller adminisrator!");
		return -1;
	}
}

void *ConnectWithHistoryServer(void *arg)
{
	//char* IP_Dst, int Port_Dst, char* url_list
	int sockfd_http,sockfdudp;
	int head_length = 0;
	int Stream_length = 0;
	int segement_length = 0;
    int real_length = 0;
	char *ptr = NULL;
	char de_ptr[200] = {'\0'};
	struct sockaddr_in serveraddr;
	struct sockaddr_in udpaddr;
	char *Receivept = NULL;
    char Send_to_http[1000] = {'\0'};
	char *send_buf[200] = {0};
	char RECVBUF[1500] = {'\0'};
	char temp_send[500] ={'\0'};
	char Receive_BUF[5000] = {'\0'};
	char *response = NULL;

    struct ParameterToThread *inter_para;
    inter_para = (struct ParameterToThread *)arg;
    char* IP_Dst = inter_para->dst;
    int Port_Dst = inter_para->port_dst;
    char* url_list = inter_para->url;

    if(strlen(IP_Dst) <= 1 || Port_Dst == 0 || strnlen(url_list) <= 1)
    {
        printf("Receive failed, Please retry!\n");
        return NULL;
    }

    printf("%s\n", IP_Dst);
    printf("%s\n", url_list);
    printf("%d\n", Port_Dst);

    if ((sockfd_http = socket(AF_INET, SOCK_STREAM, 0)) < 0 ) {
        printf("socket error!\n");
        exit(0);
        };
    if ((sockfdudp = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0 ) {
        printf("socket error!\n");
        exit(0);
        };

    //http socket config
    bzero(&serveraddr, sizeof(serveraddr));
    serveraddr.sin_family = AF_INET;
    serveraddr.sin_port = htons(PORT);
    //udp socket config
    bzero(&udpaddr, sizeof(udpaddr));
    udpaddr.sin_family = AF_INET;
    udpaddr.sin_port = htons(Port_Dst);
    udpaddr.sin_addr.s_addr = inet_addr((const char*)IP_Dst);
    //connect http
    if (inet_pton(AF_INET, IPSTR, &serveraddr.sin_addr) <= 0 ){
            printf("inet_pton error!\n");
            exit(0);
    }
    if (connect(sockfd_http, (struct sockaddr *)&serveraddr, sizeof(serveraddr)) < 0){
            printf("connect error!\n");
            return NULL;
    }

    tv_timeout.tv_sec = BlockTime;//time_out is one minute.
    tv_timeout.tv_usec = 0;
//    setsockopt(sockfd_http,SOL_SOCKET,SO_RCVTIMEO,&tv_timeout, sizeof(tv_timeout));//Set receiving timeout is 1 minute.
    //using loop sending and receiving
    memset(temp_send,'/0',sizeof(temp_send));
    ptr = strchr(url_list+7,'/');//this ptr is what we looking for//!!!!!!!!!!!!!maybe get wrong!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    head_length = Detect_number(url_list);
    strncpy(de_ptr,ptr,head_length+1);//get the common url
    //build HTTP payload.
    sprintf(temp_send,HTTP_GET1,ptr,"UESTC_HTTP_TO_UDP_SERVER");

    if (HTTPSend(sockfd_http,temp_send,strlen(temp_send)) < 0)
    {
        printf("http_send failed..\n");
        close(sockfd_http);
        close(sockfdudp);        
        return NULL;
    }

    if (recv(sockfd_http,Receive_BUF,4000,0) < 0)
    {
        printf("http_RECEVIVE failed,maybe timeout\n");
        close(sockfd_http);
        close(sockfdudp);        
        return NULL;
    }
    response = http_parse_result(Receive_BUF);
    if(strlen(response) != http_parse_Contentlength(Receive_BUF))
    {
        printf("Received data is wrong!,Please check!");
        close(sockfd_http);
        close(sockfdudp);
        return;
    }
    //phease URLs
    Receivept = (char*)strstr(Receive_BUF, ",\n");
    if(Receivept == NULL)
    {
        printf("Received data is not we want!,Please check!");
        close(sockfd_http);
        close(sockfdudp);
        return NULL;
    }
    send_buf[0] = (char*)malloc(500*sizeof(char));
    if(CopySendBuf(send_buf[0],(Receivept+2)) == -1)
    {
        printf("get send data error!");
        close(sockfd_http);
        close(sockfdudp);
        return NULL;
    }
    int SendCounter = 1;
    while(1)
    {
        Receivept = (char*)strstr(Receivept + 2, ",\n");
        if(Receivept == NULL)
        {break;}
        send_buf[SendCounter] = (char*)malloc(500*sizeof(char));
        if(CopySendBuf(send_buf[SendCounter],(Receivept+2)) == -1)
        {
            printf("get send data error!");
            close(sockfd_http);
            close(sockfdudp);
            return NULL;
        }
        SendCounter++;
    }
    for(int k = 0; k < SendCounter; k++)
    {
        memset(Send_to_http,'\0',sizeof(Send_to_http));
        Stream_length = 0;
        segement_length = 0;
        real_length = 0;
        sprintf(Send_to_http,HTTP_GET2,de_ptr,send_buf[k],"UESTC_HTTP_TO_UDP_SERVER");//build send message.
        if (HTTPSend(sockfd_http,Send_to_http,strlen(Send_to_http)) < 0)
        {
            printf("http_send failed..\n");
            close(sockfd_http);
            close(sockfdudp);
            return NULL;
        }
        while(1)
        {
            memset(RECVBUF,'\0',sizeof(RECVBUF));
            response = NULL;
            int ReceiveLength = recv(sockfd_http,RECVBUF,1500,0);
            if(ReceiveLength < 0 )
            {
                printf("http_RECEVIVE failed,maybe timeout\n");
                close(sockfd_http);
                close(sockfdudp);
                return NULL;
            }

            int headerLength = http_parse_result_with_zero(RECVBUF);//Determine if there is an http header in RECVBUF.
            /*if it has a http header*/
            if(headerLength != -1){
                printf("deal with http header\n");
                real_length = http_parse_Contentlength(RECVBUF);
                response =(char *)strstr(RECVBUF, "\r\n\r\n");
                int SendLength  = sendto(sockfdudp,(response+4),(ReceiveLength - headerLength),0,(struct sockaddr *)&udpaddr, sizeof(udpaddr));
                if(SendLength <= 0)
                {
                    perror("the error is:\n");
                    return NULL;
                }
                segement_length = (ReceiveLength - headerLength);
                Stream_length = Stream_length + segement_length;
                if(Stream_length == real_length)
                {break;}
            }
            else
            {
                segement_length = ReceiveLength;
                int SendLength1 = sendto(sockfdudp,RECVBUF,ReceiveLength,0,(struct sockaddr *)&udpaddr, sizeof(udpaddr));
                if(SendLength1 <= 0)
                {                    
                    perror("the error is:\n");
                    return NULL;
                }
                Stream_length = Stream_length + segement_length;
                if(Stream_length == real_length)
                {break;}
            }

        }
    }
    close(sockfd_http);
    close(sockfdudp);
    printf("Send UDP SUCCESSFULLY\n");
    return "Send Success!\n";
}

int main(int argc, char *argv[])
{
 	//创建socket对象
    printf("This is a HTTP_TO_UDP_SERVER bulit by UESTC!\n");
    int sockfd=socket(AF_INET,SOCK_DGRAM,0);
    char Length_buff[Length_size] = {'\0'};
    char change[4] = {'\0'};
    int Start_thread_No = 0;
  	int spilt_num = 0;
    struct ParameterToThread Parameters;
    //创建网络通信对象
    struct sockaddr_in addr,clientaddr;
    int clientaddrlength;
    addr.sin_family = AF_INET;
    addr.sin_port = htons(RecevieController);
    addr.sin_addr.s_addr= htonl(INADDR_ANY);//establish server to receive contorller's messages.

    //绑定socket对象与通信链接
    if(bind(sockfd,(struct sockaddr*)&addr,sizeof(addr)) == -1)
    {
		printf("bind error!");
        return -1;
    }

    while(1)
    {
        memset(Length_buff, '\0', sizeof(Length_buff));
        memset(recv_buf,'\0',sizeof(recv_buf)); //数组清零
        Parameters.dst = NULL;
        Parameters.port_dst = 0;
        Parameters.url = NULL;
        Buffsize = 0;
        if(recvfrom(sockfd,Length_buff,Length_size,0,(struct sockaddr *) &clientaddr, &clientaddrlength) == -1) //首先接收数据头部
        {
        	printf("receive_failed, length_buff");
        	return -1;
        }
        if(Length_buff[0] == '[' && Length_buff[Length_size - 1] == ']')
        {
        	//转换四字节为int，即后面数据的长度
        	// Buffsize = Length_buff[4]+(Length_buff[3]<<8)+(Length_buff[2]<<16)+(Length_buff[1]<<24);
            change[0] = Length_buff[4];
            change[1] = Length_buff[3];
            change[2] = Length_buff[2];
            change[3] = Length_buff[1];
            Buffsize = *(int *)change;
        	//buffsize就是要取的大小
        }else{
        	continue;
        }
        if(recvfrom(sockfd,recv_buf,Buffsize,0,(struct sockaddr *) &clientaddr, &clientaddrlength) == -1) //取出一条指令
        {
        	printf("receive_failed, Buff_size");
        	return -1;
        }
        //解析recv_Buff中的dst_ip和dst_port，分割http存放至URL_List结构体，解析比较简单。
        PhraseString(recv_buf,&spilt_num);
        //只须一个线程，负责Send和Recv.
        Parameters.dst = dst_ip;
        Parameters.port_dst = dst_port;
        Parameters.url = ReadyForSend;
        pthread_create(&pthid[Start_thread_No],NULL,ConnectWithHistoryServer,&(Parameters));//establish a new thread.
        usleep(50);//Let Parameters get into the threads.
        Start_thread_No++;
        if(Start_thread_No == 9999)
        {
            Start_thread_No = 0;
        }
        //Send_HTTP(URL list);//采用结构体形式可能比较简单。
    }
    close(sockfd);
}