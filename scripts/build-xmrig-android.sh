#!/usr/bin/env bash
set -euo pipefail

: "${ANDROID_NDK_ROOT:?Android NDK is required}"
: "${RUNNER_TEMP:?GitHub runner temporary directory is required}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEPS="$RUNNER_TEMP/omg-xmrig-deps"
NDK_TOOLCHAIN="$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake"

# Preserve the OpenSSL libraries built by GitHub Actions.
mkdir -p "$DEPS/lib" "$DEPS/include"
cp "$RUNNER_TEMP/omg-openssl-arm64/lib/"*.a "$DEPS/lib/"
cp -R "$RUNNER_TEMP/omg-openssl-arm64/include/." "$DEPS/include/"

# Build libuv for Android ARM64.
cmake -S "$ROOT/native/libuv" -B "$RUNNER_TEMP/omg-uv-build" \
  -DCMAKE_TOOLCHAIN_FILE="$NDK_TOOLCHAIN" \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-26 \
  -DLIBUV_BUILD_TESTS=OFF \
  -DLIBUV_BUILD_SHARED=OFF

cmake --build "$RUNNER_TEMP/omg-uv-build" --target uv_a -j2

cp "$RUNNER_TEMP/omg-uv-build/libuv.a" "$DEPS/lib/libuv.a"
cp "$ROOT/native/libuv/include/uv.h" "$DEPS/include/"
cp -R "$ROOT/native/libuv/include/." "$DEPS/include/"

# Compile the actual XMRig engine for Android ARM64.
cmake -S "$ROOT/native/xmrig" -B "$RUNNER_TEMP/omg-xmrig-build" \
  -DCMAKE_TOOLCHAIN_FILE="$NDK_TOOLCHAIN" \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-26 \
  -DXMRIG_DEPS="$DEPS" \
  -DUV_LIBRARY="$DEPS/lib/libuv.a" \
  -DUV_INCLUDE_DIR="$DEPS/include" \
  -DOPENSSL_ROOT_DIR="$DEPS" \
  -DOPENSSL_USE_STATIC_LIBS=ON \
  -DWITH_HWLOC=OFF \
  -DWITH_OPENCL=OFF \
  -DWITH_CUDA=OFF \
  -DWITH_MSR=OFF \
  -DWITH_DMI=OFF \
  -DWITH_ASM=OFF \
  -DWITH_HTTP=OFF \
  -DWITH_TLS=ON

cmake --build "$RUNNER_TEMP/omg-xmrig-build" -j2
echo "OMG Android ARM64 XMRig compilation completed"
