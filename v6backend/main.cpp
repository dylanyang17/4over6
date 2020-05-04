#include <sys/socket.h>
#include <cstdio>

int main() {
    int fd = socket(AF_INET6, SOCK_STREAM, 0);
    if (fd < 0) {
        perror("cannot create socket");
        return 0;
    }
    printf("created socket, fd: %d\n", fd);
    
    sockaddr addr;
    addr.sa_family = AF_INET6;
    addr.sa_data
    bind(fd, );
    return 0;
}