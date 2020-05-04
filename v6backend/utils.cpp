#include <cassert>
#include <inttypes.h>
#include <cstring>
#include <cstdio>

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
    
    // 将 ipv6 的字符串转为 uint16_t 数组
    void v62uint16(const char *ipv6, uint16_t *addr) {
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
};