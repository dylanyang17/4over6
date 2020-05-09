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

    WorkRunnable(WorkHandler handler, String ipFifoPath, String tunFifoPath, String statFifoPath, String FBFifoPath) {
        super();
        this.handler = handler;
        this.ipFifoPath = ipFifoPath;
        this.tunFifoPath = tunFifoPath;
        this.statFifoPath = statFifoPath;
        this.FBFifoPath = FBFifoPath;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        try {
            File ipFifoFile = new File(ipFifoPath);
            File statFifoFile = new File(statFifoPath);
            while(!ipFifoFile.exists()) ;
            while(!statFifoFile.exists());
            FileInputStream fileInputStream = new FileInputStream(new File(ipFifoPath));
            BufferedInputStream ipFifoIn = new BufferedInputStream(fileInputStream);
            fileInputStream = new FileInputStream(new File(statFifoPath));
            BufferedInputStream statFifoIn = new BufferedInputStream(fileInputStream);
            while (true) {
                // 暂停 1s
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.w("WorkRunnable", "Interrupted Exception while sleeping.");
                }
                Log.d("WorkRunnable", "timer");
                if (!handler.activity.get().isRunning) {
                    // 停止执行时，清理各个子线程
                    // 关闭后台线程 (Backend 对应线程)
                    File FBFifoFile = new File(FBFifoPath);
                    Msg msg = new Msg();
                    msg.length = 5;
                    msg.type = Constants.TYPE_TIMEOUT;
                    if (!Msg.writeMsg(FBFifoFile, msg)) {
                        Log.i("WorkRunnable", "关闭线程时发送消息失败");
                    }
                    Log.i("WorkRunnable", "关闭各个线程");
                    // 关闭当前线程
                    break;
                }
                // 读 ip 管道
                if (!hasIP) {
                    if (!ipFifoFile.exists()) {
                        Log.i("WorkRunnable", "ip 管道暂不存在");
                        continue;
                    } else {
                        // 读取 ip 管道
                        Msg msg = new Msg();
                        boolean suc = Msg.readMsg(ipFifoIn, buffer, msg);
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
                    Msg msg = new Msg();
                    boolean suc = Msg.readMsg(statFifoIn, buffer, msg);
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
            ipFifoIn.close();
            statFifoIn.close();
        } catch (Exception e) {
            Log.e("WorkRunnable", e.toString());
        }
    }

    private WorkHandler handler;
    private String ipFifoPath, tunFifoPath, statFifoPath, FBFifoPath;
    private byte []buffer = new byte[4200];
    private boolean hasIP = false;
}
