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
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;

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
        editTextIPv6 = (EditText) findViewById(R.id.editTextIPv6);
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        buttonConnect = (Button) findViewById(R.id.buttonConnect);
        textViewUploadSpeed = (TextView)  findViewById(R.id.textViewUploadSpeed);
        textViewUploadBytes = (TextView)  findViewById(R.id.textViewUploadBytes);
        textViewUploadPackages = (TextView)  findViewById(R.id.textViewUploadPackages);
        textViewDownloadSpeed = (TextView)  findViewById(R.id.textViewDownloadSpeed);
        textViewDownloadBytes = (TextView)  findViewById(R.id.textViewDownloadBytes);
        textViewDownloadPackages = (TextView)  findViewById(R.id.textViewDownloadPackages);
        textViewIpv4 = (TextView) findViewById(R.id.textViewIpv4);
        checkPermissions(this);
        ipFifoPath = getFilesDir().getAbsolutePath() + "/ip_fifo";
        tunFifoPath = getFilesDir().getAbsolutePath() + "/tun_fifo";
        statFifoPath = getFilesDir().getAbsolutePath() + "/stat_fifo";
        debugFifoPath = getFilesDir().getAbsolutePath() + "/debug_fifo";
        FBFifoPath = getFilesDir().getAbsolutePath() + "/FB_fifo";
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
            textViewIpv4.setText(ipv4);
            buttonConnect.setText("断开");
            buttonConnect.setEnabled(true);
        }
    }

    // 点击连接/断开
    public void connect(View view) {
        if (buttonConnect.getText().equals("连接")) {
            // 进行连接
            buttonConnect.setText("连接中...");
            buttonConnect.setEnabled(false);
            isRunning = true;

            uploadBytes = uploadPackages = downloadBytes = downloadPackages = 0;
            updateStat(0, 0, 0, 0);
            File ipFifoFile = new File(ipFifoPath), tunFifoFile = new File(tunFifoPath),
                    statFifoFile = new File(statFifoPath), debugFifoFile = new File(debugFifoPath),
                    FBFifoFile = new File(FBFifoPath);
            if (ipFifoFile.exists() || tunFifoFile.exists() || statFifoFile.exists() || debugFifoFile.exists() || FBFifoFile.exists()) {
                Log.i("connect", "清理管道");
                if ((ipFifoFile.exists() && !ipFifoFile.delete()) ||
                        (tunFifoFile.exists() && !tunFifoFile.delete()) ||
                        (statFifoFile.exists() && !statFifoFile.delete()) ||
                        (debugFifoFile.exists() && !debugFifoFile.delete()) ||
                        (FBFifoFile.exists() && !FBFifoFile.delete())) {
                    Log.e("connect", "清理管道失败");
                }
            }

            Log.i("connect", "启动计时器线程");
            textViewTime.setText("0");
            workHandler = new WorkHandler(this, getMainLooper());
            workThread = new Thread(new WorkRunnable(workHandler, ipFifoPath, tunFifoPath, statFifoPath, FBFifoPath));
            workThread.start();

            Log.i("connect", "启动 C++ 后台线程");
            backendHandler = new BackendHandler(this, getMainLooper());
            backendThread = new Thread(new BackendRunnable(backendHandler, editTextIPv6.getText().toString(),
                    Integer.parseInt(editTextPort.getText().toString()), ipFifoPath, tunFifoPath,
                    statFifoPath, debugFifoPath, FBFifoPath));
            backendThread.start();

            Log.i("connect", "启动 debug 线程");
            debugThread = new Thread(new DebugRunnable(debugFifoPath));
            debugThread.start();

        } else {
            // 断开连接
            Log.i("lala", "try to stop");
            buttonConnect.setEnabled(false);
            buttonConnect.setText("断开中...");
            isRunning = false;
            myVpnService.stopVpn();
            unbindService(connection);
            stopService(intentVpnService);
        }
    }

    // 返回合适单位下的值，例如 1.5 MB
    // isSpeed 为 true 时，还应加上 /s
    public String getProperScale(int bytes, boolean isSpeed) {
        String []unit = new String[4];
        double size = bytes;
        unit[0] = "B";
        unit[1] = "KB";
        unit[2] = "MB";
        unit[3] = "GB";
        int nowUnit = 0;
        while (size >= 1000 && nowUnit < 3) {
            size /= 1000;
            nowUnit ++;
        }
        NumberFormat formatter = new DecimalFormat("0.00");
        String formattedSize = formatter.format(size);
        return formattedSize + unit[nowUnit] + (isSpeed ? "/s" : "");
    }

    // 更新统计信息
    public void updateStat(int uploadBytesSec, int uploadPackagesSec, int downloadBytesSec, int downloadPackagesSec) {
        uploadBytes += uploadBytesSec;
        downloadBytes += downloadBytesSec;
        uploadPackages += uploadPackagesSec;
        downloadPackages += downloadPackagesSec;
        textViewUploadSpeed.setText(getProperScale(uploadBytesSec, true));
        textViewUploadBytes.setText(getProperScale(uploadBytes, false));
        textViewUploadPackages.setText(String.valueOf(uploadPackages));
        textViewDownloadSpeed.setText(getProperScale(downloadBytesSec, true));
        textViewDownloadBytes.setText(getProperScale(downloadBytes, false));
        textViewDownloadPackages.setText(String.valueOf(downloadPackages));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    public boolean isBound = false;
    public MyVpnService myVpnService;

    public String ipFifoPath, tunFifoPath, statFifoPath, debugFifoPath, FBFifoPath;

    public String ipv4, route, dns1, dns2, dns3;  // 通过 101 ip 响应获得
    public int protectedFd;                       // 通过 101 ip 响应获得
    public TextView textViewTime, textViewIpv4;
    public TextView textViewUploadSpeed, textViewUploadBytes, textViewUploadPackages;
    public TextView textViewDownloadSpeed, textViewDownloadBytes, textViewDownloadPackages;
    public EditText editTextIPv6, editTextPort;
    public Button buttonConnect;
    public Intent intentVpnService;
    public WorkHandler workHandler;
    public BackendHandler backendHandler;
    public Thread workThread, backendThread, debugThread;

    public boolean isRunning = false;

    public int uploadBytes, uploadPackages, downloadBytes, downloadPackages;
}

