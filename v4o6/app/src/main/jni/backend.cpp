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

static bool timeout;   // 是否超时
static int beatTimer;  // 记录从上一次 beat 起到现在花的时间
static int debugFifoHandle;
static pthread_mutex_t beatTimerMutex;
static pthread_mutex_t socketReadMutex, socketWriteMutex;
static pthread_mutex_t debugFifoMutex;

// 向 Fifo 中写入 Message
void writeMessageToFifo(int fifoHandle, Message message) {
    // 按照大端序(网络字节序)传输
    int len = htonl(message.length);
    write(fifoHandle, &len, 4);
    write(fifoHandle, &message.type, 1);
    int res = message.length - 5, cnt = 0;
    while (res > cnt) {
        int tmp = write(fifoHandle, message.data + cnt, res - cnt);
        if (tmp < 0) break;
        cnt += tmp;
    }
}

// 从 Fifo 中读取 Message
Message readMessageFromFifo(int fifoHandle) {
    Message message;
    read(fifoHandle, &message.length, 4);
    read(fifoHandle, &message.type, 1);
    int res = message.length - 5, cnt = 0;
    while (res > cnt) {
        int tmp = read(fifoHandle, message.data + cnt, res - cnt);
        if (tmp < 0) break;
        cnt += tmp;
    }
    return message;
}

// 从 Socket 中读取 Message
void writeMessageToSocket(int socketFd, Message message) {
    pthread_mutex_lock(&socketWriteMutex);
    send(socketFd, (void*)&message.length, 4, 0);
    send(socketFd, (void*)&message.type, 1, 0);
    int res = message.length - 5, cnt = 0;
    while (res > cnt) {
        int tmp = send(socketFd, message.data + cnt, res - cnt, 0);
        if (tmp < 0) break;
        cnt += tmp;
    }
    pthread_mutex_unlock(&socketWriteMutex);
}

// 向 Socket 中写入 Message
Message readMessageFromSocket(int socketFd, int &cnt) {
    pthread_mutex_lock(&socketReadMutex);
    Message message;
    cnt = 0;
    if (recv(socketFd, &message.length, 4, 0) < 4) {
        printf("读取 length 失败\n");
    }
    if (recv(socketFd, &message.type, 1, 0) < 1) {
        printf("读取 type 失败\n");
    }
    int res = message.length - 5;
    while (cnt < res) {
        int tmp = recv(socketFd, message.data + cnt, res - cnt, 0);
        if (tmp == -1) break;
        cnt += tmp;
    }
    pthread_mutex_unlock(&socketReadMutex);
    return message;
}

// 发送 debug 消息，需要加锁
void writeDebugMessage(Message message) {
    pthread_mutex_lock(&debugFifoMutex);
    writeMessageToFifo(debugFifoHandle, message);
    pthread_mutex_unlock(&debugFifoMutex);
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

// 读取 tun 的线程
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
        //    writeDebugMessage(message);
        }
        // sprintf(tmp, "%d", size);
        // strcpy(info, tmp);
    }
    return NULL;
}

// 定时器线程
void* timerWorker(void *arg) {
    ThreadArg threadArg = *(ThreadArg*)arg;
    int socketFd = threadArg.socketFd;
    while (true) {
        sleep(1);
        pthread_mutex_lock(&beatTimerMutex);
        if (++beatTimer > 60) {
            // 超时
            timeout = true;
            pthread_mutex_unlock(&beatTimerMutex);
            break;
        } else if (beatTimer % 20 == 0) {
            // 向服务器发送心跳包
            Message message;
            message.length = 5;
            message.type = 104;
            writeDebugMessage(message);
            writeMessageToSocket(socketFd, message);
        }
        pthread_mutex_unlock(&beatTimerMutex);
    }
}

// 创建 socket、连接服务器，并请求 IP 地址
// ret 为返回的字符串信息，若失败则对应为失败信息，若成功则对应为 "IP Route DNS DNS DNS" 格式的信息
// 成功时返回 0，失败返回 -1
int init(char *ipv6, int port, char *ipFifoPath, char *tunFifoPath, char *statFifoPath, char *debugFifoPath, char *info) {
    // 创建 socket
    timeout = false;
    int cnt;
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
    message = readMessageFromSocket(socketFd, cnt);
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
        char tmp[] = "打开 ipFifo 管道失败\n";
        strcpy(info, tmp);
        return -1;
    }
    writeMessageToFifo(ipFifoHandle, message);

    // 创建 debugFifo 管道 —— NOTE：注意不要在多个线程中同时对其进行写操作
    if (mkfifo(debugFifoPath, 0666) < 0) {
        char tmp[] = "创建 debugFifo 管道失败\n";
        strcpy(info, tmp);
        return -1;
    }
    if((debugFifoHandle = open(debugFifoPath, O_RDWR | O_CREAT | O_TRUNC)) < 0) {
        char tmp[] = "打开 debugFifo 管道失败\n";
        strcpy(info, tmp);
        return -1;
    }
    writeDebugMessage(message);

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

    // 创建 readTun 线程用于读取 tun0 接口并发送给服务器
    pthread_t readTunThread;
    ThreadArg threadArg;
    threadArg.tunFd = tunFd, threadArg.socketFd = socketFd;
    if (pthread_create(&readTunThread, NULL, &readTun, (void*)&threadArg) != 0) //创建线程
    {
        char tmp[] = "创建线程 readTun 失败\n";
        strcpy(info, tmp);
        return -1;
    }

    // 创建 timerWorker 定时器线程
    pthread_t timerWorkerThread;
    if (pthread_create(&timerWorkerThread, NULL, &timerWorker, (void*)&threadArg) != 0) //创建线程
    {
        char tmp[] = "创建线程 timerWorker 失败\n";
        strcpy(info, tmp);
        return -1;
    }

    while (true) {
        message = readMessageFromSocket(socketFd, cnt);
        pthread_mutex_lock(&beatTimerMutex);
        if (timeout == true) {
            // 超时
            char tmp[] = "与服务器的连接超时";
            strcpy(info, tmp);
            Message message;
            message.type = 107;
            message.length = 5;
            writeDebugMessage(message);
            pthread_mutex_unlock(&beatTimerMutex);
            break;
        }
        pthread_mutex_unlock(&beatTimerMutex);
        if (cnt < message.length-5) {
            sprintf(info, "Error: length: %d, in fact: %d", message.length, cnt + 5);
            break;
        }
        writeDebugMessage(message);
        if (message.type == 103) {
            write(tunFd, (void*)message.data, message.length - 5);
        } else if (message.type == 104) {
            // 收到服务端的心跳包
            pthread_mutex_lock(&beatTimerMutex);
            beatTimer = 0;
            pthread_mutex_unlock(&beatTimerMutex);
        }
        // sprintf(info, "收到 socket 消息, length:%d, type：%d, data: %s", message.length, (int)message.type, message.data);
        // return 0;
    }
    // 暂时直接写在这里

    return 0;
}