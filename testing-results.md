REALWORLD SURVIVAL 3D - Testing Results
=======================================

DATE: 2026-11-11
APK UNDER TEST: RealWorldSurvival3D.apk (versionCode 2, versionName 1.1)
PROJECT: /data/data/com.termux/files/home/realworld-survival-3d/

WHAT WAS ACTUALLY TESTED VS NOT
-------------------------------
TESTED (real commands, real results, this environment):
  1. JVM game-logic test suite ............ PASS
  2. Full Java compilation (17 classes) ... PASS
  3. d8 dex conversion .................... PASS
  4. aapt2 resource compile + link ........ PASS
  5. APK signing (v1+v2+v3) ............... PASS
  6. apksigner verify ..................... PASS
  7. aapt2 dump badging (manifest) ........ PASS
  8. APK content inventory (unzip -l) ..... PASS
NOT TESTED (impossible in this environment - no adb, no emulator,
cannot self-install APKs):
  X. On-device install / launch
  X. Touch controls on real hardware
  X. Rendering performance / FPS on device
  X. Audio playback on device
  X. Activity lifecycle (rotate, HOME, interrupt)

1. JVM LOGIC TEST SUITE: PASS
-----------------------------
Command: sh tests/run.sh
Result:  2,432 checks passed, 0 failed (exit 0)
Scope:   engine-free classes on a plain JVM: Player physics, HunterEnemy AI,
         WorldMap topology, Obstacle, Item.
Coverage highlights:
  - Tower Alpha roof walk keeps the player grounded and on the roof
  - Jump/double-jump mechanics; no triple jump
  - 18m fall Alpha->Spire: damage without roll, ZERO damage with roll
  - Hazard street: lethal on sustained contact (ticking damage)
  - Wall-run cannot trigger while standing on a roof (regression test)
  - Enemy patrol stays on its roof for 20 simulated seconds (1,200 frames)
  - Same-roof chase closes distance; enemy never hovers over the void
  - Enemy cannot cross a 70m chasm without a leap path
  - Ballistic leap resolves and lands at roof height
  - Drop-stomp defeats a chaser, bounces the runner, awards 150 score
  - Item effects (medkit heal, drink refill, drive currency)
Bugs these tests caught and that were FIXED in this cycle:
  1. Wall push-out fired while standing/landing on roofs (shoved players
     off rooftops) -> now requires feet below the obstacle top.
  2. Wall-run triggered while standing on a roof (player inside the
     building's own footprint) -> footprint + top-margin gating added.
  3. Hazard pool dealt 35 damage PER FRAME (instant death, no chance to
     escape) -> 1s cooldown between ticks added.
  4. Movement code overwrote the DEAD / HARD_LAND / SAFETY_ROLL states
     set during landing (death was silently reverted) -> state-tail guard.
  5. Stomp contact band unreachable from the test's spawn height ->
     test corrected (game contact rule unchanged: |dy| < 1.6m).

2. JAVA COMPILATION: PASS
-------------------------
Command: javac -source 8 -target 8 -Xlint:-options -classpath
         ~/.android-sdk/platforms/android-30/android.jar
Result:  17 source files -> 69 class files, 0 errors, 0 warnings
         (only the expected bootstrap-classpath Note, filtered as expected)

3. DEX CONVERSION: PASS
-----------------------
Command: d8 --release --lib <android.jar> --output dex_out obj/...
Result:  classes.dex built, 0 errors, 0 warnings after adding --lib
         (previous builds logged desugaring warnings; fixed)

4. RESOURCES + APK LINK: PASS
-----------------------------
aapt2 compile: theme/strings/icon mipmaps compiled (res_compiled/*.flat)
aapt2 link:    app_unsigned.apk with full assets/ bundle; -0 .wav keeps
               audio uncompressed for AssetFileDescriptor streaming

5. SIGNING: PASS
----------------
apksigner sign with the Android debug keystore
(alias androiddebugkey, standard debug password - NOT a release credential)

6. APK VERIFICATION: PASS
-------------------------
apksigner verify --verbose:
  Verified using v1 scheme (JAR signing):            true
  Verified using v2 scheme (APK Signature Scheme v2): true
  Verified using v3 scheme (APK Signature Scheme v3): true
  Number of signers: 1

7. MANIFEST BADGING: PASS
-------------------------
  package: name='com.devagent.realworldsurvival3d' versionCode='2'
  versionName='1.1'  compileSdkVersion='30'
  minSdkVersion:'24'  targetSdkVersion:'34'
  uses-permission: android.permission.VIBRATE   (only permission)
  application-label: 'REALWORLD SURVIVAL 3D'
  launchable-activity: com.devagent.realworldsurvival3d.GameActivity
  uses-gl-es: 0x20000 (GLES 2.0)
  landscape orientation, touchscreen feature
  densities: 160/240/320/480/640

8. APK CONTENT INVENTORY: PASS
------------------------------
Size: 22,007,694 bytes (~21 MB) - down from the stale 169 MB build
Entries (51 total):
  - classes.dex
  - 5 music loops  (assets/audio/music/*.wav, 3,969,044 bytes each,
    90s seamless 22.05kHz mono loops)
  - 17 SFX         (assets/audio/sfx/*.wav)
  - 8 textures     (assets/textures/*.png, power-of-two)
  - 6 OBJ prop models (assets/models/*.obj)
  - 1 level data file (assets/data/parkour_levels.json)
  - res/ theme, strings, icon mipmaps (mdpi..xxxhdpi)

RUNTIME FEATURES NOT VERIFIED ON DEVICE (checklist for a future device test)
--------------------------------------------------------------------------
[ ] Install + launch on a physical device/emulator
[ ] Main menu navigation and Settings persistence across restarts
[ ] New Game / Continue restore flow
[ ] Touch joystick + camera swipe feel (sensitivity, multi-touch)
[ ] All parkour actions (jump, double jump, wall-run, wall kick, mantle,
    vault, slide, safety roll) with real touch buttons
[ ] Enemy chase/leap/attack behavior at 30 and 60 FPS settings
[ ] Data-drive interact pickups + extraction console prompt
[ ] Mission complete -> next mission progression
[ ] Game over on death / hazard fall
[ ] Music tracks switching per mission; SFX and music volumes
[ ] Damage flash, haptics, breathing/heartbeat loops
[ ] Save on pause/quit; Continue after app kill
[ ] BACK key pause/resume behavior
[ ] Frame-rate: 30 FPS limiter vs uncapped; thermal behavior
[ ] GLES2 shader compile on the device GPU (ARM/Adreno/Mali variants)

KNOWN OPEN ISSUES
-----------------
1. No device verification possible in this environment (see top of file).
2. Debug-signed APK only - not publishable to Play Store.
3. Menu/HUD text uses Android View overlays (not in-GL text).
