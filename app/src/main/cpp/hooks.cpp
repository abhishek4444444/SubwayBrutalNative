#include "hooks.h"
#include "memory.h"
#include "offsets.h"
#include <android/log.h>
#include <cstring>
#include <dobby.h>

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

    static void* (*orig_PlayerUpdate)(void* thiz) = nullptr;
    static void* (*orig_TakeDamage)(void* thiz, int damage) = nullptr;
    static void* (*orig_Die)(void* thiz) = nullptr;
    static void* (*orig_Jump)(void* thiz) = nullptr;
    static void* (*orig_AddCoins)(void* thiz, int count) = nullptr;
    static void* (*orig_AddScore)(void* thiz, int amount) = nullptr;
    static void* (*orig_ApplyFlightPhysics)(void* thiz) = nullptr;
    static void* (*orig_CollisionEnter)(void* thiz, void* collision) = nullptr;
    static void* (*orig_UpdateSpeed)(void* thiz, float dt) = nullptr;

    static void* hook_PlayerUpdate(void* thiz) {
        if (!thiz) {
            if (orig_PlayerUpdate) return orig_PlayerUpdate(thiz);
            return nullptr;
        }
        if (g_speedHack.load()) {
            Memory::WriteFloat((uintptr_t)thiz + OFFSET_CURRENT_SPEED, SPEED_MULTIPLIER * 10.0f);
        }
        if (g_superJump.load()) {
            Memory::WriteFloat((uintptr_t)thiz + OFFSET_MAX_JUMP_HEIGHT, SUPER_JUMP_HEIGHT);
        }
        if (g_infiniteJump.load()) {
            Memory::WriteInt((uintptr_t)thiz + OFFSET_JUMP_COUNT, 0);
            Memory::WriteInt((uintptr_t)thiz + OFFSET_MAX_JUMPS, MAX_JUMPS_INFINITE);
        }
        if (orig_PlayerUpdate) return orig_PlayerUpdate(thiz);
        return nullptr;
    }

    static void* hook_TakeDamage(void* thiz, int damage) {
        if (!thiz) return nullptr;
        if (g_godMode.load()) {
            Memory::WriteBool((uintptr_t)thiz + OFFSET_IS_INVULNERABLE, true);
            int maxH = Memory::ReadInt((uintptr_t)thiz + OFFSET_MAX_HEALTH);
            if (maxH > 0) Memory::WriteInt((uintptr_t)thiz + OFFSET_CURRENT_HEALTH, maxH);
            return nullptr;
        }
        if (orig_TakeDamage) return orig_TakeDamage(thiz, damage);
        return nullptr;
    }

    static void* hook_Die(void* thiz) {
        if (!thiz) return nullptr;
        if (g_godMode.load()) {
            Memory::WriteBool((uintptr_t)thiz + OFFSET_IS_DEAD, false);
            return nullptr;
        }
        if (orig_Die) return orig_Die(thiz);
        return nullptr;
    }

    static void* hook_AddCoins(void* thiz, int count) {
        if (!thiz) {
            if (orig_AddCoins) return orig_AddCoins(thiz, count);
            return nullptr;
        }
        if (g_unlimitedCoins.load()) {
            if (orig_AddCoins) return orig_AddCoins(thiz, COINS_ADD_AMOUNT);
            return nullptr;
        }
        if (orig_AddCoins) return orig_AddCoins(thiz, count);
        return nullptr;
    }

    static void* hook_AddScore(void* thiz, int amount) {
        if (!thiz) {
            if (orig_AddScore) return orig_AddScore(thiz, amount);
            return nullptr;
        }
        if (g_autoScore.load()) {
            if (orig_AddScore) return orig_AddScore(thiz, SCORE_ADD_AMOUNT);
            return nullptr;
        }
        if (orig_AddScore) return orig_AddScore(thiz, amount);
        return nullptr;
    }

    static void* hook_Jump(void* thiz) {
        if (!thiz) {
            if (orig_Jump) return orig_Jump(thiz);
            return nullptr;
        }
        if (g_infiniteJump.load()) {
            Memory::WriteInt((uintptr_t)thiz + OFFSET_JUMP_COUNT, 0);
        }
        if (orig_Jump) return orig_Jump(thiz);
        return nullptr;
    }

    static void* hook_ApplyFlightPhysics(void* thiz) {
        if (!thiz) {
            if (orig_ApplyFlightPhysics) return orig_ApplyFlightPhysics(thiz);
            return nullptr;
        }
        if (g_infiniteJetpack.load()) {
            Memory::WriteFloat((uintptr_t)thiz + OFFSET_MAX_FUEL, FUEL_MAX_VALUE);
            Memory::WriteFloat((uintptr_t)thiz + OFFSET_CURRENT_FUEL, FUEL_MAX_VALUE);
            Memory::WriteBool((uintptr_t)thiz + OFFSET_IS_FLYING, true);
        }
        if (orig_ApplyFlightPhysics) return orig_ApplyFlightPhysics(thiz);
        return nullptr;
    }

    static void* hook_CollisionEnter(void* thiz, void* collision) {
        if (!thiz || !collision) {
            if (orig_CollisionEnter) return orig_CollisionEnter(thiz, collision);
            return nullptr;
        }
        if (g_noObstacles.load()) return nullptr;
        if (orig_CollisionEnter) return orig_CollisionEnter(thiz, collision);
        return nullptr;
    }

    static void* hook_UpdateSpeed(void* thiz, float dt) {
        if (!thiz) {
            if (orig_UpdateSpeed) return orig_UpdateSpeed(thiz, dt);
            return nullptr;
        }
        if (g_speedHack.load()) {
            float baseSpeed = Memory::ReadFloat((uintptr_t)thiz + OFFSET_BASE_RUN_SPEED);
            if (baseSpeed < 1.0f) baseSpeed = 5.0f;
            Memory::WriteFloat((uintptr_t)thiz + OFFSET_MAX_RUN_SPEED, baseSpeed * SPEED_MULTIPLIER);
            Memory::WriteFloat((uintptr_t)thiz + OFFSET_CURRENT_SPEED, baseSpeed * SPEED_MULTIPLIER);
        }
        if (orig_UpdateSpeed) return orig_UpdateSpeed(thiz, dt);
        return nullptr;
    }

    bool InstallHooks(uintptr_t base) {
        if (base == 0) {
            LOGE("InstallHooks: base is 0");
            return false;
        }
        bool allOk = true;
        int result;

        #define HOOK_FN(rva, hookFn, origPtr) do { \
            uintptr_t addr = base + (rva); \
            result = DobbyHook(reinterpret_cast<void*>(addr), \
                reinterpret_cast<void*>(hookFn), \
                reinterpret_cast<void**>(&(origPtr))); \
            if (result != 0) { \
                LOGE("Hook FAILED: " #hookFn " at 0x%lx", (unsigned long)addr); \
                (origPtr) = nullptr; \
                allOk = false; \
            } else { \
                LOGI("Hook OK: " #hookFn " at 0x%lx", (unsigned long)addr); \
            } \
        } while(0)

        HOOK_FN(RVA_PLAYER_UPDATE, hook_PlayerUpdate, orig_PlayerUpdate);
        HOOK_FN(RVA_TAKE_DAMAGE, hook_TakeDamage, orig_TakeDamage);
        HOOK_FN(RVA_DIE, hook_Die, orig_Die);
        HOOK_FN(RVA_JUMP, hook_Jump, orig_Jump);
        HOOK_FN(RVA_ADD_COINS, hook_AddCoins, orig_AddCoins);
        HOOK_FN(RVA_ADD_SCORE, hook_AddScore, orig_AddScore);
        HOOK_FN(RVA_APPLY_FLIGHT_PHYSICS, hook_ApplyFlightPhysics, orig_ApplyFlightPhysics);
        HOOK_FN(RVA_COLLISION_ENTER, hook_CollisionEnter, orig_CollisionEnter);
        HOOK_FN(RVA_UPDATE_SPEED, hook_UpdateSpeed, orig_UpdateSpeed);

        #undef HOOK_FN

        LOGI("InstallHooks done. AllOk=%d", allOk ? 1 : 0);
        return allOk;
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
        const uint8_t BX_LR[] = {0x1E, 0xFF, 0x2F, 0xE1};
        Memory::PatchBytes(base + RVA_GAME_OVER, BX_LR, sizeof(BX_LR));
        LOGI("ApplyPatches done");
    }

    void TickUpdate() {
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
