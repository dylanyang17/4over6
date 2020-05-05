package com.yangyr17.v4o6;

public class JNIUtils {
    static public native String connectToServer();
    static {
        System.loadLibrary("hellojni");
    }
}