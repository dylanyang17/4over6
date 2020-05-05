#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <cstdio>
#include "message.h"
#include "utils.cpp"

Message readMessage(int fd) {
    Message message;
    if (recv(fd, &message.length, 4, 0) < 4) {
        printf("读取 length 失败\n");
        exit(0);
    }
    if (recv(fd, &message.type, 1, 0) < 1) {
        printf("读取 type 失败\n");
        exit(0);
    }
    int res = message.length - 5;
    if (recv(fd, message.data, res, 0) < res) {
        printf("读取 data 失败\n");
        exit(0);
    }
    return message;
}

// 创建 socket、连接服务器，并请求 IP 地址
void init(char *ipv6, int port) {
    // 创建 socket
    int fd = socket(AF_INET6, SOCK_STREAM, 0);
    if (fd < 0) {
        printf("创建 socket 失败\n");
        exit(0);
    }
    printf("创建 socket, fd: %d\n", fd);
    
    // 连接服务器
    sockaddr_in6 saddr6 = utils::getSockaddr6(const_cast<char*>(ipv6), port);
    int ret;
    if ((ret=connect(fd, (sockaddr*)(&saddr6), sizeof(saddr6))) < 0) {
        printf("连接服务器失败，请确认拥有 ipv6 网络\n", ret);
        exit(0);
    } else {
        printf("连接服务器成功：%s %d\n", ipv6, port);
    }
    Message message;
    message.type = 100;
    message.length = sizeof(message);
    int num = send(fd, (void*)&message, message.length, 0);
    if (num < message.length) {
        printf("向服务器发送 IP 地址请求失败\n");
        exit(0);
    }
    message = readMessage(fd);
    printf("IP 地址请求成功:\n");
    message.print();
    printf("进入主循环...\n\n");
}

int main() {
    const char ipv6[] = "2402:f000:4:72:808::9a47";
    const int PORT = 5678;
    init(const_cast<char*>(ipv6), PORT);
    
    return 0;
}