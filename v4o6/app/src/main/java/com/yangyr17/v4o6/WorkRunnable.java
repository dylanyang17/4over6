package com.yangyr17.v4o6;

import android.os.Looper;
import android.os.Message;

import android.os.Handler;
import android.util.Log;

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
            Message message = Message.obtain();
            handler.sendMessage(message);
        }
    }

    private WorkHandler handler;
}
