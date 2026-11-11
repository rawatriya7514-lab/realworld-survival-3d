REALWORLD SURVIVAL 3D - Build Instructions
==========================================

Project: RealWorld Survival 3D (native Android)
Platform: Android (arm64-v8a / armeabi-v7a universal dex)
Project Path: /data/data/com.termux/files/home/realworld-survival-3d/

IMPORTANT: The shipped game is the NATIVE JAVA ANDROID APP in android-build/.
The Godot 4.x skeleton (project.godot, scenes/, scripts/) is unused reference
material: the Godot binary cannot execute in this Termux sandbox, and no
export templates are installed. All build steps below are native-toolchain.

PREREQUISITES (all present in this environment)
-----------------------------------------------
- JDK 21 (javac) with -source 8 -target 8 for maximum device compatibility
- Android SDK platform android-30 (android.jar at ~/.android-sdk/platforms/)
- Build tools: aapt2, d8, apksigner (on PATH in Termux)
- Python 3 (build script uses it for dex injection + 4-byte zipalign)
- Debug keystore at ~/.android/debug.keystore (auto-created by keytool usage;
  standard debug credentials: alias androiddebugkey, store/key pass "android")

ONE-COMMAND BUILD
-----------------
    cd /data/data/com.termux/files/home/realworld-survival-3d
    ./export-and-build.sh debug

Output: RealWorldSurvival3D.apk (project root AND android-build/),
~21 MB, signed v1+v2+v3, verified with apksigner verify.

PIPELINE STAGES (what the script does)
--------------------------------------
1. javac  - compiles android-build/src/com/devagent/realworldsurvival3d/*.java
   (17 classes) against android-30 android.jar into obj/
2. d8     - converts .class files to classes.dex
   (--lib android.jar so interface desugaring has the full classpath)
3. aapt2 compile - compiles res/ (theme, strings, icon mipmaps)
4. aapt2 link    - links manifest + resources + full assets/ bundle
   (-A assets, -0 .wav keeps WAVs uncompressed for direct AssetFileDescriptor
   streaming; generates gen/R.java)
5. Python step   - injects classes.dex into the APK and performs a 4-byte
   zipalign (pure-python implementation, no zipalign binary needed)
6. apksigner sign - signs with the debug keystore using v1, v2 and v3 schemes
   (--min-sdk-version 24), then apksigner verify --verbose confirms validity

MANUAL STEPS (equivalent, for debugging)
----------------------------------------
    cd android-build
    javac -source 8 -target 8 -Xlint:-options \
      -classpath ~/.android-sdk/platforms/android-30/android.jar \
      -d obj src/com/devagent/realworldsurvival3d/*.java
    d8 --release --lib ~/.android-sdk/platforms/android-30/android.jar \
      --output dex_out obj/com/devagent/realworldsurvival3d/*.class
    aapt2 compile -o res_compiled --dir res
    aapt2 link -o app_unsigned.apk --manifest AndroidManifest.xml \
      -I ~/.android-sdk/platforms/android-30/android.jar \
      -R res_compiled/*.flat -A assets -0 .wav --java gen
    # inject dex + zipalign + sign (see export-and-build.sh steps 5-6)

UNIT TESTS (JVM, no device required)
------------------------------------
    sh tests/run.sh
Compiles the engine-free classes (Player, WorldMap, HunterEnemy, Obstacle,
Item) plus GameLogicTest and runs 2,432 assertions covering landing/fall
damage, safety rolls, hazard-pool lethality, wall-run gating, enemy patrol
containment, leap resolution, stomps, and item effects.
Exit code 0 = all pass.

RELEASE BUILD
-------------
./export-and-build.sh release is reserved for a future release keystore.
Until a release keystore is created, the script builds the debug-signed APK
(suitable for personal sideloading; not for Play Store upload).

INSTALL
-------
    adb install -r RealWorldSurvival3D.apk
or copy the APK to a device and open it (requires "install unknown apps").
On this Termux device: termux-open RealWorldSurvival3D.apk hands the APK to
the Android package installer.

VERIFY A BUILT APK
------------------
    apksigner verify --verbose RealWorldSurvival3D.apk
    aapt2 dump badging RealWorldSurvival3D.apk
    unzip -l RealWorldSurvival3D.apk | grep assets/
