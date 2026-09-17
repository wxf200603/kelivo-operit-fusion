#include <jni.h>
#include <android/log.h>
#include <string>

#define LOG_TAG "FusionNativeBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VAVA_ARGS__)

extern "C" {

/**
 * 原生桥接入口 - 用于 Dart FFI 高性能调用
 *
 * 当前版本保留接口，实际逻辑由 Kotlin 层实现。
 * 未来可将容器执行、终端 I/O 等热点路径下沉到 Native 层。
 */

JNIEXPORT jstring JNICALL
Java_com_kelivo_operit_fusion_bridge_FusionBridge_nativeGetVersion(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF("Kelivo Operit Fusion v1.0.0");
}

JNIEXPORT jstring JNICALL
Java_com_kelivo_operit_fusion_bridge_FusionBridge_nativeExecuteCommand(
        JNIEnv* env,
        jobject /* this */,
        jstring command) {
    const char* cmd = env->GetStringUTFChars(command, nullptr);
    // TODO: 实现原生命令执行（未来下沉热点路径）
    env->ReleaseStringUTFChars(command, cmd);
    return env->NewStringUTF("Not implemented");
}

} // extern "C"
