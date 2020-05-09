package com.yangyr17.v4o6;

import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class BackendRunnable implements Runnable {
    public BackendRunnable(BackendHandler handler, String ipv6, int port, String ipFifoPath, String tunFifoPath, String statFifoPath, String debugFifoPath, String FBFifoPath) {
        super();
        this.handler = handler;
        this.ipv6 = ipv6;
        this.port = port;
        this.ipFifoPath = ipFifoPath;
        this.tunFifoPath = tunFifoPath;
        this.statFifoPath = statFifoPath;
        this.debugFifoPath = debugFifoPath;
        this.FBFifoPath = FBFifoPath;
    }

    @Override
    public void run() {
        String ret = JNIUtils.startBackend(ipv6, port, ipFifoPath, tunFifoPath, statFifoPath, debugFifoPath, FBFifoPath);
        Log.i("BackendRunnable", "Finish: " + ret);
        Message message = Message.obtain();
        message.obj = ret;
        handler.sendMessage(message);
    }

    private String ipv6, ipFifoPath, tunFifoPath, statFifoPath, debugFifoPath, FBFifoPath;
    private int port;
    private BackendHandler handler;
}
