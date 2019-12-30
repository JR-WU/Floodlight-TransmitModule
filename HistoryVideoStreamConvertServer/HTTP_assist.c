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

void split(char *src,const char *separator,char **dest,int *num) {
	/*
		src 源字符串的首地址(buf的地址)
		separator 指定的分割字符
		dest 接收子字符串的数组
		num 分割后子字符串的个数
	*/
     char *pNext;
     int count = 0;
     if (src == NULL || strlen(src) == 0) //如果传入的地址为空或长度为0，直接终止
        return;
     if (separator == NULL || strlen(separator) == 0) //如未指定分割的字符串，直接终止
        return;
     pNext = (char *)strtok(src,separator); //必须使用(char *)进行强制类型转换(虽然不写有的编译器中不会出现指针错误)
     while(pNext != NULL) {
          *dest++ = pNext;
          ++count;
         pNext = (char *)strtok(NULL,separator);  //必须使用(char *)进行强制类型转换
    }
    *num = count;
}

/*将字符串s转换成相应的整数*/
int atoi_define(char s[])
{
    int i;
    int n = 0;
    for (i = 0; s[i] >= '0' && s[i] <= '9'; ++i)
    {
        n = 10 * n + (s[i] - '0');
    }
    return n;
}

int HTTPRecv(int socket, char *lpbuff, int BUFFER_SIZE)
{
	int recvnum = 0;

	recvnum = recv(socket, lpbuff, BUFFER_SIZE*4, 0);

	return recvnum;
}

int HTTPSend(int socket, char *buff, int size)
{
	int sent=0, tmpres=0;

	while (sent < size) {
		tmpres = send(socket, buff+sent, size-sent, 0);
		if(tmpres == -1){
			return -1;
		}
		sent += tmpres;
	}
	return sent;
}

char *http_parse_result(const char*lpbuf)
{
	char Status[3] = {'\0'};
	char *ptmp = NULL;
	char *response = NULL;
	ptmp = (char*)strstr(lpbuf, "HTTP/1.1");
	if (!ptmp) {
		printf("http/1.1 not faind\n");
		return NULL;
	}
	/*maybe it's wrong!*/
	for(int i = 0;i<3;i++)
	{
        Status[i] = *(ptmp+(9+i));
	}

	if (atoi(Status) != 200 && atoi(Status) != 206 ){
		printf("result:\n%s\n", lpbuf);
		return NULL;
	}

	ptmp = (char *)strstr(lpbuf, "\r\n\r\n");
	if (!ptmp) {
		printf("ptmp is NULL\n");
		return NULL;
	}
    printf("the response length is %d\n",strlen(ptmp+4));
	response = (char *)malloc(strlen(ptmp+4) + 1);
	if (!response) {
		printf("malloc failed \n");
		return NULL;
	}
	strcpy(response, ptmp+4);
	return response;
}

int http_parse_result_with_zero(const char*lpbuf)
{
	char Status[3] = {'\0'};
	char *ptmp = NULL;
    char *toolman = NULL;
	char *response = NULL;
    toolman = lpbuf;
	ptmp = (char*)strstr(lpbuf, "HTTP/1.1");
	if (!ptmp) {
		printf("http/1.1 not found\n");
		return -1;
	}
	/*maybe it's wrong!*/
	for(int i = 0;i<3;i++)
	{
        Status[i] = *(ptmp+(9+i));
	}

	if (atoi(Status) != 200 && atoi(Status) != 206 ){
		printf("result:\n%s\n", lpbuf);
		return -1;
	}

	ptmp = (char *)strstr(lpbuf, "\r\n\r\n");
	if (!ptmp) {
		printf("ptmp is NULL\n");
		return -1;
	}
    return ((ptmp+4) - toolman);
}

int http_parse_Contentlength(char*lpbuf)
{
    char *Length = NULL;
    char *ptmp = NULL;
    int Length_of_Content = 0;
    ptmp = (char *)strstr(lpbuf, "Content-Length: ");
	if(!ptmp){
        printf("Content-Length not found\n");
        return -1;
	}
    Length = (char*)malloc(sizeof(char)*30);
    if(CopyContentLength(Length,(ptmp+16)) == -1)
    {
        printf("get ContentLength data error!");
        return -1;
    }

    Length_of_Content = atoi_define(Length);
    return Length_of_Content;
}

int Detect_number(char* URL)
{
    char* ptmp = NULL;
    ptmp = (char*)strstr(URL, "http://");
    if(ptmp != NULL)
    {
        ptmp = ptmp + 7;
    }
    ptmp = strchr(ptmp,'/');
    int counter = 0 ;
    int maincounter = 0;
    while(1)
    {
        ptmp++;
        if(*(ptmp) == '/')
        {
            maincounter++;
        }
        counter++;
        if(maincounter == 4)
        {break;}
    }
    return counter;

}

int CopySendBuf(char *dst, char *src)
{
    char *toolman = NULL;
    toolman = strchr(src,'\n');
    int number = 0;
    if(toolman == NULL)
    {
        printf("there is no change line symbol, Please check");
        return -1;
    }
    else
    {
        for(int counter = 0;;counter++)
        {
            if(*(src+counter) == '\n')
            {
                break;
            }
            number++;
        }
        strncpy(dst,src,number);//copy url to dst.
        return 1;
    }
    
}

int CopyContentLength(char *dst, char *src)
{
    char *toolman = NULL;
    int number = 0;
    toolman = strstr(src,"\r\n");
    if(toolman == NULL)
    {
        printf("there will be endless, Please check");
        return -1;
    }
    else
    {
        for(int counter = 0;;counter++)
        {
            if(*(src+counter) == '\r' && *(src+counter+1) == '\n')
            {
                break;
            }
            number++;
        }
        strncpy(dst,src,number);//copy url to dst.
        return 1;
    }
    
}