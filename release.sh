#!/usr/bin/env bash
# Build & publish a HyperOS3EULocalization release locally.
# Replaces the GitHub Actions workflow when LFS bandwidth is exhausted.
#
# Usage: ./release.sh
# Requires: gh CLI authed as repo owner, JDK 17, Android SDK at $ANDROID_HOME

set -euo pipefail

# ---------- Config ----------
REPO="rhe91709-netizen/HyperOS3EULocalization"
KEEP_RELEASES=3
ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
BUILD_TOOLS="$ANDROID_HOME/build-tools/36.0.0"
DEBUG_KEYSTORE="${DEBUG_KEYSTORE:-$HOME/.android/debug.keystore}"
ROOT="$(cd "$(dirname "$0")" && pwd)"

# ---------- Sanity ----------
[ -d "$ANDROID_HOME" ] || { echo "ANDROID_HOME not found: $ANDROID_HOME"; exit 1; }
[ -x "$BUILD_TOOLS/apksigner" ] || { echo "apksigner missing: $BUILD_TOOLS/apksigner"; exit 1; }
[ -f "$DEBUG_KEYSTORE" ] || { echo "debug.keystore missing: $DEBUG_KEYSTORE"; exit 1; }
command -v gh >/dev/null || { echo "gh CLI missing"; exit 1; }
command -v zip >/dev/null || { echo "zip missing"; exit 1; }
export ANDROID_HOME ANDROID_SDK_ROOT="$ANDROID_HOME"

# ---------- Compute version + tag ----------
cd "$ROOT"
VERSION=$(grep versionName toolbox/app/build.gradle | sed -E 's/.*versionName "(.*)".*/\1/' | tr -d ' ')
SHORT=$(echo "$VERSION" | sed -E 's/^OS3\.0\.//' | sed -E 's/\.([A-Z][A-Z0-9]*)$/-\1/')
SHA=$(git rev-parse --short=7 HEAD)
TAG="v${SHORT}-${SHA}"
ZIP_NAME="HyperOS3EULocalization-${VERSION}.zip"
APK_NAME="HyperOS3_Toolbox_${TAG}.apk"

echo "Version : $VERSION"
echo "Tag     : $TAG"
echo "Zip     : $ZIP_NAME"
echo "APK     : $APK_NAME"

# ---------- Build & sign Toolbox APK ----------
echo
echo "==> Building toolbox APK..."
( cd toolbox && ./gradlew --console=plain assembleRelease )

UNSIGNED="$ROOT/toolbox/app/build/outputs/apk/release/app-release-unsigned.apk"
[ -f "$UNSIGNED" ] || { echo "unsigned APK missing: $UNSIGNED"; exit 1; }

echo "==> Signing APK..."
"$BUILD_TOOLS/apksigner" sign \
    --ks "$DEBUG_KEYSTORE" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$ROOT/$APK_NAME" \
    "$UNSIGNED"
rm -f "$ROOT/${APK_NAME}.idsig"

# ---------- Package module zip ----------
echo
echo "==> Packaging $ZIP_NAME..."
cd "$ROOT"
rm -f "$ZIP_NAME"
find . -name "._*" -delete 2>/dev/null || true
zip -rq "$ZIP_NAME" . \
    --exclude "./.git*" \
    --exclude "./toolbox/*" \
    --exclude "./.github/*" \
    --exclude "*/.DS_Store" \
    --exclude "*/._*" \
    --exclude "./debug.keystore" \
    --exclude "./HyperOS3_Toolbox_*" \
    --exclude "./release.sh" \
    --exclude "./HyperOS3EULocalization-*.zip"
ls -lh "$ZIP_NAME"

# ---------- Publish release ----------
echo
echo "==> Creating GitHub release $TAG..."
NOTES="## HyperOS 3 EU Localization — nezha (Xiaomi 17 Ultra)

### 功能 / Features
- 国行相册 v5.0（Flutter 架构）
- 国行相册编辑器 v4.2.5
- 电诈防护 / 通话防护 / AI 通话预警
- 安全守护 / 家人守护
- 地震预警、自然灾害预警订阅地区修复
- MiPush CN 区推送支持
- 小爱同学、负一屏、短信、传送门、黄页、通讯录
- 小米钱包 / MiPay / 日历 / 天气 / 音乐 / 录音机

### 刷入说明 / Flash Instructions
1. Magisk/KernelSU 刷入 \`.zip\`
2. 按音量键提示选择需要的功能
3. 安装 \`$APK_NAME\`，在 LSPosed 中启用并勾选相关应用
4. 重启设备

> Built locally · commit \`$SHA\`"

gh release create "$TAG" -R "$REPO" \
    --latest \
    --title "HyperOS3 EU Localization $VERSION" \
    --notes "$NOTES" \
    "$ZIP_NAME" \
    "$APK_NAME"

# ---------- Prune old releases ----------
echo
echo "==> Pruning to $KEEP_RELEASES newest releases..."
gh release list -R "$REPO" --limit 100 --json tagName,createdAt \
    --jq "sort_by(.createdAt) | reverse | .[$KEEP_RELEASES:] | .[].tagName" \
    | while read -r tag; do
        [ -z "$tag" ] && continue
        echo "  deleting $tag"
        gh release delete "$tag" -R "$REPO" --yes --cleanup-tag || true
      done

echo
echo "✓ Released $TAG"
echo "  https://github.com/$REPO/releases/tag/$TAG"
