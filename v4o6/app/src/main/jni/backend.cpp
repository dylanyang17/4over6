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
#include <errno.h>
#include "utils.cpp"

static bool timeout;   // 是否超时，同时也用来控制关闭线程
static int beatTimer;  // 记录从上一次 beat 起到现在花的时间
static int debugFifoHandle;
static pthread_mutex_t beatTimerMutex;
static pthread_mutex_t socketReadMutex, socketWriteMutex;
static pthread_mutex_t debugFifoMutex, uploadMutex, downloadMutex;
static int uploadBytes, uploadPackages, downloadBytes, downloadPackages;

// 向 Fifo 中写入 Message
void writeMessageToFifo(int fifoHandle, Message message) {
    // 按照大端序(网络字节序)传输
    int len = htonl(message.length);
    write(fifoHandle, &len, 4);
    write(fifoHandle, &message.type, 1);
    int res = message.length - 5, cnt = 0;
    while (res > cnt) {
        int tmp = write(fifoHandle, message.data + cnt, res - cnt);
        if (tmp <= 0) break;
        cnt += tmp;
    }
}

// 从 Fifo 中读取 Message
Message readMessageFromFifo(int fifoHandle, bool &suc) {
    Message message;
    suc = true;
    if (read(fifoHandle, &message.length, 4) < 4) {
        // 理应是文件结尾
        message.length = message.type = 0;
        suc = false;
        return message;
    }
    read(fifoHandle, &message.type, 1);
    int res = message.length - 5, cnt = 0;
    while (res > cnt) {
        int tmp = read(fifoHandle, message.data + cnt, res - cnt);
        if (tmp <= 0) { suc = false; break; }
        cnt += tmp;
    }
    return message;
}

// 发送 debug 消息，需要加锁
void writeDebugMessage(Message message) {
    pthread_mutex_lock(&debugFifoMutex);
    writeMessageToFifo(debugFifoHandle, message);
    pthread_mutex_unlock(&debugFifoMutex);
}

// 发送 107 类型的 debug 消息，用于发送自定义的字符串信息
void writeDebugMessage(const char *info) {
    Message message;
    strcpy(message.data, info);
    message.type = 107;
    message.length = 5 + strlen(message.data);
    writeDebugMessage(message);
}

// 模拟阻塞行为，并加入对 timeout 的判断
bool writeSingleToSocket(int socketFd, char *data, int len) {
    int cnt = 0;
    char ttt[100];
    // sprintf(ttt, "写 socket single, len: %d", len);
    // writeDebugMessage(ttt);
    while (len > cnt) {
        // 判断 timeout
        pthread_mutex_lock(&beatTimerMutex);
        if (timeout) {
            pthread_mutex_unlock(&beatTimerMutex);
            return false;
        }
        pthread_mutex_unlock(&beatTimerMutex);

        int tmp = send(socketFd, data + cnt, len - cnt, 0);
        if (tmp > 0) {
            cnt += tmp;
        } else if (errno == EINTR || errno == EWOULDBLOCK || errno == EAGAIN) {
            sprintf(ttt, "重写 errno: %d", errno);
            writeDebugMessage(ttt);
            continue;
        } else {
            return false;
        }
    }
    return true;
}

// 从 Socket 中读取 Message
// 注意 Socket 为非阻塞形式
void writeMessageToSocket(int socketFd, Message message) {
    pthread_mutex_lock(&socketWriteMutex);
    writeSingleToSocket(socketFd, (char*)&message.length, 4);
    writeSingleToSocket(socketFd, (char*)&message.type, 1);
    // send(socketFd, &message.length, 4, 0);
    // send(socketFd, &message.type, 1, 0);
    if (message.length - 5 > 0) {
        writeSingleToSocket(socketFd, message.data, message.length - 5);
        // send(socketFd, message.data, message.length-5, 0);
    }
    pthread_mutex_unlock(&socketWriteMutex);
}

// 模拟阻塞行为，并加入对 timeout 的判断
bool readSingleFromSocket(int socketFd, char *data, int len) {
    int cnt = 0;
    while (len > cnt) {
        // 判断 timeout
        pthread_mutex_lock(&beatTimerMutex);
        if (timeout) {
            pthread_mutex_unlock(&beatTimerMutex);
            return false;
        }
        pthread_mutex_unlock(&beatTimerMutex);

        int tmp = recv(socketFd, data + cnt, len - cnt, 0);
        if (tmp > 0) {
            cnt += tmp;
        } else if (errno == EINTR || errno == EWOULDBLOCK || errno == EAGAIN) {
            continue;
        } else {
            return false;
        }
    }
    return true;
}

