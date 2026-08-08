#pragma once

#include <atomic>
#include <cstdint>

namespace Hacks {
    extern std::atomic<bool> g_unlimitedCoins;
    extern std::atomic<bool> g_godMode;
    extern std::atomic<bool> g_infiniteJump;
    extern std::atomic<bool> g_speedHack;
    extern std::atomic<bool> g_superJump;
    extern std::atomic<bool> g_infiniteJetpack;
    extern std::atomic<bool> g_noObstacles;
    extern std::atomic<bool> g_autoScore;
    extern std::atomic<bool> g_unlockAll;
    extern std::atomic<bool> g_antiCrash;

    bool InstallHooks(uintptr_t base);
    void RemoveHooks();
    void ApplyPatches(uintptr_t base);
    void TickUpdate();
}
