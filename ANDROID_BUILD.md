# REALWORLD 3D — Android Build

Build is identical to the proven pipeline of this repository (see
`build-instructions.md` for the long form).

## One command
```bash
cd /data/data/com.termux/files/home/realworld-survival-3d
./export-and-build.sh debug
```
Output: `RealWorldSurvival3D.apk` (project root + `android-build/`).

## Pipeline
1. `javac` (17+ classes → obj/) against android-30 jar
2. `d8 --lib android.jar` → classes.dex
3. `aapt2 compile` + `aapt2 link` (manifest, res, full assets, WAVs stored)
4. Python dex-inject + 4-byte zipalign
5. `apksigner sign` (debug keystore; v1+v2+v3) + `apksigner verify`

## Install
- `adb install -r RealWorldSurvival3D.apk` (workstation), or
- copy APK to device and open it (allow unknown apps), or
- on-device Termux: `termux-open RealWorldSurvival3D.apk`

## Unit tests (no device needed)
```bash
sh tests/run.sh
```

## Requirements
- Android 7.0+ (minSdk 24), GLES 2.0, landscape, ~24 MB storage.