// 向 Socket 中写入 Message
// 注意 Socket 为非阻塞形式
Message readMessageFromSocket(int socketFd, bool &suc) {
    pthread_mutex_lock(&socketReadMutex);
    Message message;
    int cnt = 0;
    suc = true;
    if (!readSingleFromSocket(socketFd, (char*)&message.length, 4)) {
    // if (recv(socketFd, &message.length, 4, 0) < 4) {
        printf("读取 length 失败\n");
        suc = false;
        pthread_mutex_unlock(&socketReadMutex);
        return message;
    }
    char tmp[100];
    sprintf(tmp, "message.length: %d", message.length);
    writeDebugMessage(tmp);
    if (!readSingleFromSocket(socketFd, (char*)&message.type, 1)) {
    // if (recv(socketFd, &message.type, 1, 0) < 1) {
        printf("读取 type 失败\n");
        suc = false;
    }
    if (message.length - 5 > 0) {
        if (!readSingleFromSocket(socketFd, message.data, message.length-5)) {
        // if (recv(socketFd, message.data, message.length-5, 0) < message.length-5) {
            printf("读取 data 失败\n");
            suc = false;
        }
    }
    pthread_mutex_unlock(&socketReadMutex);
    return message;
}

/*
// 从 Socket 中读取 Message
void writeMessageToSocket(int socketFd, Message message) {
    pthread_mutex_lock(&socketWriteMutex);
    send(socketFd, (void*)&message.length, 4, 0);
    send(socketFd, (void*)&message.type, 1, 0);
    int res = message.length - 5, cnt = 0;
    while (res > cnt) {
        int tmp = send(socketFd, message.data + cnt, res - cnt, 0);
        if (tmp <= 0) break;
        cnt += tmp;
    }
    pthread_mutex_unlock(&socketWriteMutex);
}

// 向 Socket 中写入 Message
Message readMessageFromSocket(int socketFd, bool &suc) {
    pthread_mutex_lock(&socketReadMutex);
    Message message;
    int cnt = 0;
    suc = true;
    if (recv(socketFd, &message.length, 4, 0) < 4) {
        printf("读取 length 失败\n");
        suc = false;
    }
    if (recv(socketFd, &message.type, 1, 0) < 1) {
        printf("读取 type 失败\n");
        suc = false;
    }
    int res = message.length - 5;
    while (cnt < res) {
        int tmp = recv(socketFd, message.data + cnt, res - cnt, 0);
        if (tmp <= 0) { suc = false; break; }
        cnt += tmp;
    }
    pthread_mutex_unlock(&socketReadMutex);
    return message;
}*/

struct ThreadArg {
    int tunFd;
    int socketFd;
    int statFifoHandle;
};

// 读取 tun 的线程
void* readTun(void *arg) {
    ThreadArg threadArg = *(ThreadArg*)arg;
    int tunFd = threadArg.tunFd;
    int socketFd = threadArg.socketFd;
    Message message;
    while (true) {
        char tmp[4096];
        int bytes = read(tunFd, tmp, 2000);
        pthread_mutex_lock(&beatTimerMutex);
        if (timeout) {
            pthread_mutex_unlock(&beatTimerMutex);
            break;
        }
        pthread_mutex_unlock(&beatTimerMutex);
        if (bytes > 0) {
            // 检查到有发往 tun0 的消息，转发给服务器
            pthread_mutex_lock(&uploadMutex);
            uploadPackages++;
            uploadBytes += bytes;
            pthread_mutex_unlock(&uploadMutex);
            message.length = 5 + bytes;
            message.type = 102;
            for (int i = 0; i < bytes; ++i) message.data[i] = tmp[i];
            writeMessageToSocket(socketFd, message);
        //    writeDebugMessage(message);
        }
        // sprintf(tmp, "%d", size);
        // strcpy(info, tmp);
    }
    writeDebugMessage("切断后台 readTun 线程");
    return NULL;
}

// 定时器线程
void* timerWorker(void *arg) {
    ThreadArg threadArg = *(ThreadArg*)arg;
    int socketFd = threadArg.socketFd, statFifoHandle = threadArg.statFifoHandle;
    while (true) {
        sleep(1);
        pthread_mutex_lock(&beatTimerMutex);
        ++beatTimer;
        char tmp[100];
        writeDebugMessage(tmp);
        if (timeout) {
            pthread_mutex_unlock(&beatTimerMutex);
            break;
        }
        if (beatTimer > 60) {
            // 超时
            timeout = true;
            pthread_mutex_unlock(&beatTimerMutex);
            break;
        } else if (beatTimer % 20 == 0) {
            // 向服务器发送心跳包
            pthread_mutex_unlock(&beatTimerMutex);
            Message message;
            message.length = 5;
            message.type = 104;
            writeDebugMessage(message);
            writeMessageToSocket(socketFd, message);
        } else {
            pthread_mutex_unlock(&beatTimerMutex);
        }

        // 发送统计信息
        pthread_mutex_lock(&uploadMutex);
        pthread_mutex_lock(&downloadMutex);
        Message message;
        sprintf(message.data, "%d %d %d %d", uploadBytes, uploadPackages, downloadBytes, downloadPackages);
        message.type = 106;
        message.length = strlen(message.data) + 5;
        writeMessageToFifo(statFifoHandle, message);
        uploadBytes = uploadPackages = downloadBytes = downloadPackages = 0;
        pthread_mutex_unlock(&downloadMutex);
        pthread_mutex_unlock(&uploadMutex);
    }
    writeDebugMessage("切断后台 timerWorker 线程");
}

