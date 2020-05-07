package com.yangyr17.v4o6;

import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class MyVpnService extends VpnService {
    protected ParcelFileDescriptor tun;

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
        return ret;
    }

    @Override
    public void onDestroy(){
        try {
            tun.close();
        } catch (java.io.IOException e) {
            Log.e("Destroy tun", "java.io.IOException");
        }
    }
}
