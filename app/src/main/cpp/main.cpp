#include "memory.h"
#include "hooks.h"
#include "socket_server.h"
#include <android/log.h>
#include <unistd.h>
#include <pthread.h>

#define LOG_TAG "SubwayBrutal_Main"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static uintptr_t waitForIL2Cpp(int maxRetriesSec) {
    for (int i = 0; i < maxRetriesSec; i++) {
        uintptr_t base = Memory::GetIL2CppBase();
        if (base != 0) {
            LOGI("libil2cpp loaded at 0x%lx after %d sec", (unsigned long)base, i);
            return base;
        }
        sleep(1);
    }
    LOGE("libil2cpp NOT found after %d sec", maxRetriesSec);
    return 0;
}

static void* tickThread(void*) {
    while (true) {
        Hacks::TickUpdate();
        usleep(100000);
    }
    return nullptr;
}

static void* modInitThread(void*) {
    LOGI("Mod init starting...");
    uintptr_t base = waitForIL2Cpp(60);
    if (base == 0) {
        LOGE("Abort: no il2cpp");
        return nullptr;
    }
    LOGI("Applying patches...");
    Hacks::ApplyPatches(base);
    LOGI("Installing hooks...");
    Hacks::InstallHooks(base);
    LOGI("Starting socket server...");
    SocketServer::Start();

    pthread_t tickTid;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_create(&tickTid, &attr, tickThread, nullptr);
    pthread_attr_destroy(&attr);

    LOGI("Subway Brutal fully loaded!");
    return nullptr;
}

__attribute__((constructor))
static void onLibLoad() {
    LOGI("libsubwaybrutal.so loaded");
    pthread_t tid;
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
    pthread_create(&tid, &attr, modInitThread, nullptr);
    pthread_attr_destroy(&attr);
}

__attribute__((destructor))
static void onLibUnload() {
    LOGI("Unloading");
    SocketServer::Stop();
    Hacks::RemoveHooks();
}
