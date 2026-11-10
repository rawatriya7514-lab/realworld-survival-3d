REALWORLD SURVIVAL 3D - Known Limitations
==========================================

BUILD ENVIRONMENT
-----------------
- Godot 4.7.2-stable Linux ARM64 binary downloaded (138 MB) but CANNOT execute
  in Termux sandbox: execve() fails with "required file not found"
- Cannot generate Godot-based APK directly from this environment
- Native Android APK built successfully using aapt2/d8/apkSigner toolchain

APK INSTALLATION
----------------
- APK built, signed (v2, v3 schemes), and verified as valid
- Cannot auto-install on this device: Package manager blocked by SELinux
  from reading APK in Termux private directory
- No writable system location accessible for APK staging
- No user interaction available to accept installation dialog
- APK can be installed manually on any Android device via:
  adb install RealWorldSurvival3D.apk

GODOT PROJECT LIMITATIONS
-------------------------
- Runtime testing not performed (Godot binary blocked)
- No audio files present (audio/ directory empty)
- No font files present (fonts/ directory empty)
- No textures or materials assigned to MeshInstance3D nodes
- No collision shapes assigned to CollisionShape3D nodes
- Scenes require Godot Editor to assign assets and verify import

NATIVE ANDROID GAME LIMITATIONS
-------------------------------
- OpenGL ES 1.0 fixed-pipeline (not modern shader-based)
- Procedural colored cubes (no mesh files)
- Basic collision (distance-based, no physics engine)
- Simple enemy AI (no navigation mesh)
- No sound effects (audio files not included)
- Save system uses SharedPreferences (limited to simple key-value)

PERFORMANCE LIMITATIONS
-----------------------
- No device testing performed
- FPS not measured on actual hardware
- Memory usage not measured
- Battery impact not measured
