#pragma once

#include <cstdint>
#include <cstddef>

namespace Memory {
    bool PatchBytes(uintptr_t address, const uint8_t* bytes, size_t size);
    float ReadFloat(uintptr_t address);
    bool WriteFloat(uintptr_t address, float value);
    int ReadInt(uintptr_t address);
    bool WriteInt(uintptr_t address, int value);
    bool ReadBool(uintptr_t address);
    bool WriteBool(uintptr_t address, bool value);
    uint32_t ReadPointer(uintptr_t address);
    uintptr_t GetLibBase(const char* libName);
    uintptr_t GetIL2CppBase();
    void FlushCache(uintptr_t address, size_t size);
}
