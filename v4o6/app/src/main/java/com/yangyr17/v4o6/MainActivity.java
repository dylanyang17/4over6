package com.yangyr17.v4o6;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

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
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        buttonConnect = (Button) findViewById(R.id.buttonConnect);
        checkPermissions(this);
        ipFifoPath = getFilesDir().getAbsolutePath() + "/ip_fifo";
        tunFifoPath = getFilesDir().getAbsolutePath() + "/tun_fifo";
        statFifoPath = getFilesDir().getAbsolutePath() + "/stat_fifo";
        debugFifoPath = getFilesDir().getAbsolutePath() + "/debug_fifo";
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
            intentVpnService.putExtra("protectedFd", protectedFd);
            intentVpnService.putExtra("tunFifoPath", tunFifoPath);
            startService(intentVpnService);
            bindService(intentVpnService, connection, Context.BIND_AUTO_CREATE);

            buttonConnect.setText("断开");
        }
    }

    // 点击连接/断开
    public void connect(View view) {
        if (buttonConnect.getText().equals("连接")) {
            // 进行连接
            File ipFifoFile = new File(ipFifoPath), tunFifoFile = new File(tunFifoPath),
                    statFifoFile = new File(statFifoPath), debugFifoFile = new File(debugFifoPath);
            if (ipFifoFile.exists() || tunFifoFile.exists() || statFifoFile.exists() || debugFifoFile.exists()) {
                Log.i("connect", "清理管道");
                if ((ipFifoFile.exists() && !ipFifoFile.delete()) ||
                        (tunFifoFile.exists() && !tunFifoFile.delete()) ||
                        (statFifoFile.exists() && !statFifoFile.delete()) ||
                        (debugFifoFile.exists() && !debugFifoFile.delete())) {
                    Log.e("connect", "清理管道失败");
                }
            }

            Log.i("connect", "启动计时器线程");
            textViewTime.setText("0");
            workHandler = new WorkHandler(this, getMainLooper());
            wordThread = new Thread(new WorkRunnable(workHandler, ipFifoPath, tunFifoPath, statFifoPath));
            wordThread.start();

            Log.i("connect", "启动 C++ 后台线程");
            backendHandler = new BackendHandler(this, getMainLooper());
            backendThread = new Thread(new BackendRunnable(backendHandler, editTextIPv6.getText().toString(),
                    Integer.parseInt(editTextPort.getText().toString()), ipFifoPath, tunFifoPath,
                    statFifoPath, debugFifoPath));
            backendThread.start();

            Log.i("connect", "启动 debug 线程");
            debugThread = new Thread(new DebugRunnable(debugFifoPath));
            debugThread.start();

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

    public String ipFifoPath, tunFifoPath, statFifoPath, debugFifoPath;

    public String ipv4, route, dns1, dns2, dns3;  // 通过 101 ip 响应获得
    public int protectedFd;                       // 通过 101 ip 响应获得
    public TextView textViewTime, textViewState;
    public EditText editTextIPv6, editTextPort;
    public Button buttonConnect;
    public Intent intentVpnService;
    public WorkHandler workHandler;
    public BackendHandler backendHandler;
    public Thread wordThread, backendThread, debugThread;
}

