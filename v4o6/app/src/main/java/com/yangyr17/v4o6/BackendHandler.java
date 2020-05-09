package com.yangyr17.v4o6;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class BackendHandler extends Handler {
    BackendHandler(MainActivity activity, Looper looper) {
        super(looper);
        this.activity = new WeakReference<MainActivity>(activity);
    }

    @Override
    public void handleMessage(Message message) {
        String s = (String)message.obj;
        MainActivity mainActivity = activity.get();
        Toast.makeText(mainActivity, s, Toast.LENGTH_LONG).show();
    }
    private WeakReference<MainActivity> activity;
}
