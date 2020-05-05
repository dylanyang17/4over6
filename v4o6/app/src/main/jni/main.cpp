//
// Created by yangyr17 on 2020/5/4.
//
#include "com_yangyr17_v4o6_JNIUtils.h"
#include <sys/socket.h>
#include "backend.cpp"


JNIEXPORT jstring JNICALL Java_com_yangyr17_v4o6_JNIUtils_connectToServer
  (JNIEnv *env, jclass thiz)
{
    // TODO: 之后可以改为从前台传入
    const char ipv6[] = "2402:f000:4:72:808::9a47";
    const int PORT = 5678;
    char ret[200];
    init(const_cast<char*>(ipv6), PORT, ret);
    return env->NewStringUTF(ret);
}