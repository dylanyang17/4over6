package com.yangyr17.v4o6;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class Utils {
    static public int byteToInt(byte[] buf) {
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bais);
        try {
            return dis.readInt();
        } catch (IOException e) {
            Log.e("byteToInt", e.toString());
            return -1;
        }
    }

    // 大端序
    static public byte[] intToByte(int value) {
        byte[] ret = new byte[4];
        ret[0] =  (byte) (value & 0xFF);
        ret[1] =  (byte) ((value>>8) & 0xFF);
        ret[2] =  (byte) ((value>>16) & 0xFF);
        ret[3] =  (byte) ((value>>24) & 0xFF);
        return ret;
    }
}
