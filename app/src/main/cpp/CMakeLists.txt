cmake_minimum_required(VERSION 3.22.1)
project(subwaybrutal)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

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
    android
    log
    EGL
    GLESv2
)

target_include_directories(
    subwaybrutal PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_options(
    subwaybrutal PRIVATE
    -O2
    -fvisibility=hidden
    -fno-exceptions
    -fno-rtti
    -Wall
    -Wno-unused-parameter
    -Wno-unused-variable
    -Wno-unused-function
)

add_executable(
    ffinjector
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../injector/injector.cpp
)

target_link_libraries(ffinjector log)
target_compile_options(ffinjector PRIVATE -O2)
