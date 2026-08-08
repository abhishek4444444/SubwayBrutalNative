#include <jni.h>
#include <android/log.h>
#include <cstring>
#include "memory.h"
#include "hooks.h"
#include "socket_server.h"

#define LOG_TAG "SubwayBrutal_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jint JNICALL
Java_com_abhishek_subwaybrutal_NativeBridge_nativeGetStatus(JNIEnv* env, jobject) {
    uintptr_t base = Memory::GetIL2CppBase();
    if (base != 0 && SocketServer::IsRunning()) return 2;
    if (base != 0) return 1;
    return 0;
}

JNIEXPORT void JNICALL
Java_com_abhishek_subwaybrutal_NativeBridge_nativeSetFlag(JNIEnv* env, jobject, jstring jcmd, jboolean enabled) {
    const char* cmd = env->GetStringUTFChars(jcmd, nullptr);
    if (!cmd) return;
    bool en = (enabled == JNI_TRUE);
    if (strcmp(cmd, "GOD_MODE") == 0) Hacks::g_godMode.store(en);
    else if (strcmp(cmd, "UNLIMITED_COINS") == 0) Hacks::g_unlimitedCoins.store(en);
    else if (strcmp(cmd, "INFINITE_JUMP") == 0) Hacks::g_infiniteJump.store(en);
    else if (strcmp(cmd, "SPEED_HACK") == 0) Hacks::g_speedHack.store(en);
    else if (strcmp(cmd, "SUPER_JUMP") == 0) Hacks::g_superJump.store(en);
    else if (strcmp(cmd, "INFINITE_JETPACK") == 0) Hacks::g_infiniteJetpack.store(en);
    else if (strcmp(cmd, "NO_OBSTACLES") == 0) Hacks::g_noObstacles.store(en);
    else if (strcmp(cmd, "AUTO_SCORE") == 0) Hacks::g_autoScore.store(en);
    else if (strcmp(cmd, "UNLOCK_ALL") == 0) Hacks::g_unlockAll.store(en);
    else if (strcmp(cmd, "ANTI_CRASH") == 0) Hacks::g_antiCrash.store(en);
    LOGI("JNI setFlag: %s = %d", cmd, en ? 1 : 0);
    env->ReleaseStringUTFChars(jcmd, cmd);
}

}