// 前台对后台的控制器线程，利用 tunFifo 实现
void* FBController(void *arg) {
    bool suc;
    char *FBFifoPath = (char*)arg;
    writeDebugMessage("运行 FBController");
    // 读取 tunFifo 管道获得 tun 描述符
    if (mkfifo(FBFifoPath, 0666) < 0) {
        writeDebugMessage("创建 FBFifo 管道失败");
        return NULL;
    }
    int FBFifoHandle;
    if ((FBFifoHandle = open(FBFifoPath, O_RDONLY)) < 0) {
        writeDebugMessage("打开 FBFifo 失败");
        return NULL;
    }
    while (true) {
        sleep(0.2);
        Message message = readMessageFromFifo(FBFifoHandle, suc);
        if (suc && message.type == 108) {
            pthread_mutex_lock(&beatTimerMutex);
            timeout = true;
            pthread_mutex_unlock(&beatTimerMutex);
            writeDebugMessage("切断后台 FBController 线程");
            break;
        }
    }
    return NULL;
}

// 创建 socket、连接服务器，并请求 IP 地址
// ret 为返回的字符串信息，若失败则对应为失败信息，若成功则对应为 "IP Route DNS DNS DNS" 格式的信息
// 成功时返回 0，失败返回 -1
int init(char *ipv6, int port, char *ipFifoPath, char *tunFifoPath, char *statFifoPath, char *debugFifoPath, char *FBFifoPath, char *info, int &socketFd) {
    // 创建 socket
    bool suc;
    timeout = false;
    uploadBytes = uploadPackages = downloadBytes = downloadPackages = 0;
    int cnt;

    // 创建 debugFifo 管道
    if (mkfifo(debugFifoPath, 0666) < 0) {
        char tmp[] = "创建 debugFifo 管道失败";
        strcpy(info, tmp);
        return -1;
    }
    if((debugFifoHandle = open(debugFifoPath, O_RDWR | O_CREAT | O_TRUNC)) < 0) {
        char tmp[] = "打开 debugFifo 管道失败";
        strcpy(info, tmp);
        return -1;
    }

    writeDebugMessage("debug 启动");

    socketFd = socket(AF_INET6, SOCK_STREAM, 0);
    if (socketFd < 0) {
        char tmp[] = "创建 socket 失败";
        printf("%s\n", tmp);
        strcpy(info, tmp);
        return -1;
    }
    printf("创建 socket, socketFd: %d\n", socketFd);

    char tmp[4096];
    sprintf(tmp, "socket 创建成功：%d", socketFd);
    writeDebugMessage(tmp);
    
    // 连接服务器
    sockaddr_in6 saddr6 = utils::getSockaddr6(const_cast<char*>(ipv6), port);
    int ret;
    if ((ret=connect(socketFd, (sockaddr*)(&saddr6), sizeof(saddr6))) < 0) {
        char tmp[] = "连接服务器失败，请确认拥有 ipv6 网络\n";
        strcpy(info, tmp);
        return -1;
    } else {
        printf("连接服务器成功：%s %d\n", ipv6, port);
        writeDebugMessage("连接服务器成功");
    }

    // 将 socket 设置为非阻塞
    int flags = fcntl(socketFd, F_GETFL, 0);
    if(fcntl(socketFd, F_SETFL, flags|O_NONBLOCK) < 0) {
        char tmp[] = "设置为非阻塞失败";
        strcpy(info, tmp);
        return -1;
    } else {
        writeDebugMessage("设置为非阻塞成功");
    }

    Message message;
    message.type = 100;
    message.length = 5;
    writeMessageToSocket(socketFd, message);
    writeDebugMessage("向 socket 写入 ip 请求");

    message = readMessageFromSocket(socketFd, suc);
    if (message.type != 101) {
        char tmp[100];
        sprintf(tmp, "IP. length: %d, type: %d, data: %s", message.length, (int)message.type, message.data);
        strcpy(info, tmp);
        return -1;
    }
    printf("IP 地址请求成功:\n");
    writeDebugMessage("ip 地址请求成功");

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

    // 创建 statFifo 管道
    int statFifoHandle;
    if (mkfifo(statFifoPath, 0666) < 0) {
        char tmp[] = "创建 statFifo 管道失败";
        strcpy(info, tmp);
        return -1;
    }
    if((statFifoHandle = open(statFifoPath, O_RDWR | O_CREAT | O_TRUNC)) < 0) {
        char tmp[] = "打开 statFifo 管道失败";
        strcpy(info, tmp);
        return -1;
    }

    writeDebugMessage(message);

    // 读取 tunFifo 管道获得 tun 描述符
    if (mkfifo(tunFifoPath, 0666) < 0) {
        char tmp[] = "创建 tunFifo 管道失败";
        strcpy(info, tmp);
        return -1;
    }
    int tunFifoHandle;
    if ((tunFifoHandle = open(tunFifoPath, O_RDONLY)) < 0) {
        char tmp[] = "打开 tunFifo 失败";
        strcpy(info, tmp);
        return -1;
    }
    message = readMessageFromFifo(tunFifoHandle, suc);
    if (!suc) {
        char tmp[] = "读取 tun 失败";
        strcpy(info, tmp);
        return -1;
    }
    int tunFd;
    sscanf(message.data, "%d", &tunFd);
    close(ipFifoHandle);
    close(tunFifoHandle);

    // std::thread t(mainLoop);
    printf("进入主循环...");
    // strcpy(info, message.data);

    // 创建 readTun 线程用于读取 tun0 接口并发送给服务器
    pthread_t readTunThread;
    ThreadArg threadArg;
    threadArg.tunFd = tunFd, threadArg.socketFd = socketFd, threadArg.statFifoHandle = statFifoHandle;
    if (pthread_create(&readTunThread, NULL, &readTun, (void*)&threadArg) != 0)
    {
        char tmp[] = "创建线程 readTun 失败";
        strcpy(info, tmp);
        return -1;
    }

    // 创建 timerWorker 定时器线程
    pthread_t timerWorkerThread;
    if (pthread_create(&timerWorkerThread, NULL, &timerWorker, (void*)&threadArg) != 0)
    {
        char tmp[] = "创建线程 timerWorker 失败";
        strcpy(info, tmp);
        return -1;
    }

    // 创建 FBController 前端到后端的控制线程
    pthread_t FBControllerThread;
    if (pthread_create(&FBControllerThread, NULL, &FBController, (void*)FBFifoPath) != 0)
    {
        char tmp[] = "创建线程 FBController 失败";
        strcpy(info, tmp);
        return -1;
    }

    int failCnt = 0;  // 读取 socket 的连续失败次数
    while (true) {
        message = readMessageFromSocket(socketFd, suc);
        pthread_mutex_lock(&beatTimerMutex);
        if (timeout == true) {
            // 超时
            char tmp[] = "与服务器的连接超时";
            pthread_mutex_unlock(&beatTimerMutex);
            strcpy(info, tmp);
            Message message;
            message.type = 107;
            message.length = 5;
            message.data[0] = '\0';
            writeDebugMessage(message);
            break;
        }
        pthread_mutex_unlock(&beatTimerMutex);
        if (suc) {
            failCnt = 0;
            writeDebugMessage(message);
            if (message.type == 103) {
                // 收到服务端的访问响应
                pthread_mutex_lock(&downloadMutex);
                int bytes = message.length - 5;
                if (bytes < 0 || bytes > 4096) {
                    // 不应当出现
                    writeDebugMessage("socket 读取长度出错");
                }
                downloadPackages++;
                downloadBytes += bytes;
                pthread_mutex_unlock(&downloadMutex);
                write(tunFd, (void*)message.data, bytes);
            } else if (message.type == 104) {
                // 收到服务端的心跳包
                pthread_mutex_lock(&beatTimerMutex);
                beatTimer = 0;
                pthread_mutex_unlock(&beatTimerMutex);
            }
        } else {
            writeDebugMessage("读取 socket 失败");
            if (++failCnt == 10) {
                // 连续超过十次失败，认为已经断开连接
                pthread_mutex_lock(&beatTimerMutex);
                timeout = 1;
                pthread_mutex_unlock(&beatTimerMutex);
            }
        }
        // sprintf(info, "收到 socket 消息, length:%d, type：%d, data: %s", message.length, (int)message.type, message.data);
        // return 0;
    }
    // 暂时直接写在这里
    writeDebugMessage("3秒后切断后台主线程");
    // 关闭 DebugRunnable
    sleep(2);
    message.length = 5;
    message.type = 109;
    writeDebugMessage(message);
    sleep(1);
    close(debugFifoHandle);
    close(socketFd);
    strcpy(info, "断开连接成功");
    return 0;
}