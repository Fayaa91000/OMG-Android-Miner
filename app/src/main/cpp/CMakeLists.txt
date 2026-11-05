cmake_minimum_required(VERSION 3.22.1)

project(omgminer LANGUAGES C CXX)

# Build XMRig's libuv dependency for Android ARM64.
set(LIBUV_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LIBUV_BUILD_SHARED OFF CACHE BOOL "" FORCE)
set(ENABLE_CLANG_TIDY OFF CACHE BOOL "" FORCE)

add_subdirectory(
    "${CMAKE_CURRENT_LIST_DIR}/../../../../native/libuv"
    "${CMAKE_CURRENT_BINARY_DIR}/libuv"
)

# Preserve our existing Android JNI bridge.
add_library(omgminer SHARED native_bridge.cpp)

find_library(android_log log)

# Import OpenSSL libraries built by GitHub Actions.
set(OMG_OPENSSL_ROOT "$ENV{RUNNER_TEMP}/omg-openssl-arm64")

if(EXISTS "${OMG_OPENSSL_ROOT}/lib/libssl.a"
   AND EXISTS "${OMG_OPENSSL_ROOT}/lib/libcrypto.a")

    add_library(omg_ssl STATIC IMPORTED)
    set_target_properties(omg_ssl PROPERTIES
        IMPORTED_LOCATION "${OMG_OPENSSL_ROOT}/lib/libssl.a")

    add_library(omg_crypto STATIC IMPORTED)
    set_target_properties(omg_crypto PROPERTIES
        IMPORTED_LOCATION "${OMG_OPENSSL_ROOT}/lib/libcrypto.a")

    target_link_libraries(omgminer PRIVATE
        uv_a omg_ssl omg_crypto ${android_log})
else()
    message(FATAL_ERROR "OMG Android ARM64 OpenSSL libraries not found")
endif()
