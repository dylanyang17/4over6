package com.yangyr17.v4o6;

import android.content.Context;
import android.os.Looper;
import android.os.Message;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.logging.LogRecord;

public class WorkRunnable implements Runnable {
    WorkRunnable(WorkHandler handler) {
        super();
        this.handler = handler;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.w("WorkThread", "Interrupted Exception while sleeping.");
            }
            // 读管道
            // TODO: 暂时硬编码
            Log.i("worker", "start");
            String path = "/data/user/0/com.yangyr17.v4o6/files/fifo";
            File file = new File(path);
            Log.i("worker", "lalala0");
            try {
                byte []buf = new byte[100];
                Log.i("worker", "lalala1");
                FileInputStream fileInputStream = new FileInputStream(file);
                Log.i("worker", "lalala2");
                BufferedInputStream in = new BufferedInputStream(fileInputStream);
                Log.i("worker", "lalala3");
                int readLen = in.read(buf);
                in.close();
                Log.i("fifo", "Suc to read, len: " + String.valueOf(readLen) + ". Content: " + new String(buf));
            } catch (FileNotFoundException e) {
                Log.e("fifo", "FileNotFoundException");
            } catch (IOException e) {
                Log.e("fifo", "IOException");
            }
//            Message message = Message.obtain();
//            handler.sendMessage(message);
        }
    }

    private WorkHandler handler;
}
