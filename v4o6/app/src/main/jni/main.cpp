//
// Created by yangyr17 on 2020/5/4.
//
#include "com_yangyr17_v4o6_JNIUtils.h"
#include <sys/socket.h>


JNIEXPORT jstring JNICALL Java_com_yangyr17_v4o6_JNIUtils_StringFromJNI
  (JNIEnv *env, jclass thiz)
{
    int sock = socket(AF_INET6, SOCK_STREAM, 0);
    return env->NewStringUTF("Hello from JNI");
}