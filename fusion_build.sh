#!/usr/bin/env bash
# 融合版构建脚本
# 用法: ./fusion_build.sh [debug|release|clone|nightly]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

BUILD_TYPE="${1:-debug}"
APK_OUTPUT="build/app/outputs/apk/${BUILD_TYPE}"

echo "============================================"
echo "  Kelivo × Operit 融合版构建"
echo "  构建类型: ${BUILD_TYPE}"
echo "============================================"

# 前置检查
for cmd in flutter java unzip; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "错误: 缺少必要工具: $cmd"
    exit 1
  fi
done

# 1. 获取依赖
echo ""
echo "[1/5] 获取 Flutter 依赖..."
flutter pub get

# 2. 检查 NDK 和 CMake
echo ""
echo "[2/5] 检查 Android 环境..."
if [[ ! -d "$ANDROID_HOME/ndk/28.2.13676358" ]]; then
  echo "  警告: NDK 28.2.13676358 未找到，尝试使用 Flutter 默认配置"
fi

# 3. 构建 APK
echo ""
echo "[3/5] 开始构建 APK (${BUILD_TYPE})..."
flutter build apk --"${BUILD_TYPE}" \
  --target-platform android-arm64 \
  --dart-define=FUSION_BUILD_TYPE="${BUILD_TYPE}" \
  2>&1

# 4. 检查产物
echo ""
echo "[4/5] 检查构建产物..."
APK_PATH="build/app/outputs/apk/${BUILD_TYPE}/app-${BUILD_TYPE}.apk"
if [[ -f "$APK_PATH" ]]; then
  APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
  echo "  APK: $APK_PATH ($APK_SIZE)"
  
  # 显示 APK 信息
  if command -v aapt2 >/dev/null 2>&1; then
    echo ""
    echo "  APK 详细信息:"
    aapt2 dump badging "$APK_PATH" 2>/dev/null | grep -E "package:|sdkVersion:|targetSdkVersion:" | head -3
  fi
else
  echo "  错误: APK 未生成"
  exit 1
fi

# 5. 显示构建结果
echo ""
echo "[5/5] 构建完成!"
echo ""
echo "============================================"
echo "  产物: $APK_PATH"
echo "  大小: $APK_SIZE"
echo "============================================"
echo ""
echo "安装命令:"
echo "  adb install -r $APK_PATH"
echo ""
echo "签名命令 (如果有 keystore.properties):"
echo "  apksigner sign --ks android/app/ks.jks --out signed.apk $APK_PATH"
