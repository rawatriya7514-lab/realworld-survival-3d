# RealWorld Survival 3D v1.2.0 — Launch Fix & Visual Realism Overhaul

A major update that fixes installation and launch issues, and completely overhauls the game's 3D graphics for visual realism.

---

### What's New & Fixed in v1.2.0

#### 1. App Launch & Installation Fixes ("Game Chalu Ho Rhi Hai")
- **Menu Dismissal Fix**: Fixed the UI overlay bug where the main menu stayed on screen after tapping "Open World" or "Rooftop Parkour". Controls are now immediately responsive.
- **Rooftop Overview on Launch**: The 3D background behind the main menu now renders a panoramic rooftop view of the skyline instead of spawning inside a wall.
- **Complete v1 + v2 + v3 Signature Verification (`minSdkVersion=21`)**:
  - `Verified using v1 scheme (JAR signing): true`
  - `Verified using v2 scheme (APK Signature Scheme v2): true`
  - `Verified using v3 scheme (APK Signature Scheme v3): true`
  - Fixes "App not installed" or "Package parse error" across all Android OEMs (HyperOS, MIUI, ColorOS, OneUI, Vivo, Transsion, stock Android).

#### 2. Visual Realism Overhaul ("Real-Life Graphics Upgrade")
- **PBR-Style GLES2 World Shaders**:
  - Fresnel rim lighting outlines buildings, vehicles, and characters with crisp physical edge highlights.
  - Directional sunlight with warm specular highlights.
  - Atmospheric solar forward-scattering haze when looking toward the sun.
- **Dynamic 3D Sky Dome**:
  - Real 3D sun disc with radiant corona glare and Mie scattering.
  - High-altitude cirrus cloud layer and smooth horizon haze blending with the terrain.
- **High-Definition Procedural Terrain Atlas**:
  - **Meadow Grass**: Multi-frequency noise with mossy gradients and golden blades.
  - **Sedimentary Rock**: Horizontal strata layers with granite striations and rock crevices.
  - **Asphalt Highway**: Weathered bitumen texture with double yellow center divider lines and white shoulder edge markings.
  - **Sand**: Wind-blown dune ripples with warm amber highlights.
- **Architectural Skyline & City Depth**:
  - Buildings now feature roof ledges, cornices, rooftop mechanical penthouses, antenna masts, and blinking red aircraft warning beacons.
- **Detailed 3D Vehicles**:
  - **SUV**: Metallic cobalt finish, chrome front grille, tinted glass cabin, alloy wheel rims on black rubber tires, glowing xenon headlights with forward road light beams, and red taillights.
  - **Trail Motorcycle**: Tubular racing orange frame, chrome exhaust pipe, leather seat, and forward headlight beam.
- **Character Realism**:
  - Tactical neon visors (cyan for the parkour runner, menacing red for stalker chasers) and survivalist backpacks.
- **Animated Water Surface**:
  - Multi-layer animated wave ripples with sparkling sunlight specular glint.

---

### Verification
- **16 JVM Test Suites**: 3,362 tests passing, 0 failing (`sh tests/run.sh`).
- **Target SDK**: Android API 34 (minSdkVersion: API 21, Android 5.0 through Android 16).
- **Architecture**: Universal (Pure Java + GLES2).
- **APK Size**: ~22.4 MB.
