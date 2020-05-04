package com.yangyr17.v4o6;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
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
        timeTextView.setText("0");
        handler = new WorkHandler(this, getMainLooper());
        Thread thread = new Thread(new WorkRunnable(handler));
        thread.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        timeTextView = (TextView) findViewById(R.id.textViewTime);
        timeTextView.setText(JNIUtils.StringFromJNI());
        // 请求建立 VPN 连接
        // startVpn();
        // startWorker();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result == RESULT_OK) {
            // 用户同意建立 VPN 连接
            Intent intent = new Intent(MainActivity.this, MyVpnService.class);
            startService(intent);
        }
    }

    public TextView timeTextView;
    public WorkHandler handler;
}

