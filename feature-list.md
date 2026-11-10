REALWORLD SURVIVAL 3D - Feature List
=====================================

NATIVE ANDROID GAME FEATURES (BUILT AND VERIFIED)
--------------------------------------------------
- Third-person 3D survival action adventure (OpenGL ES 1.0)
- Explorable environment with ground plane
- Player character with health (100) and stamina (100) systems
- Movement: walk, sprint, jump, gravity, fall handling
- Enemy AI: idle, patrol, chase, attack states
- Collectible items (resources)
- Mission system: reach checkpoint, collect items, survive timer, eliminate enemies
- Health bar and stamina bar UI overlays
- Pause / resume gameplay
- Game-over state
- Mission-complete state
- Save and load via SharedPreferences
- Settings: FPS limit (30/60), camera sensitivity, music/SFX volume
- Touch controls: virtual joystick + camera look zone
- Fullscreen, landscape orientation
- Android permissions: INTERNET, storage, vibrate, wake lock

GODOT 4.x GAME FEATURES (SOURCE COMPLETE, NOT RUNTIME-TESTED)
-------------------------------------------------------------
- Main menu: Play, Continue, Settings, Info, Exit
- Game-over screen: Retry, Main Menu
- Mission-complete screen: Continue
- Pause menu: Resume, Restart Mission, Settings, Main Menu
- Settings: Graphics quality, FPS limit, sensitivity, volume
- 5 mission types: Reach, Collect, Find, Survive, Eliminate
- Enemy AI: Idle, Patrol, Detect, Chase, Attack, Die
- Interaction system: Raycast-based proximity interaction
- Save/load: Persistent JSON save data
- Audio: Music and SFX with separate volume controls
- Mobile-optimized rendering: No SSAO, no volumetric fog, low GI rays
- Level-of-detail and distance-based visibility
- Touch joystick with dead-zone handling
- Multi-touch support
- Responsive UI for different screen sizes

ANDROID PERFORMANCE TARGETS
---------------------------
- 30 FPS on lower-end hardware
- 60 FPS on capable hardware
- Mobile-friendly rendering (forward renderer)
- Texture compression (ETC1/ASTC)
- Efficient collision (raycast-based)
- Limited enemy update frequency
- Memory-efficient (16bpc textures disabled)
