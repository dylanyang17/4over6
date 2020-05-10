package com.yangyr17.v4o6;

import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;

// 不断读取 debugFifo 以获取调试信息
public class DebugRunnable implements Runnable{
    public DebugRunnable(String debugFifoPath, DebugHandler handler) {
        super();
        this.debugFifoPath = debugFifoPath;
        this.handler = handler;
    }

    @Override
    public void run() {
        File debugFifoFile = new File(debugFifoPath);
        BufferedInputStream debugFifoIn;
        try {
            while (!debugFifoFile.exists());
            FileInputStream fileInputStream = new FileInputStream(debugFifoFile);
            debugFifoIn = new BufferedInputStream(fileInputStream);
            while (true) {
                if (!debugFifoFile.exists()) {
                    Log.i("DebugRunnable", "debug 管道暂不存在");
                    continue;
                } else {
                    // 读取 debug 管道
                    Msg msg = new Msg();

                    boolean suc = Msg.readMsg(debugFifoIn, buffer, msg);
                    if (!suc) {
                        Log.e("DebugRunnable", "读取 Message 失败");
                        continue;
                    }
                    if (msg.type != Constants.TYPE_PACKAGE_RESPONSE) {
                        Log.i("DebugRunnable", "length: " + msg.length + ", type: " + msg.type + ", data: " + msg.data);
                    } else {
                        Log.i("DebugRunnable", "length: " + msg.length + ", type: " + msg.type);
                    }
                    if (msg.type == Constants.TYPE_CLOSE_DEBUG) {
                        Log.i("DebugRunnable", "关闭 DebugRunnable");
                        break;
                    }
                }
            }
            debugFifoIn.close();
        }  catch (Exception e) {
            Log.e("DebugRunnable", e.toString());
        }
        Message message = Message.obtain();
        message.what = Constants.TYPE_CLOSE_DEBUG;
        handler.handleMessage(message);
    }

    private String debugFifoPath;
    private DebugHandler handler;
    private byte []buffer = new byte[4200];
}
