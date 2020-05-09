#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <cstdio>
#include <pthread.h>
#include <unistd.h>
#include <limits.h>
#include <fcntl.h>
#include "message.h"
#include "utils.cpp"

void writeMessageToSocket(int socketFd, Message message) {
    send(socketFd, (void*)&message.length, 4, 0);
    send(socketFd, (void*)&message.type, 4, 0);
    send(socketFd, (void*)message.data, message.length-5, 0);
}

Message readMessageFromSocket(int socketFd) {
    Message message;
    if (recv(socketFd, &message.length, 4, 0) < 4) {
        printf("读取 length 失败\n");
    }
    if (recv(socketFd, &message.type, 1, 0) < 1) {
        printf("读取 type 失败\n");
    }
    int res = message.length - 5;
    if (recv(socketFd, message.data, res, 0) < res) {
        printf("读取 data 失败\n");
    }
    return message;
}

// 主循环线程
void mainLoop() {
    while (true) {

    }
}

struct ThreadArg {
    int tunFd;
    int socketFd;
};

void* readTun(void *arg) {
    ThreadArg threadArg = *(ThreadArg*)arg;
    int tunFd = threadArg.tunFd;
    int socketFd = threadArg.socketFd;
    Message message;
    while (true) {
        char tmp[4096];
        int size = read(tunFd, tmp, 2000);
        if (size > 0) {
            message.length = 5 + size;
            message.type = 102;
            for (int i = 0; i < size; ++i) message.data[i] = tmp[i];
            writeMessageToSocket(socketFd, message);
        }
        // sprintf(tmp, "%d", size);
        // strcpy(info, tmp);
    }
    return NULL;
}

void writeMessageToFifo(int fifoHandle, Message message) {
    // 按照大端序(网络字节序)传输
    int len = htonl(message.length);
    write(fifoHandle, &len, 4);
    write(fifoHandle, &message.type, 1);
    write(fifoHandle, message.data, message.length - 5);
}

// 从 Fifo 中读取 Message
Message readMessageFromFifo(int fifoHandle) {
    Message message;
    read(fifoHandle, &message.length, 4);
    read(fifoHandle, &message.type, 1);
    read(fifoHandle, message.data, message.length - 5);  // TODO：有可能崩溃
    return message;
}

// 创建 socket、连接服务器，并请求 IP 地址
// ret 为返回的字符串信息，若失败则对应位失败信息，若成功则对应为 "IP Route DNS DNS DNS" 格式的信息
// 成功时返回 0，失败返回 -1
int init(char *ipv6, int port, char *ipFifoPath, char *tunFifoPath, char *statFifoPath, char *info) {
    // 创建 socket
    int socketFd = socket(AF_INET6, SOCK_STREAM, 0);
    if (socketFd < 0) {
        char tmp[] = "创建 socket 失败";
        printf("%s\n", tmp);
        strcpy(info, tmp);
        return -1;
    }
    printf("创建 socket, socketFd: %d\n", socketFd);
    
    // 连接服务器
    sockaddr_in6 saddr6 = utils::getSockaddr6(const_cast<char*>(ipv6), port);
    int ret;
    if ((ret=connect(socketFd, (sockaddr*)(&saddr6), sizeof(saddr6))) < 0) {
        char tmp[] = "连接服务器失败，请确认拥有 ipv6 网络\n";
        strcpy(info, tmp);
        return -1;
    } else {
        printf("连接服务器成功：%s %d\n", ipv6, port);
    }
    Message message;
    message.type = 100;
    message.length = sizeof(message);
    writeMessageToSocket(socketFd, message);
    /*int num = send(socketFd, (void*)&message, message.length, 0);
    if (num < message.length) {
        char tmp[] = "向服务器发送 IP 地址请求失败\n";
        strcpy(info, tmp);
        return -1;
    }*/
    message = readMessageFromSocket(socketFd);
    if (message.type != 101) {
        char tmp[] = "IP 地址响应格式错误\n";
        strcpy(info, tmp);
        return -1;
    }
    printf("IP 地址请求成功:\n");
    char tmp[4096];

    // 将 protectedFd 加入到 message 中，注意到 message.data 收到时结尾有个空格
    sprintf(tmp, "%s%d", message.data, socketFd);
    strcpy(message.data, tmp);
    message.length = strlen(message.data) + 5;
    message.print();

    // DEBUG: 如果已经存在文件了，则 mkfifo 将返回 -1
    /*struct stat stats;
    if (stat(fifo_path, &stats) >= 0) {
        if (unlink(fifo_path) < 0) {
            char tmp[] = "清理管道文件失败\n";
            strcpy(info, tmp);
            return -1;
        }
    }*/

    // 将 ipv4, route, dns 等信息写入 ipFifo 管道
    if (mkfifo(ipFifoPath, 0666) < 0) {
        char tmp[] = "创建 ipFifo 管道失败\n";
        strcpy(info, tmp);
        return -1;
    }
    int ipFifoHandle;
    if((ipFifoHandle = open(ipFifoPath, O_RDWR | O_CREAT | O_TRUNC)) < 0) {
        char tmp[] = "打开管道文件失败\n";
        strcpy(info, tmp);
        return -1;
    }
    writeMessageToFifo(ipFifoHandle, message);

    // 读取 tunFifo 管道获得 tun 描述符
    if (mkfifo(tunFifoPath, 0666) < 0) {
        char tmp[] = "创建 tunFifo 管道失败\n";
        strcpy(info, tmp);
        return -1;
    }
    int tunFifoHandle;
    if ((tunFifoHandle = open(tunFifoPath, O_RDONLY)) < 0) {
        char tmp[] = "打开 tunFifo 失败\n";
        strcpy(info, tmp);
        return -1;
    }
    message = readMessageFromFifo(tunFifoHandle);
    close(tunFifoHandle);
    int tunFd;
    sscanf(message.data, "%d", &tunFd);
    close(ipFifoHandle);
    close(tunFifoHandle);

    // std::thread t(mainLoop);
    printf("进入主循环...\n\n");
    // strcpy(info, message.data);

    pthread_t readTunThread;
    ThreadArg threadArg;
    threadArg.tunFd = tunFd, threadArg.socketFd = socketFd;
    if (pthread_create(&readTunThread, NULL, &readTun, (void*)&threadArg) != 0) //创建线程
    {
        char tmp[] = "创建线程 readTun 失败\n";
        strcpy(info, tmp);
        return -1;
    }

    while (true) {
        message = readMessageFromSocket(socketFd);
        sprintf(info, "收到 socket 消息, length:%d, type：%d, data: %s", message.length, (int)message.type, message.data);
        return 0;
    }
    // 暂时直接写在这里

    return 0;
}