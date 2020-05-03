//
// Created by yangyr17 on 2020/5/4.
//
#include "com_yangyr17_v4o6_JNIUtils.h"

JNIEXPORT jstring JNICALL Java_com_yangyr17_v4o6_JNIUtils_StringFromJNI
  (JNIEnv *env, jobject thiz)
{
    return (*env)->NewStringUTF(env, "hello from JNI");
}