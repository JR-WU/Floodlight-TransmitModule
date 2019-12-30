#ifndef HTTP_ASSIST_H_
#define HTTP_ASSIST_H_
#endif

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

void split(char *src,const char *separator,char **dest,int *num);

int atoi_define(char s[]);

int HTTPRecv(int socket, char *lpbuff, int BUFFER_SIZE);

int HTTPSend(int socket, char *buff, int size);

char *http_parse_result(const char*lpbuf);

int http_parse_Contentlength(char*lpbuf);

int Detect_number(char* URL);

int CopySendBuf(char *dst, char *src);

int CopyContentLength(char *dst, char *src);

int http_parse_result_with_zero(const char*lpbuf);
