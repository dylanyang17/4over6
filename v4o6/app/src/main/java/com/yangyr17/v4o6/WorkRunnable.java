package com.yangyr17.v4o6;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.logging.LogRecord;

public class WorkRunnable implements Runnable {

    WorkRunnable(WorkHandler handler, String ipFifoPath, String tunFifoPath, String statFifoPath) {
        super();
        this.handler = handler;
        this.ipFifoPath = ipFifoPath;
        this.tunFifoPath = tunFifoPath;
        this.statFifoPath = statFifoPath;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while (true) {
            // 暂停 1s
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.w("WorkRunnable", "Interrupted Exception while sleeping.");
            }
            Log.d("WorkRunnable", "timer");
            if (!handler.activity.get().isRunning) {
                // 停止执行时，关闭子线程
                break;
            }
            // 读 ip 管道
            if (!hasIP) {
                File ipFifoFile = new File(ipFifoPath);
                if (!ipFifoFile.exists()) {
                    Log.i("WorkRunnable", "ip 管道暂不存在");
                    continue;
                } else {
                    // 读取 ip 管道
                    Msg msg = new Msg();
                    boolean suc = Msg.readMsg(ipFifoFile, buffer, msg);
                    if (!suc) {
                        Log.e("WorkRunnable", "读取 ip message 失败");
                        continue;
                    }
                    if (msg.type == Constants.TYPE_IP_RESPONSE) {
                        hasIP = true;
                        Message message = Message.obtain();
                        message.what = msg.type;
                        message.obj = msg;
                        handler.sendMessage(message);
                    }
                }
            } else {
                // 已经开启了 vpn
                Message message = Message.obtain();
                message.what = 1;
                handler.sendMessage(message);

                // 读取统计信息
                File statFifoFile = new File(statFifoPath);
                Msg msg = new Msg();
                boolean suc = Msg.readMsg(statFifoFile, buffer, msg);
                if (!suc) {
                    Log.e("WorkRunnable", "读取 stat message 失败");
                    continue;
                }
                if (msg.type == Constants.TYPE_STAT) {
                    message = Message.obtain();
                    message.what = msg.type;
                    message.obj = msg;
                    handler.sendMessage(message);
                }
            }
        }
    }

    private WorkHandler handler;
    private String ipFifoPath, tunFifoPath, statFifoPath;
    private byte []buffer = new byte[4200];
    private boolean hasIP = false;
}
