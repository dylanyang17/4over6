package com.yangyr17.v4o6;

public final class Constants {
    static final int mtu = 1500;
    static final String session = "myvpn";

    static boolean isMessageType(int type) {
        // TODO: 若有新类型则将修改
        return type >= TYPE_IP_REQUEST && type <= TYPE_TIMEOUT;
    }

    static final byte WHAT_TIMER = 1;
    static final byte TYPE_IP_REQUEST = 100;
    static final byte TYPE_IP_RESPONSE = 101;
    static final byte TYPE_PACKAGE_REQUEST = 102;
    static final byte TYPE_PACKAGE_RESPONSE = 103;
    static final byte TYPE_BEAT = 104;
    static final byte TYPE_TUN = 105;
    static final byte TYPE_STAT = 106;
    static final byte TYPE_DEBUG = 107;
    static final byte TYPE_TIMEOUT = 108;
    static final byte TYPE_CLOSE_DEBUG = 109;
}
