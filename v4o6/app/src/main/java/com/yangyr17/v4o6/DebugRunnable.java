package com.yangyr17.v4o6;

import android.os.Message;
import android.util.Log;

import java.io.File;

// 不断读取 debugFifo 以获取调试信息
public class DebugRunnable implements Runnable{
    public DebugRunnable(String debugFifoPath) {
        super();
        this.debugFifoPath = debugFifoPath;
    }

    @Override
    public void run() {
        while (true) {
            File debugFifoFile = new File(debugFifoPath);
            if (!debugFifoFile.exists()) {
                // Log.i("DebugRunnable", "debug 管道暂不存在");
                continue;
            } else {
                // 读取 debug 管道
                Msg msg = new Msg();
                boolean suc = Msg.readMsg(debugFifoFile, buffer, msg);
                if (!suc) {
                    Log.e("DebugRunnable", "读取 Message 失败");
                    continue;
                }
                Log.i("DebugRunnable", "length: " + msg.length + ", type: " + msg.type + ", data: " + msg.data);
            }
        }
    }

    private String debugFifoPath;
    private byte []buffer = new byte[4200];
}
