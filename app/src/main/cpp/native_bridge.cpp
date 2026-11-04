#include <jni.h>
#include <android/log.h>

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_openmininggateway_miner_MainActivity_nativeEngineAvailable(
        JNIEnv*, jobject) {
    __android_log_print(
        ANDROID_LOG_INFO,
        "OMG-MINER",
        "OMG native ARM64 engine bridge loaded"
    );
    return JNI_TRUE;
}
