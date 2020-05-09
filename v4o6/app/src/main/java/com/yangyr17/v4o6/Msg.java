package com.yangyr17.v4o6;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Msg {
    int length;
    byte type;
    String data;

    static public boolean readMsg(File fifo, byte[] buf, Msg ret) {
        try {
            FileInputStream fileInputStream = new FileInputStream(fifo);
            BufferedInputStream in = new BufferedInputStream(fileInputStream);
            // length
            int readLen = in.read(buf, 0, 4);
            if (readLen < 4) {
                Log.e("readMsg", "读入 length 失败");
                return false;
            }
            ret.length = Utils.byteToInt(buf);
            if (ret.length > 4096 || ret.length < 0) {
                Log.e("readMsg", "读取 Msg 失败，length 不在正常范围内：" + ret.length);
                return false;
            }
            // type
            readLen = in.read(buf, 0, 1);
            Log.i("readMsg", "length: " + ret.length);
            if (readLen < 1) {
                Log.e("readMsg", "读入 type 失败");
                return false;
            }
            ret.type = buf[0];
            // data
            int expectLen = ret.length - 5;
            if (expectLen > 0) {
                readLen = in.read(buf, 0, expectLen);
                if (readLen < expectLen) {
                    Log.e("readMsg", "读入 data 失败");
                    return false;
                }
                ret.data = new String(buf, 0, expectLen);
            }
            in.close();
            //Log.i("readMsg", "Suc to read, len: " + ret.length + ", type: "
            //        + ret.type + ", data: " + ret.data);
            return true;
        } catch (FileNotFoundException e) {
            Log.e("readMsg", "FileNotFoundException");
        } catch (IOException e) {
            Log.e("readMsg", "IOException");
        } catch (Exception e) {
            Log.e("readMsg", e.toString());
        }
        return false;
    }

    static public boolean writeMsg(File fifo, Msg msg) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fifo);
            BufferedOutputStream out = new BufferedOutputStream(fileOutputStream);
            byte []tmp = Utils.intToByte(msg.length);
            out.write(tmp, 0, 4);
            tmp = new byte[1];
            tmp[0] = msg.type;
            out.write(tmp, 0, 1);
            out.write(msg.data.getBytes());
            // out.write(arr, 0, arr.length);//arr 是存放数据的 byte 类型数组
            out.close();
            return true;
        } catch (Exception e) {
            Log.e("writeMsg", e.toString());
            return false;
        }
    }
}
