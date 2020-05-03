package com.yangyr17.v4o6;

import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

public class MyVpnService extends VpnService {
    public MyVpnService() {
        super();
        Builder builder = new Builder();
        builder.setMtu(Constants.mtu)
                .addAddress(Constants.address, Constants.addressPreLen)
                .addRoute(Constants.routeIp, Constants.routePreLen)
                .addDnsServer(Constants.dnsServer)
                .setSession(Constants.session);
        ParcelFileDescriptor file = builder.establish();
    }
}

