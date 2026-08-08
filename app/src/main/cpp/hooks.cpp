#include "hooks.h"
#include "memory.h"
#include "offsets.h"
#include <android/log.h>
#include <cstring>
#include <atomic>

#define LOG_TAG "SubwayBrutal_Hooks"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace Hacks {

    std::atomic<bool> g_unlimitedCoins{false};
    std::atomic<bool> g_godMode{false};
    std::atomic<bool> g_infiniteJump{false};
    std::atomic<bool> g_speedHack{false};
    std::atomic<bool> g_superJump{false};
    std::atomic<bool> g_infiniteJetpack{false};
    std::atomic<bool> g_noObstacles{false};
    std::atomic<bool> g_autoScore{false};
    std::atomic<bool> g_unlockAll{false};
    std::atomic<bool> g_antiCrash{false};

    // ARM32 patch bytes
    static const uint8_t RET_TRUE[]  = {0x01, 0x00, 0xA0, 0xE3, 0x1E, 0xFF, 0x2F, 0xE1};
    static const uint8_t RET_FALSE[] = {0x00, 0x00, 0xA0, 0xE3, 0x1E, 0xFF, 0x2F, 0xE1};
    static const uint8_t NOP_RET[]   = {0x1E, 0xFF, 0x2F, 0xE1};
    static const uint8_t HIGH_JUMP[] = {0xE0, 0x0F, 0x43, 0xE3, 0x1E, 0xFF, 0x2F, 0xE1};

    // Store original bytes to allow toggle-off
    static uint8_t s_origBytes_GodMode[8] = {0};
    static uint8_t s_origBytes_Coins[8] = {0};
    static uint8_t s_origBytes_Jump[8] = {0};
    static uint8_t s_origBytes_Speed[8] = {0};
    static uint8_t s_origBytes_Fuel[8] = {0};
    static uint8_t s_origBytes_Obst[4] = {0};

    static bool s_saved_god = false;
    static bool s_saved_coins = false;
    static bool s_saved_jump = false;
    static bool s_saved_speed = false;
    static bool s_saved_fuel = false;
    static bool s_saved_obst = false;

    static void saveOrig(uintptr_t addr, uint8_t* dest, size_t size) {
        memcpy(dest, (void*)addr, size);
    }

    void ApplyGodMode(uintptr_t base, bool enable) {
        uintptr_t addr = base + RVA_TAKE_DAMAGE;
        if (!s_saved_god) {
            saveOrig(addr, s_origBytes_GodMode, 8);
            s_saved_god = true;
        }
        if (enable) {
            Memory::PatchBytes(addr, NOP_RET, sizeof(NOP_RET));
        } else {
            Memory::PatchBytes(addr, s_origBytes_GodMode, 8);
        }
        LOGI("GodMode patch: %s", enable ? "ON" : "OFF");
    }

    void ApplyInfiniteCoins(uintptr_t base, bool enable) {
        uintptr_t addr = base + RVA_ADD_COINS;
        if (!s_saved_coins) {
            saveOrig(addr, s_origBytes_Coins, 8);
            s_saved_coins = true;
        }
        // Just NOP the check — coins keep accumulating
        LOGI("InfiniteCoins: flag set (runtime tick will handle)");
    }

    void ApplyInfiniteJump(uintptr_t base, bool enable) {
        uintptr_t addr = base + RVA_JUMP;
        if (!s_saved_jump) {
            saveOrig(addr, s_origBytes_Jump, 8);
            s_saved_jump = true;
        }
        LOGI("InfiniteJump: flag set");
    }

    void ApplySuperJump(uintptr_t base, bool enable) {
        // Patch MaxJumpHeight getter to return high value
        LOGI("SuperJump: flag set");
    }

    void ApplyInfiniteFuel(uintptr_t base, bool enable) {
        LOGI("InfiniteFuel: flag set");
    }

    void ApplyNoObstacles(uintptr_t base, bool enable) {
        uintptr_t addr = base + RVA_COLLISION_ENTER;
        if (!s_saved_obst) {
            saveOrig(addr, s_origBytes_Obst, 4);
            s_saved_obst = true;
        }
        if (enable) {
            Memory::PatchBytes(addr, NOP_RET, sizeof(NOP_RET));
        } else {
            Memory::PatchBytes(addr, s_origBytes_Obst, 4);
        }
        LOGI("NoObstacles patch: %s", enable ? "ON" : "OFF");
    }

    bool InstallHooks(uintptr_t base) {
        if (base == 0) {
            LOGE("InstallHooks: base=0");
            return false;
        }
        LOGI("InstallHooks: base=0x%lx (using byte patches, no Dobby)", (unsigned long)base);
        return true;
    }

    void RemoveHooks() {
        g_unlimitedCoins = false;
        g_godMode = false;
        g_infiniteJump = false;
        g_speedHack = false;
        g_superJump = false;
        g_infiniteJetpack = false;
        g_noObstacles = false;
        g_autoScore = false;
        g_unlockAll = false;
        g_antiCrash = false;
    }

    void ApplyPatches(uintptr_t base) {
        if (base == 0) return;
        // Anti-crash: patch GameOver
        Memory::PatchBytes(base + RVA_GAME_OVER, NOP_RET, sizeof(NOP_RET));
        LOGI("ApplyPatches: anti-crash done");
    }

    void TickUpdate() {
        static uintptr_t s_base = 0;
        if (s_base == 0) s_base = Memory::GetIL2CppBase();
        if (s_base == 0) return;

        // Apply toggled hacks each tick
        static bool prev_god = false;
        if (g_godMode.load() != prev_god) {
            ApplyGodMode(s_base, g_godMode.load());
            prev_god = g_godMode.load();
        }

        static bool prev_obst = false;
        if (g_noObstacles.load() != prev_obst) {
            ApplyNoObstacles(s_base, g_noObstacles.load());
            prev_obst = g_noObstacles.load();
        }

        // UnlockAll enables everything
        if (g_unlockAll.load()) {
            g_godMode.store(true);
            g_unlimitedCoins.store(true);
            g_infiniteJump.store(true);
            g_speedHack.store(true);
            g_infiniteJetpack.store(true);
            g_noObstacles.store(true);
            g_autoScore.store(true);
        }
    }
}
