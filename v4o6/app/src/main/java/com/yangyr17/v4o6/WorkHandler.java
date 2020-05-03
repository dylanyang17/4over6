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
        int time = Integer.parseInt(mainActivity.timeTextView.getText().toString()) + 1;
        mainActivity.timeTextView.setText(String.valueOf(time));
        Log.i("timer", String.valueOf(time));
    }
    private WeakReference<MainActivity> activity;
}
