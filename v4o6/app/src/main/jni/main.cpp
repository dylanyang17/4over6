//
// Created by yangyr17 on 2020/5/4.
//
#include "com_yangyr17_v4o6_JNIUtils.h"
#include <sys/socket.h>
#include "backend.cpp"

void jstringToChar(JNIEnv* env, jstring jstr, char* rtn)
{
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray barr= (jbyteArray)env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0)
    {
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
}

JNIEXPORT jstring JNICALL Java_com_yangyr17_v4o6_JNIUtils_connectToServer
  (JNIEnv *env, jclass thiz, jstring jipFifoPath)
{
    // TODO: 之后可以改为从前台传入
    const char ipv6[] = "2402:f000:4:72:808::9a47";
    const int PORT = 5678;
    char ipFifoPath[100];
    jstringToChar(env, jipFifoPath, ipFifoPath);
    return env->NewStringUTF(ipFifoPath);
    //char ret[200];
    //init(const_cast<char*>(ipv6), PORT, ret);
    //return env->NewStringUTF(ret);
}