package com.yangyr17.v4o6;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class DebugHandler extends Handler {
    DebugHandler(MainActivity activity, Looper looper) {
        super(looper);
        this.activity = new WeakReference<MainActivity>(activity);
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == Constants.TYPE_CLOSE_DEBUG) {
            activity.get().isRunning = false;
            activity.get().isBackendRunning = false;  // 既然是后台传上来的，那么后台一定被关闭了
            activity.get().stopVpn();
        }
    }
    private WeakReference<MainActivity> activity;
}
