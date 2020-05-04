#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <cstdio>
#include "utils.cpp"

int main() {
    const char ipv6[] = "2402:f000:4:72:808::9a47";
    const int PORT = 5678;
    int fd = socket(AF_INET6, SOCK_STREAM, 0);
    if (fd < 0) {
        perror("cannot create socket");
        return 0;
    }
    printf("created socket, fd: %d\n", fd);
    
    // sockaddr_in6 addr;
    // addr.sin6_family = AF_INET6;
    // addr.sin6_addr = 


    sockaddr_in6 saddr6 = utils::getSockaddr6(const_cast<char*>(ipv6), PORT);
    int ret = connect(fd, (sockaddr*)(&saddr6), sizeof(saddr6));
    printf("%d\n", ret);
    return 0;
}