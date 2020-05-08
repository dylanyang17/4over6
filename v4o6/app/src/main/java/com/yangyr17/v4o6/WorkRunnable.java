package com.yangyr17.v4o6;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.logging.LogRecord;

public class WorkRunnable implements Runnable {
    public class Msg {
        int length;
        byte type;
        String data;
    };

    WorkRunnable(WorkHandler handler, String ipFifoPath) {
        super();
        this.handler = handler;
        this.ipFifoPath = ipFifoPath;
    }

    int byteToInt(byte[] buf) {
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bais);
        try {
            return dis.readInt();
        } catch (IOException e) {
            Log.e("byteToInt", e.toString());
            return -1;
        }
    }

    boolean readMsg(File fifo, byte[] buf, Msg ret) {
        try {
            FileInputStream fileInputStream = new FileInputStream(fifo);
            BufferedInputStream in = new BufferedInputStream(fileInputStream);
            // length
            int readLen = in.read(buf, 0, 4);
            if (readLen < 4) {
                Log.e("readMsg", "读入 length 失败");
                return false;
            }
            ret.length = byteToInt(buf);
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
            Log.i("readMsg", "Suc to read, len: " + ret.length + ", type: "
                    + ret.type + ", data: " + ret.data);
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

    boolean writeMsg(File fifo, Msg msg) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fifo);
            BufferedOutputStream out = new BufferedOutputStream(fileOutputStream);
            out.write(msg.length);
            byte []tmp = new byte[1];
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

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while (true) {
            // 暂停 1s
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.w("WorkRunnable", "Interrupted Exception while sleeping.");
            }
            Log.d("WorkRunnable", "timer");
            // 读 ip 管道
            if (!hasIP) {
                File ipFifoFile = new File(ipFifoPath);
                if (!ipFifoFile.exists()) {
                    Log.i("WorkRunnable", "ip 管道暂不存在");
                    continue;
                } else {
                    // 读取 ip 管道
                    Msg msg = new Msg();
                    boolean suc = readMsg(ipFifoFile, buffer, msg);
                    if (!suc) {
                        Log.e("WorkRunnable", "读取 Message 失败");
                        continue;
                    }
                    if (msg.type == Constants.TYPE_IP_RESPONSE) {
                        hasIP = true;
                        Message message = Message.obtain();
                        message.what = msg.type;
                        message.obj = msg;
                        handler.sendMessage(message);
                    }
                    msg.length = 5;
                    msg.type = Constants.TYPE_TUN;
                    msg.data = "123";
                    writeMsg(ipFifoFile, msg);
                }

            } else {
                // 已经开启了 vpn
                Message message = Message.obtain();
                handler.sendMessage(message);
            }
        }
    }

    private WorkHandler handler;
    private String ipFifoPath;
    private byte []buffer = new byte[4200];
    private boolean hasIP = false;
}
