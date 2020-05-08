package com.yangyr17.v4o6;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

public class WorkHandler extends Handler {
    WorkHandler(MainActivity activity, Looper looper) {
        super(looper);
        this.activity = new WeakReference<MainActivity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        MainActivity mainActivity = activity.get();
        if (msg.what == Constants.TYPE_IP_RESPONSE) {
            // 收到 IP 响应，连接 vpn
        }
        int time = Integer.parseInt(mainActivity.textViewTime.getText().toString()) + 1;
        mainActivity.textViewTime.setText(String.valueOf(time));
        Log.i("timer", String.valueOf(time));
    }
    private WeakReference<MainActivity> activity;
}
