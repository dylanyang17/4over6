package com.yangyr17.v4o6;

public class BackendRunnable implements Runnable {
    public BackendRunnable(String ipv6, int port, String ipFifoPath, String tunFifoPath, String statFifoPath) {
        super();
        this.ipv6 = ipv6;
        this.port = port;
        this.ipFifoPath = ipFifoPath;
        this.tunFifoPath = tunFifoPath;
        this.statFifoPath = statFifoPath;
    }

    @Override
    public void run() {
        JNIUtils.startBackend(ipv6, port, ipFifoPath, tunFifoPath, statFifoPath);
    }

    private String ipv6, ipFifoPath, tunFifoPath, statFifoPath;
    private int port;
}
