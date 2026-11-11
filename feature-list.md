REALWORLD SURVIVAL 3D - Feature List
====================================

Status labels:
  [VERIFIED]    exercised by the 2,432-check JVM test suite (tests/run.sh)
  [BUILT]       implemented and compiles/links into the APK, but runtime
                behavior on a physical device was NOT tested (no adb in
                this environment)
  [LIMITED]     implemented in a simplified form (see known-limitations.md)

CORE LOOP
---------
- Main menu with New Game / Continue / Settings / About / Exit [BUILT]
- 3 sequential missions with distinct objectives [BUILT]
  1. Rooftop Infiltration - reach the extraction helipad
  2. Scavenger's High-Rise - collect 6 items, then extract
  3. Apex Extraction - collect 3 data drives (interact pickups), activate
     the helipad console, extract
- Pause / resume (button + BACK key), auto-save on pause/quit [BUILT]
- Game-over screen with score + retry [BUILT]
- Mission-complete screen with score, best score, next mission [BUILT]
- Save/load via SharedPreferences (mission, score, currency, items,
  health, best score); Continue restores the last mission [BUILT]

PLAYER PHYSICS & PARKOUR
------------------------
- Walk / run / sprint toggle with inertia and stamina drain [VERIFIED]
- Ground jump + one mid-air double jump [VERIFIED]
- Wall-run on building faces & billboards (left/right), 75% gravity,
  2s duration, stamina cost, Dutch-angle camera roll [VERIFIED*]
  * gating verified in tests: cannot trigger while standing on a roof,
    requires airborne state and a wall beside the player
- Wall-kick jump out of wall-runs [VERIFIED]
- Ledge grab + mantle onto rooftops (sine interpolation) [BUILT]
- Vault over waist-high barriers [BUILT]
- Knee slide + slide-kick cancel [BUILT]
- High-fall safety roll: slide input at landing negates fall damage [VERIFIED]
- Fall damage beyond 5m drop, scaled by distance; hard-land stun [VERIFIED]
- Health with slow regen after 6s without damage [BUILT]
- Stamina pool: sprint -12/s, wall-run -14/s, vault -8, mantle -8,
  slide -10; regen +18/s [VERIFIED]
- Adrenaline combo system: chained parkour actions multiply score;
  full meter = rush (1.25x speed, no sprint drain) [VERIFIED]
- Hazard pools (street level): 35 dmg per second of contact, lethal [VERIFIED]
- 3rd / 1st person camera toggle, dynamic FOV, head-bob, camera shake [BUILT]

WORLD
-----
- 10-skyscraper city: Alpha/Beta/Gamma/Apex/Delta/Epsilon/Spire/Relay/Crane
  platform/West outpost, plus 15 street-level filler buildings [VERIFIED]
- Wall-run billboards, balance beams, fan jump pads, hazard street,
  extraction helipad, rooftop props (vents, AC units, water tanks,
  antennas, signal console) [VERIFIED]
- Solid AABB collision: roof landing, wall push-out, floor queries
  (getFloorY / buildingAt) [VERIFIED]
- Extraction compass arrow points to the helipad [BUILT]

ENEMIES
-------
- Hunter chasers: patrol (roof-bounded), detect (24m), chase (same roof),
  edge-follow + ballistic leap across gaps <= 9m, give-up after 6s,
  adopt new roof, return to patrol [VERIFIED]
- Never flies, never teleports; falls into the hazard when missing a leap [VERIFIED]
- Contact rules: melee attack (18 dmg, 1.2s cooldown), defeated by
  drop-stomp (bounce + 150 score) or slide tackle (35 dmg / stun) [VERIFIED]
- 3 chasers in mission 1, 5 in missions 2-3 [BUILT]
- Alert roar sfx on detection [BUILT]

ITEMS & INTERACTION
-------------------
- Energy drink (full stamina), medkit (+45 HP), parkour tokens (score),
  data drives (mission-3 keys, interact pickup) [VERIFIED]
- Proximity auto-collect (1.8m) for regular items [VERIFIED]
- Interaction system with contextual INTERACT button + prompts for data
  drives and the extraction console [BUILT]

RENDERING (GLES2)
-----------------
- Custom GLES2 shaders: Lambert directional sun, hemisphere ambient,
  Blinn-Phong specular, exponential fog, sky gradient [BUILT]
- Textured surfaces: 8 procedurally generated POT textures [BUILT]
- VBO geometry for cubes/quads/fans/tris; frustum + behind-camera culling [BUILT]
- Blob shadows under player/enemies [BUILT]
- Quality presets: High / Performance (toggle in Settings) [BUILT]
- 30 FPS frame-budget limiter option (60 = uncapped vsync) [BUILT]

AUDIO
-----
- SoundPool SFX engine, 18 loaded sounds, up to 12 simultaneous streams [BUILT]
- MediaPlayer music: 5 seamless-loop tracks (menu + one per mission) [BUILT]
- Separate Music and SFX volume settings (live-applied, persisted) [BUILT]
- Heavy-breathing loop at low stamina; heartbeat loop at low health [BUILT]

PLATFORM
--------
- Landscape, fullscreen immersive; GLES 2.0 context [BUILT]
- minSdk 24, targetSdk 34; VIBRATE is the only permission [BUILT]
- Haptic feedback on damage, extraction signal, game over [BUILT]
- BACK key: pause / resume / menu navigation [BUILT]

NOT IMPLEMENTED (by design, see known-limitations.md)
-----------------------------------------------------
- Text rendering inside the GL layer (HUD text is Android View overlays)
- 3D character/enemy models (blob + box composites, no skeletal animation)
- Multiplayer, leaderboard, achievements
- Release (Play-Store) signing
