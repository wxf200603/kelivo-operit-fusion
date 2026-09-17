cmake_minimum_required(VERSION 3.22.1)
project("kelivo_operit_fusion")

# ── Operit C++ 底层引入（可选） ──
# Operit 的 native 层包括 quickjs, terminal, 自定义库等。
# 如需引入 Operit 的 C++ 源码，取消下面注释并指定路径：
#
# set(OPERIT_SRC ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../operit-src/app/src/main/cpp)
# if(EXISTS ${OPERIT_SRC})
#     add_subdirectory(${OPERIT_SRC}/quickjs ${CMAKE_BINARY_DIR}/quickjs)
#     add_subdirectory(${OPERIT_SRC}/terminal ${CMAKE_BINARY_DIR}/terminal)
# endif()

# ── Native Bridge ──
# 用于 Dart FFI 直接调用原生能力（高性能场景，可选）
add_library(
    fusion_native_bridge
    SHARED
    native_bridge.cpp
)

target_link_libraries(
    fusion_native_bridge
    android
    log
)

# ── 引入 Operit 预编译 .so（如果有） ──
# Operit 提供预编译的 liboperit_ripgrep.so 等。
# 如需使用，放置到 jniLibs 目录，无需在此处链接。

# ── FFmpegKit（可选） ──
# 如需视频处理能力，引入 Operit 自定义 FFmpegKit AAR
# implementation(files("libs/ffmpeg-kit-local.aar"))
