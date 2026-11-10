REALWORLD SURVIVAL 3D - Controls Guide
=======================================

NATIVE ANDROID GAME CONTROLS
-----------------------------
The native Android build uses OpenGL ES rendering with touch controls:

MOVEMENT (Left side of screen)
- Touch and drag on left side: Virtual joystick for movement
- Joystick position determines direction and speed
- Dead zone: Small movements ignored

CAMERA LOOK (Right side of screen)
- Touch and drag on right side: Rotate camera around player
- Camera follows player smoothly from behind and above

ACTION BUTTONS
- Jump button: Jump (consumes stamina)
- Sprint button: Sprint while moving (consumes stamina)
- Interact button: Interact with objects

UI BUTTONS
- Pause button: Open pause menu
- Settings: Graphics quality, FPS limit, sensitivity, volume

GODOT VERSION CONTROLS (when built with Godot)
----------------------------------------------
Left side:
- Virtual movement joystick (analog, dead-zone handling)
- Smooth analog movement in all directions

Right side:
- Camera movement area (touch and drag to look around)
- Jump button
- Sprint button
- Interaction button (E equivalent)

Additional controls:
- Pause button
- Settings button
- Inventory access
- Mission information

GESTURES
--------
- Swipe left/right on camera area: Rotate view horizontally
- Swipe up/down on camera area: Adjust vertical camera angle
- Tap interaction button near object: Interact

SAFETY
------
- Buttons avoid Android system navigation area
- Layout works on different screen aspect ratios
- Landscape orientation (optimized for gameplay)
