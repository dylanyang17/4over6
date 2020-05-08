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

JNIEXPORT jstring JNICALL Java_com_yangyr17_v4o6_JNIUtils_startBackend
  (JNIEnv *env, jclass thiz, jstring jipv6, jint jport, jstring jipFifoPath, jstring jtunFifoPath, jstring jstatFifoPath)
{
    char ipv6[100], ipFifoPath[100], tunFifoPath[100], statFifoPath[100];
    int port = (int)jport;
    jstringToChar(env, jipv6, ipv6);
    jstringToChar(env, jipFifoPath, ipFifoPath);
    jstringToChar(env, jtunFifoPath, tunFifoPath);
    jstringToChar(env, jstatFifoPath, statFifoPath);
    char ret[200];
    init(ipv6, port, ipFifoPath, tunFifoPath, statFifoPath, ret);
    return (env)->NewStringUTF(ret);
}