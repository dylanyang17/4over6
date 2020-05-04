#include <cassert>
#include <cstdio>
#include <cstring>
#include <cstdio>
#include <algorithm>
#include <inttypes.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>

namespace utils {
    // 将单个 16 进制字符转换为 16 进制数
    uint8_t hex2dec(char c) {
        if (c >= '0' && c <= '9')
            return c - '0';
        else if (c >= 'a' && c <= 'f')
            return c - 'a' + 10;
        else if (c >= 'A' && c <= 'F')
            return c - 'A' + 10;
        else assert(0);
    }

    // 输出小端序（主机序）的 ipv6
    void printV6(uint16_t *addr) {
        for (int i = 0; i < 8; ++i) {
            printf("%x%s", (int)addr[i], ((i==7) ? "" : ":"));
        }
        printf("\n");
    }

    // 单个 uint16_t 的大小端转换
    uint16_t switchEndian(uint16_t num) {
        int tmp = (num & ((1<<8)-1));
        num = (num >> 8) | (tmp << 8);
        return num;
    }

    // ipv6 的大小端转换
    void switchEndian(uint16_t *addr) {
        for (int i = 0; i <= 3; ++i) {
            std::swap(addr[i], addr[7-i]);
            addr[i] = switchEndian(addr[i]);
            addr[7-i] = switchEndian(addr[7-i]);
        }
    }
    
    // 将 ipv6 的字符串转为 uint16_t 数组
    void v62uint16(char *ipv6, uint16_t *addr) {
        int singleColonCnt = 0, doubleColonPos = -1;
        int len = strlen(ipv6);
        memset (addr, 0, 128/8);  // 清空
        // 扫描得到单冒号数量以及双冒号位置（即双冒号前面有几个单冒号）
        for (int i = 0; i < len; ++i) {
            if (ipv6[i] == ':') {
                if (i < len-1 && ipv6[i+1] == ':') {
                    // 双冒号
                    doubleColonPos = singleColonCnt;
                    ++i;  // 避免后面的冒号被认为单冒号
                } else {
                    // 单冒号
                    singleColonCnt++;
                }
            }
        }
        // 扫描得到各段的数
        uint16_t num = 0;
        int now = 0;
        for (int i = 0; i < len; ++i) {
            if (ipv6[i] == ':') {
                if (i < len-1 && ipv6[i+1] == ':') {
                    // 双冒号
                    addr[now] = num;
                    num = 0, now += 6 - singleColonCnt + 1;
                    ++i;  // 跳过整个双冒号
                } else {
                    // 单冒号
                    addr[now] = num;
                    num = 0, now += 1;
                }
            } else {
                num = (num << 4) | hex2dec(ipv6[i]);
            }
        }
        if (now < 8) addr[now] = num;
    }

    // 获得 ipv6 的 sockaddr
    // ipv6 为 ipv6 格式的字符串，port 为小端序存储的端口号
    sockaddr_in6 getSockaddr6(char *ipv6, uint16_t port) {
        sockaddr_in6 ret;
        ret.sin6_family = AF_INET6;
        v62uint16(ipv6, ret.sin6_addr.__in6_u.__u6_addr16);
        switchEndian(ret.sin6_addr.__in6_u.__u6_addr16);
        ret.sin6_port = switchEndian(port);
        ret.sin6_flowinfo = 0;
        ret.sin6_scope_id = 0;
        return ret;
    }
};