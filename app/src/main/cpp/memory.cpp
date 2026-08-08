#include "memory.h"
#include <android/log.h>
#include <cstdio>
#include <cstring>
#include <sys/mman.h>
#include <unistd.h>

#define LOG_TAG "SubwayBrutal_Mem"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace Memory {

    static uintptr_t s_il2cppBase = 0;

    void FlushCache(uintptr_t address, size_t size) {
        __builtin___clear_cache(
            reinterpret_cast<char*>(address),
            reinterpret_cast<char*>(address + size)
        );
    }

    bool PatchBytes(uintptr_t address, const uint8_t* bytes, size_t size) {
        if (address == 0 || bytes == nullptr || size == 0) {
            LOGE("PatchBytes: invalid args");
            return false;
        }
        uintptr_t page_start = address & ~(uintptr_t)(getpagesize() - 1);
        size_t page_size = ((address + size) - page_start) + getpagesize();

        if (mprotect(reinterpret_cast<void*>(page_start), page_size,
                     PROT_READ | PROT_WRITE | PROT_EXEC) != 0) {
            LOGE("PatchBytes: mprotect failed at 0x%lx", (unsigned long)address);
            return false;
        }
        memcpy(reinterpret_cast<void*>(address), bytes, size);
        FlushCache(address, size);
        mprotect(reinterpret_cast<void*>(page_start), page_size, PROT_READ | PROT_EXEC);
        LOGI("PatchBytes: OK at 0x%lx (%zu bytes)", (unsigned long)address, size);
        return true;
    }

    float ReadFloat(uintptr_t address) {
        if (address == 0) return 0.0f;
        float val = 0.0f;
        memcpy(&val, reinterpret_cast<void*>(address), sizeof(float));
        return val;
    }

    bool WriteFloat(uintptr_t address, float value) {
        if (address == 0) return false;
        uintptr_t page = address & ~(uintptr_t)(getpagesize() - 1);
        mprotect(reinterpret_cast<void*>(page), getpagesize() * 2, PROT_READ | PROT_WRITE);
        memcpy(reinterpret_cast<void*>(address), &value, sizeof(float));
        return true;
    }

    int ReadInt(uintptr_t address) {
        if (address == 0) return 0;
        int val = 0;
        memcpy(&val, reinterpret_cast<void*>(address), sizeof(int));
        return val;
    }

    bool WriteInt(uintptr_t address, int value) {
        if (address == 0) return false;
        uintptr_t page = address & ~(uintptr_t)(getpagesize() - 1);
        mprotect(reinterpret_cast<void*>(page), getpagesize() * 2, PROT_READ | PROT_WRITE);
        memcpy(reinterpret_cast<void*>(address), &value, sizeof(int));
        return true;
    }

    bool ReadBool(uintptr_t address) {
        if (address == 0) return false;
        uint8_t val = 0;
        memcpy(&val, reinterpret_cast<void*>(address), 1);
        return val != 0;
    }

    bool WriteBool(uintptr_t address, bool value) {
        if (address == 0) return false;
        uintptr_t page = address & ~(uintptr_t)(getpagesize() - 1);
        mprotect(reinterpret_cast<void*>(page), getpagesize() * 2, PROT_READ | PROT_WRITE);
        uint8_t v = value ? 1 : 0;
        memcpy(reinterpret_cast<void*>(address), &v, 1);
        return true;
    }

    uint32_t ReadPointer(uintptr_t address) {
        if (address == 0) return 0;
        uint32_t val = 0;
        memcpy(&val, reinterpret_cast<void*>(address), sizeof(uint32_t));
        return val;
    }

    uintptr_t GetLibBase(const char* libName) {
        char line[512];
        FILE* fp = fopen("/proc/self/maps", "r");
        if (!fp) return 0;
        uintptr_t base = 0;
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, libName) != nullptr) {
                unsigned long start = 0;
                if (sscanf(line, "%lx-", &start) == 1) {
                    base = (uintptr_t)start;
                    break;
                }
            }
        }
        fclose(fp);
        return base;
    }

    uintptr_t GetIL2CppBase() {
        if (s_il2cppBase != 0) return s_il2cppBase;
        s_il2cppBase = GetLibBase("libil2cpp.so");
        LOGI("libil2cpp.so base: 0x%lx", (unsigned long)s_il2cppBase);
        return s_il2cppBase;
    }
}
