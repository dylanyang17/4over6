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
    public void handleMessage(Message message) {
        super.handleMessage(message);
        MainActivity mainActivity = activity.get();
        if (message.what == Constants.TYPE_IP_RESPONSE) {
            // 收到 IP 响应，连接 vpn
            Msg msg = (Msg) message.obj;
            String[] tmp = msg.data.split(" ");
            mainActivity.ipv4 = tmp[0];
            mainActivity.route = tmp[1];
            mainActivity.dns1 = tmp[2];
            mainActivity.dns2 = tmp[3];
            mainActivity.dns3 = tmp[4];
            mainActivity.protectedFd = Integer.parseInt(tmp[5]);
            Log.i("ipv4", mainActivity.ipv4);
            Log.i("route", mainActivity.route);
            Log.i("dns1", mainActivity.dns1);
            Log.i("dns2", mainActivity.dns2);
            Log.i("dns3", mainActivity.dns3);
            Log.i("protectedFd", String.valueOf(mainActivity.protectedFd));
            // 请求建立 VPN 连接，在 result 为 OK 时 setText("断开")
            mainActivity.startVpn();
        } else if (message.what == Constants.TYPE_STAT) {
            Msg msg = (Msg) message.obj;
            String[] tmp = msg.data.split(" ");
            int uploadBytes = Integer.parseInt(tmp[0]), uploadPackages = Integer.parseInt(tmp[1]);
            int downloadBytes = Integer.parseInt(tmp[2]), downloadPackages = Integer.parseInt(tmp[3]);
            mainActivity.updateStat(uploadBytes, uploadPackages, downloadBytes, downloadPackages);

        } else if (message.what == 1) {  // 计时器
            int time = Integer.parseInt(mainActivity.textViewTime.getText().toString()) + 1;
            mainActivity.textViewTime.setText(String.valueOf(time));
            Log.i("timer", String.valueOf(time));
        }
    }

    private WeakReference<MainActivity> activity;
}
