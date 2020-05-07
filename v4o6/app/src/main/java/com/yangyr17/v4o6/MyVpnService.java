package com.yangyr17.v4o6;

import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class MyVpnService extends VpnService {
    public ParcelFileDescriptor tun;
    private  MyVpnBinder binder = new MyVpnBinder();

    class MyVpnBinder extends Binder {
        public MyVpnService getService() {
            return MyVpnService.this;
        }
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        Bundle bundle = intent.getExtras();
        String ipv4 = bundle.getString("ipv4");
        String route = bundle.getString("route");
        String dns1 = bundle.getString("dns1");
        String dns2 = bundle.getString("dns2");
        String dns3 = bundle.getString("dns3");
        Builder builder = new Builder();
        builder.setMtu(Constants.mtu)
                .addAddress(ipv4, 24)
                .addRoute(route, 0)
                .addDnsServer(dns1)
                .addDnsServer(dns2)
                .addDnsServer(dns3)
                .setSession(Constants.session);
        tun = builder.establish();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("MyVpnService", "onBind");
        return binder;
    }

    @Override
    public void onDestroy(){
        Log.i("MyVpnService", "onDestroy");
        try {
            tun.close();
        } catch (java.io.IOException e) {
            Log.e("Destroy tun", "java.io.IOException");
        }
        super.onDestroy();
    }

    public void stopVpn() {
        Log.i("MyVpnSerice", "stopVpn");
        try {
            tun.close();
        } catch (java.io.IOException e) {
            Log.e("Destroy tun", "java.io.IOException");
        }
    }
}
