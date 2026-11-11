#!/bin/sh
# RealWorld Survival 3D - Android Build Script
# Builds a native Android APK using aapt2/d8/zipalign/apksigner toolchain
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

if [ ! -f "$ANDROID_JAR" ]; then
    echo "ERROR: android.jar not found at $ANDROID_JAR"
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

cd "$ANDROID_BUILD_DIR"
rm -rf obj gen dex_out app_unsigned.apk app_with_dex.apk app_aligned.apk "$APK_NAME"
mkdir -p obj gen dex_out

echo "[1/6] Compiling Java source..."
javac -source 8 -target 8 -classpath "$ANDROID_JAR" -d obj src/com/devagent/realworldsurvival3d/*.java
echo "  Java compilation: OK"

echo "[2/6] Converting to DEX..."
d8 --release --lib "$ANDROID_JAR" --output dex_out obj/com/devagent/realworldsurvival3d/*.class
echo "  DEX conversion: OK"

echo "[3/6] Compiling resources..."
rm -rf res_compiled
mkdir -p res_compiled
aapt2 compile -o res_compiled --dir res
echo "  Resource compilation: OK"

echo "[4/6] Linking APK with full asset bundle..."
aapt2 link -o app_unsigned.apk \
    --manifest AndroidManifest.xml \
    -I "$ANDROID_JAR" \
    -R res_compiled/*.flat \
    -A assets \
    -0 .wav \
    --java gen
echo "  APK linking: OK"

echo "[5/6] Injecting classes.dex and performing 4-byte zipalign..."
python3 -c "
import zipfile, shutil, os, struct

# 1. Inject classes.dex into unaligned apk
shutil.copy2('app_unsigned.apk', 'app_with_dex.apk')
with zipfile.ZipFile('app_with_dex.apk', 'a', compression=zipfile.ZIP_DEFLATED) as zf:
    zf.write('dex_out/classes.dex', 'classes.dex')
print('  classes.dex injected: OK')

# 2. Perform 4-byte zipalign
def zipalign_apk(in_apk, out_apk, alignment=4):
    with open(in_apk, 'rb') as fin, open(out_apk, 'wb') as fout:
        fin.seek(-22, 2)
        eocd = fin.read(22)
        cd_offset = int.from_bytes(eocd[16:20], 'little')
        total_entries = int.from_bytes(eocd[10:12], 'little')
        
        fin.seek(cd_offset)
        cd_entries = []
        for _ in range(total_entries):
            hdr = fin.read(46)
            if len(hdr) < 46 or hdr[:4] != b'PK\x01\x02':
                break
            fn_len = int.from_bytes(hdr[28:30], 'little')
            ex_len = int.from_bytes(hdr[30:32], 'little')
            cm_len = int.from_bytes(hdr[32:34], 'little')
            fn = fin.read(fn_len)
            ex = fin.read(ex_len)
            cm = fin.read(cm_len)
            cd_entries.append((hdr, fn, ex, cm))
        
        new_cd_entries = []
        for hdr, fn, ex, cm in cd_entries:
            old_local_offset = int.from_bytes(hdr[42:46], 'little')
            comp_method = int.from_bytes(hdr[10:12], 'little')
            compressed_size = int.from_bytes(hdr[20:24], 'little')
            
            fin.seek(old_local_offset)
            loc_hdr = bytearray(fin.read(30))
            loc_fn_len = int.from_bytes(loc_hdr[26:28], 'little')
            loc_ex_len = int.from_bytes(loc_hdr[28:30], 'little')
            loc_fn = fin.read(loc_fn_len)
            loc_ex = fin.read(loc_ex_len)
            data = fin.read(compressed_size)
            
            new_local_offset = fout.tell()
            new_loc_ex = bytearray(loc_ex)
            if comp_method == 0:
                base_len = 30 + loc_fn_len + len(new_loc_ex)
                rem = (new_local_offset + base_len) % alignment
                if rem != 0:
                    pad_len = alignment - rem
                    new_loc_ex.extend(b'\x00' * pad_len)
            
            loc_hdr[28:30] = len(new_loc_ex).to_bytes(2, 'little')
            fout.write(loc_hdr)
            fout.write(loc_fn)
            fout.write(new_loc_ex)
            fout.write(data)
            
            new_cd_hdr = bytearray(hdr)
            new_cd_hdr[30:32] = len(new_loc_ex).to_bytes(2, 'little')
            new_cd_hdr[42:46] = new_local_offset.to_bytes(4, 'little')
            new_cd_entries.append((new_cd_hdr, fn, new_loc_ex, cm))
            
        new_cd_offset = fout.tell()
        for cd_hdr, fn, ex, cm in new_cd_entries:
            fout.write(cd_hdr)
            fout.write(fn)
            fout.write(ex)
            fout.write(cm)
            
        new_cd_size = fout.tell() - new_cd_offset
        new_eocd = bytearray(eocd)
        new_eocd[12:16] = new_cd_size.to_bytes(4, 'little')
        new_eocd[16:20] = new_cd_offset.to_bytes(4, 'little')
        fout.write(new_eocd)

zipalign_apk('app_with_dex.apk', 'app_aligned.apk')
print('  4-byte zipalign: OK')
"

echo "[6/6] Signing APK (v1, v2, v3 schemes)..."
apksigner sign \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --v3-signing-enabled true \
    --min-sdk-version 24 \
    --ks ~/.android/debug.keystore \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$APK_NAME" \
    app_aligned.apk
echo "  APK signing: OK"

apksigner verify --verbose --min-sdk-version 23 "$APK_NAME"
echo "  APK verification: OK"

cp -f "$APK_NAME" "$PROJECT_DIR/$APK_NAME"
echo "  Copied signed & aligned APK to project root: $PROJECT_DIR/$APK_NAME"

echo "--------------------------------------------"
echo "BUILD COMPLETE"
echo " APK: $ANDROID_BUILD_DIR/$APK_NAME"
echo " Size: $(stat -c%s "$ANDROID_BUILD_DIR/$APK_NAME" 2>/dev/null || wc -c < "$ANDROID_BUILD_DIR/$APK_NAME") bytes"
echo " Package: com.devagent.realworldsurvival3d"
echo "--------------------------------------------"
