REALWORLD SURVIVAL 3D - Controls Guide
======================================

NATIVE ANDROID GAME (the shipped build)
---------------------------------------
All input is handled by TouchInput (multi-touch: joystick, camera and buttons
can be used simultaneously). Button positions are relative to the screen, so
the layout scales to any aspect ratio.

MOVEMENT (left 45% of the screen)
- Touch anywhere on the left half: a virtual analog joystick appears under
  your thumb (dynamic base, radius ~16% of the smaller screen edge).
- Drag to move 360 degrees; magnitude controls speed (walk <-> run).
- Release to zero movement. Dead zone: 10px.

CAMERA (right 55% of the screen)
- Touch and drag on the right half: yaw (unlimited) and pitch (-80..+80).
- Sensitivity slider (Settings) scales the swipe 0.4x - 2.0x.
- "Invert Look Y" toggle flips vertical swiping.

ACTION BUTTONS (in-GL circles, bottom-right cluster)
- JUMP (large, bottom-right corner):
    * grounded: jump
    * airborne (once): double jump / air boost
    * while wall-running: wall kick (launches off the wall)
    * while sliding: slide-kick (cancels slide into a jump; defeats chasers)
- VAULT (left of JUMP, held context):
    * sprint toward a waist-high barrier and jump: hurdle-vault it
- SLIDE (above JUMP):
    * sprinting + moving: knee slide (low profile, defeats chasers)
    * in the air just before a hard landing (>5m drop): SAFETY ROLL -
      negates all fall damage
- SPRINT (left of SLIDE): toggles turbo sprint; drains stamina, auto-cancels
  at 0 stamina. With full adrenaline: 1.25x speed, infinite stamina.

INTERACT (mid-right; only visible when something is in range)
- Collect DATA DRIVES (mission 3 items are interact-pickups, not auto)
- Activate the extraction console on the Apex helipad
A prompt line above the HUD explains what INTERACT will do.

CAM (top-right, small): toggles 3rd-person <-> 1st-person camera.

PAUSE (top-right, left of CAM): opens the pause menu. The Android BACK
button also pauses; BACK in the pause menu resumes; BACK on game-over or
mission-complete screens is ignored (explicit choice required).

HUD (drawn in GL + Android text overlays)
- Health bar (red) and stamina bar (yellow), left edge
- Mission title + goal (top-left), score/item counters
- Context prompt line (center-bottom): interact prompts, toasts, and parkour
  action notifications ("WALL RUN! x3")
- Compass arrow near the top: points at the extraction helipad
- Damage flash overlay + vibration when hit; heavy-breathing loop at
  stamina < 20%; heartbeat loop at health < 30%

MENUS (Android View overlays over the GL surface)
- MAIN MENU: New Game / Continue (shown when a save exists) / Settings /
  About / Exit. Shows best score.
- SETTINGS: Music volume, SFX volume, Look sensitivity, Invert Look Y,
  Graphics quality (High / Performance), FPS limit (60 / 30). Changes apply
  live and persist.
- PAUSE: Resume / Restart Mission / Settings / Save & Main Menu / Exit.
- GAME OVER: score summary + Retry / Main Menu.
- MISSION COMPLETE: mission summary + Next Mission (or Play Again after
  mission 3) / Main Menu.

LEGACY DOC SECTIONS REMOVED
---------------------------
Previous versions of this file described a Godot build's controls. The Godot
skeleton is not part of the shipped game; the native Android layout above is
the only control scheme.
