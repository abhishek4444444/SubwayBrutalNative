cmake_minimum_required(VERSION 3.22.1)
project(subwaybrutal)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Use pre-built Dobby via FetchContent with STABLE tag
include(FetchContent)

FetchContent_Declare(
    Dobby
    GIT_REPOSITORY https://github.com/jmpews/Dobby.git
    GIT_TAG        v1.2.2
)

# Dobby config
set(DOBBY_BUILD_SHARED_LIBRARY OFF CACHE BOOL "" FORCE)
set(DOBBY_BUILD_EXAMPLE OFF CACHE BOOL "" FORCE)
set(DOBBY_BUILD_TEST OFF CACHE BOOL "" FORCE)
set(DOBBY_GENERATE_SHARED OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(Dobby)

# Main native library
add_library(
    subwaybrutal
    SHARED
    memory.cpp
    hooks.cpp
    socket_server.cpp
    bridge.cpp
    main.cpp
)

target_link_libraries(
    subwaybrutal
    dobby
    android
    log
    EGL
    GLESv2
)

target_include_directories(
    subwaybrutal PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${dobby_SOURCE_DIR}/include
)

target_compile_options(
    subwaybrutal PRIVATE
    -O2
    -fvisibility=hidden
    -fno-exceptions
    -fno-rtti
    -Wall
    -Wno-unused-parameter
)

# Injector binary
add_executable(
    ffinjector
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../injector/injector.cpp
)

target_link_libraries(
    ffinjector
    log
)

target_compile_options(
    ffinjector PRIVATE
    -O2
)
