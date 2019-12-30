#CC = mipsel-openwrt-linux-gcc
#CC = arm-linux-gnueabihf-gcc
#CC = arm-linux-gcc	
CC = gcc
TARGET = HTTP_TO_UDP_SERVER
OBJECT = HTTP_assist.o HTTP_TO_UDP_SERVER.o 
#CFLAGS = -D_REENTRANT -DDEBUG -DDEBUG_PRINT -g -Wall
CFLAGS = -D_REENTRANT -DDEBUGON -g -Wall -std=c99 -lpthread
LDFLAGS = -L/usr/local/lib
RM = rm -f

$(TARGET):$(OBJECT)
	$(CC) -o $@ $^ $(CFLAGS) $(LDFLAGS) 
$(OBJECT):%.o:%.c
	$(CC) -c $(CFLAGS) $(LDFLAGS) $< -o $@

clean:
	$(RM) $(TARGET) $(OBJECT)
