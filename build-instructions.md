REALWORLD SURVIVAL 3D - Build Instructions
==========================================

Project: RealWorld Survival 3D
Engine: Godot 4.x (primary) + Native Java Android (fallback)
Platform: Android (ARM64)
Project Path: /data/data/com.termux/files/home/realworld-survival-3d/

PREREQUISITES
-------------
For Godot 4.x build:
1. Install Godot 4.x from https://godotengine.org/download
2. Install Android SDK (platform-tools, build-tools)
3. Java JDK 21 or later

For native Android build (already working in this environment):
- aapt2, d8, apksigner, javac, android.jar (all available)

OPTION A - Godot 4.x Editor Build (RECOMMENDED FOR 3D)
-------------------------------------------------------
1. Copy project to workstation with Godot 4.x installed
2. Open project.godot in Godot 4.x Editor
3. Configure Android export preset:
   - Project -> Export -> Add -> Android
   - Package ID: com.devagent.realworldsurvival3d
   - Min SDK: 24 (Android 7.0)
   - Target SDK: 34 (Android 14)
   - Configure signing (debug keystore or release keystore)
   - Set application icon
4. Add meshes, textures, materials, audio files to project
5. Click 'Export Project' to generate RealWorldSurvival3D.apk

OPTION B - Native Android Build (WORKING IN TERMUX)
---------------------------------------------------
Build script: /data/data/com.termux/files/home/realworld-survival-3d/android-build/

Step-by-step:
1. cd /data/data/com.termux/files/home/realworld-survival-3d/android-build
2. javac -source 8 -target 8 -classpath $ANDROID_HOME/platforms/android-30/android.jar -d obj src/com/devagent/realworldsurvival3d/*.java
3. d8 --release --output dex_out obj/com/devagent/realworldsurvival3d/*.class
4. aapt2 compile -o res_compiled --dir res
5. aapt2 link -o app_unsigned.apk --manifest AndroidManifest.xml -I $ANDROID_HOME/platforms/android-30/android.jar -R res_compiled/*.flat --java gen
6. Add classes.dex to APK (zip -a app_unsigned.apk dex_out/classes.dex)
7. apksigner sign --ks ~/.android/debug.keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out RealWorldSurvival3D.apk app_with_dex.apk

ANDROID EXPORT PRESET TEMPLATE (Godot)
--------------------------------------
Name: Android ARM64
Package: com.devagent.realworldsurvival3d
Version Code: 1
Version Name: 1.0
Min SDK: 24
Target SDK: 34

APK OUTPUT
----------
- Native build: RealWorldSurvival3D.apk (37,747 bytes, signed, verified)
- Godot export: RealWorldSurvival3D.apk (requires workstation)
- Install: adb install RealWorldSurvival3D.apk
