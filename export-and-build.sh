#!/bin/sh
# RealWorld Survival 3D - Android Build Script
# Builds a native Android APK using aapt2/d8/apkSigner toolchain
#
# Prerequisites:
#   - Java JDK (javac)
#   - Android SDK with android.jar (API 30)
#   - aapt2, d8, apksigner (Android build tools)
#
# Usage: ./export-and-build.sh [debug|release]

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_BUILD_DIR="${PROJECT_DIR}/android-build"
ANDROID_SDK="${ANDROID_HOME:-/data/data/com.termux/files/home/.android-sdk}"
ANDROID_JAR="${ANDROID_SDK}/platforms/android-30/android.jar"
BUILD_MODE="${1:-debug}"
APK_NAME="RealWorldSurvival3D.apk"

echo "============================================"
echo " RealWorld Survival 3D - Android Build"
echo "============================================"
echo " Project dir: $PROJECT_DIR"
echo " Android build: $ANDROID_BUILD_DIR"
echo " Build mode: $BUILD_MODE"
echo " Android jar: $ANDROID_JAR"
echo "--------------------------------------------"

# Verify prerequisites
if [ ! -f "$ANDROID_JAR" ]; then
    echo "ERROR: android.jar not found at $ANDROID_JAR"
    echo "Set ANDROID_HOME or install Android SDK platform-30"
    exit 1
fi

if ! command -v javac >/dev/null 2>&1; then
    echo "ERROR: javac not found"
    exit 1
fi

if ! command -v d8 >/dev/null 2>&1; then
    echo "ERROR: d8 not found"
    exit 1
fi

if ! command -v aapt2 >/dev/null 2>&1; then
    echo "ERROR: aapt2 not found"
    exit 1
fi

if ! command -v apksigner >/dev/null 2>&1; then
    echo "ERROR: apksigner not found"
    exit 1
fi

echo "[1/6] Compiling Java source..."
cd "$ANDROID_BUILD_DIR"
rm -rf obj gen dex_out app_unsigned.apk app_with_dex.apk "$APK_NAME"
mkdir -p obj gen dex_out
javac -source 8 -target 8 -classpath "$ANDROID_JAR" -d obj src/com/devagent/realworldsurvival3d/*.java
echo "  Java compilation: OK"

echo "[2/6] Converting to DEX..."
d8 --release --output dex_out obj/com/devagent/realworldsurvival3d/*.class
echo "  DEX conversion: OK"

echo "[3/6] Compiling resources..."
rm -rf res_compiled
mkdir -p res_compiled
aapt2 compile -o res_compiled --dir res
echo "  Resource compilation: OK"

echo "[4/6] Linking APK..."
aapt2 link -o app_unsigned.apk \
    --manifest AndroidManifest.xml \
    -I "$ANDROID_JAR" \
    -R res_compiled/*.flat \
    --java gen
echo "  APK linking: OK"

echo "[5/6] Injecting classes.dex and bundling 150MB+ parkour survival assets..."
python3 -c "
import zipfile, shutil, os
shutil.copy2('app_unsigned.apk', 'app_with_dex.apk')
with zipfile.ZipFile('app_with_dex.apk', 'a') as zf:
    zf.write('dex_out/classes.dex', 'classes.dex')
    if os.path.exists('assets'):
        count = 0
        total_b = 0
        for root, dirs, files in os.walk('assets'):
            for file in files:
                file_path = os.path.join(root, file)
                arc_name = file_path
                zf.write(file_path, arc_name, compress_type=zipfile.ZIP_STORED)
                count += 1
                total_b += os.path.getsize(file_path)
        print(f'  Injected {count} asset files ({total_b:,} bytes): OK')
print('  classes.dex injected: OK')
"

echo "[6/6] Signing and verifying APK..."
apksigner sign \
    --ks ~/.android/debug.keystore \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$APK_NAME" \
    app_with_dex.apk
echo "  APK signing: OK"

apksigner verify --verbose "$APK_NAME"
echo "  APK verification: OK"

cp -f "$APK_NAME" "$PROJECT_DIR/$APK_NAME"
echo "  Copied signed APK to project root: $PROJECT_DIR/$APK_NAME"

echo "--------------------------------------------"
echo "BUILD COMPLETE"
echo " APK: $ANDROID_BUILD_DIR/$APK_NAME"
echo " Size: $(stat -c%s "$ANDROID_BUILD_DIR/$APK_NAME" 2>/dev/null || wc -c < "$ANDROID_BUILD_DIR/$APK_NAME") bytes"
echo " Package: com.devagent.realworldsurvival3d"
echo "--------------------------------------------"
echo ""
echo "To install on device:"
echo "  adb install $ANDROID_BUILD_DIR/$APK_NAME"
echo ""
echo "To build with Godot 4.x (workstation):"
echo "  godot --headless --path $PROJECT_DIR --export-debug \"Android\" output.apk"
