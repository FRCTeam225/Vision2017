#include <jni.h>

jlong processImage(JNIEnv *env, int texOut, int w, int h, jobject targetInfo);
jbyteArray processJpg(JNIEnv *env, void* ptr);
void releasePtr(long ptr);

JNIEXPORT void JNICALL
Java_org_techfire225_firevision2017_Native_processImage(
        JNIEnv *env,
        jclass cls,
        jint texOut,
        jint w,
        jint h,
        jobject targetInfo) {
    processImage(env, texOut, w, h, targetInfo);
}

JNIEXPORT jbyteArray JNICALL
        Java_org_techfire225_firevision2017_Native_processJpg(JNIEnv *env, jclass cls, jlong ptr) {
    return processJpg(env, ptr);
}

JNIEXPORT void JNICALL
Java_org_techfire225_firevision2017_Native_releaseImage(JNIEnv *env, jclass cls, jlong ptr) {
    releasePtr(ptr);
}