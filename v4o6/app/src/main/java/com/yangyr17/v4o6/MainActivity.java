package com.yangyr17.v4o6;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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

    protected void checkPermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewTime = (TextView) findViewById(R.id.textViewTime);
        textViewState = (TextView) findViewById(R.id.textViewState);
        editTextIPv6 = (EditText) findViewById(R.id.editTextIPv6);
        buttonConnect = (Button) findViewById(R.id.buttonConnect);
        checkPermissions(this);
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
//            写文件
//            File extDir = Environment.getExternalStorageDirectory();
//            File file = new File(extDir, "cmd_pipe");
//            try {
//                if (!file.exists()) {
//                    boolean suc = file.createNewFile();
//                    Log.i("createNewFile", String.valueOf(suc));
//                }
//                String s = "TESTTEST";
//                byte []buf = s.getBytes();
//                FileOutputStream fileOutputStream = new FileOutputStream(file);
//                BufferedOutputStream out = new BufferedOutputStream(fileOutputStream);
//                out.write(buf, 0, buf.length);
//                out.flush();
//                out.close();
//                Log.i("fifo", "suc");
//            } catch (FileNotFoundException e) {
//                Log.e("fifo", "FileNotFoundException");
//            } catch (IOException e) {
//                Log.e("fifo", "IOException");
//            }
            // 进行连接
            startWorker();  // NOTE: delete
            String info = JNIUtils.connectToServer();

            if (info.charAt(0) >= '0' && info.charAt(0) <= '9') {
                // 成功
//                textViewState.setText("成功连接服务器");
//                String []tmp = info.split(" ");
//                ipv4 = tmp[0];
//                route = tmp[1];
//                dns1 = tmp[2];
//                dns2 = tmp[3];
//                dns3 = tmp[4];
//                Log.i("ipv4", ipv4);
//                Log.i("route", route);
//                Log.i("dns1", dns1);
//                Log.i("dns2", dns2);
//                Log.i("dns3", dns3);
//                // 请求建立 VPN 连接，在 result 为 OK 时 setText("断开")
//                startVpn();
                // startWorker();
                textViewState.setText(info);
            } else {
                // 失败
                textViewState.setText(info);
                // textViewState.setText(getFilesDir().getAbsolutePath());
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

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    public boolean isBound = false;
    public MyVpnService myVpnService;

    public String ipv4, route, dns1, dns2, dns3;  // 通过 101 ip 响应获得
    public TextView textViewTime, textViewState;
    public EditText editTextIPv6;
    public Button buttonConnect;
    public Intent intentVpnService;
    public WorkHandler handler;
}

