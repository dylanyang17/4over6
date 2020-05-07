package com.yangyr17.v4o6;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    protected void startVpn() {
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    // 开启每秒计时并进行相应处理的工作
    protected void startWorker() {
        textViewTime.setText("0");
        handler = new WorkHandler(this, getMainLooper());
        Thread thread = new Thread(new WorkRunnable(handler));
        thread.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewTime = (TextView) findViewById(R.id.textViewTime);
        textViewState = (TextView) findViewById(R.id.textViewState);
        String info = JNIUtils.connectToServer();
        if (info.charAt(0) >= '0' && info.charAt(0) <= '9') {
            // 成功
            textViewState.setText("成功连接服务器");
            String []tmp = info.split(" ");
            ipv4 = tmp[0];
            route = tmp[1];
            dns1 = tmp[2];
            dns2 = tmp[3];
            dns3 = tmp[4];
            Log.i("ipv4", ipv4);
            Log.i("route", route);
            Log.i("dns1", dns1);
            Log.i("dns2", dns2);
            Log.i("dns3", dns3);
        } else {
            // 失败
            textViewState.setText(info);
        }
        // 请求建立 VPN 连接
        startVpn();
        // startWorker();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result == RESULT_OK) {
            // 用户同意建立 VPN 连接
            Intent intent = new Intent(MainActivity.this, MyVpnService.class);
            intent.putExtra("ipv4", ipv4);
            intent.putExtra("route", route);
            intent.putExtra("dns1", dns1);
            intent.putExtra("dns2", dns2);
            intent.putExtra("dns3", dns3);
            startService(intent);
        }
    }
    public String ipv4, route, dns1, dns2, dns3;  // 通过 101 ip 响应获得
    public TextView textViewTime, textViewState;
    public WorkHandler handler;
}

