//
// Created by yangyr17 on 2020/5/4.
//
#include <sys/socket.h>
#include "backend.cpp"


int main() {
    // TODO: 之后可以改为从前台传入
    const char ipv6[] = "2402:f000:4:72:808::9a47";
    const int PORT = 5678;
    char ret[200];
    init(const_cast<char*>(ipv6), PORT, ret);
    printf("%s\n", ret);
}