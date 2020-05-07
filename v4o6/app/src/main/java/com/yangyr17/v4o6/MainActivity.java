package com.yangyr17.v4o6;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MyVpnService.MyVpnBinder myVpnBinder = (MyVpnService.MyVpnBinder) binder;
            isBound = true;
            myVpnService = myVpnBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

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
        editTextIPv6 = (EditText) findViewById(R.id.editTextIPv6);
        buttonConnect = (Button) findViewById(R.id.buttonConnect);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result == RESULT_OK) {
            // 用户同意建立 VPN 连接
            Log.i("intentVpnService", "start service");
            intentVpnService = new Intent(getBaseContext(), MyVpnService.class);
            intentVpnService.putExtra("ipv4", ipv4);
            intentVpnService.putExtra("route", route);
            intentVpnService.putExtra("dns1", dns1);
            intentVpnService.putExtra("dns2", dns2);
            intentVpnService.putExtra("dns3", dns3);
            startService(intentVpnService);
            bindService(intentVpnService, connection, Context.BIND_AUTO_CREATE);
            buttonConnect.setText("断开");
        }
    }

    // 点击连接/断开
    public void connect(View view) {
        if (buttonConnect.getText().equals("连接")) {
            // 进行连接
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
                // 请求建立 VPN 连接，在 result 为 OK 时 setText("断开")
                startVpn();
                // startWorker();
            } else {
                // 失败
                textViewState.setText(info);
                myVpnService.stopVpn();
            }
        } else {
            // 断开连接
            Log.i("lala", "try to stop");
            myVpnService.stopVpn();
            unbindService(connection);
            stopService(intentVpnService);
            buttonConnect.setText("连接");
            textViewState.setText("已断开");
        }
    }

    public boolean isBound = false;
    public MyVpnService myVpnService;

    public String ipv4, route, dns1, dns2, dns3;  // 通过 101 ip 响应获得
    public TextView textViewTime, textViewState;
    public EditText editTextIPv6;
    public Button buttonConnect;
    public Intent intentVpnService;
    public WorkHandler handler;
}

