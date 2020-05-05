#include <cstring>

struct Message {
    int length;       // 整个结构体的字节长度
    char type;        // 类型
    char data[4096];  // 数据段

    Message() {
        length = 0;
        type = 0;
        memset(data, 0, sizeof(data));
    }

    void print() {
        printf("length: %d, type: %d\ndata: %s\n", length, type, data);
    }
};