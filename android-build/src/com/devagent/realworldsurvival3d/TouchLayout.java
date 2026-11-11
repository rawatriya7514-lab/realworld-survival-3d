package com.devagent.realworldsurvival3d;

/**
 * TouchLayout - Pure Java geometry, layout, and hit-testing engine for on-screen touch controls.
 *
 * Implements Phase 5 Touch Controls:
 *  - Adaptive screen sizing and aspect ratio support
 *  - Button size scaling (0.8x to 1.3x)
 *  - Button opacity (0.4 to 1.0)
 *  - Left-handed mode mirroring (joystick on right, buttons on left)
 *  - Dedicated CROUCH button alongside JUMP, VAULT, SLIDE, SPRINT, CAM, INTERACT, PAUSE
 *  - Non-overlapping hit-testing with Euclidean circle distance
 */
public class TouchLayout {
    public enum ButtonType {
        NONE,
        JUMP,
        CROUCH,
        VAULT,
        SLIDE,
        SPRINT,
        CAM,
        INTERACT,
        PAUSE,
        MAP
    }

    public float screenWidth = 800f;
    public float screenHeight = 600f;

    public float buttonScale = 1.0f;
    public float buttonOpacity = 0.65f;
    public boolean leftHanded = false;

    // Virtual Joystick layout
    public float joystickBaseX = 144f;
    public float joystickBaseY = 450f;
    public float joystickRadius = 96f;

    // Action Buttons layout
    public float btnJumpX, btnJumpY, btnJumpRadius;
    public float btnCrouchX, btnCrouchY, btnCrouchRadius;
    public float btnVaultX, btnVaultY, btnVaultRadius;
    public float btnSlideX, btnSlideY, btnSlideRadius;
    public float btnSprintX, btnSprintY, btnSprintRadius;
    public float btnCamX, btnCamY, btnCamRadius;
    public float btnInteractX, btnInteractY, btnInteractRadius;
    public float btnPauseX, btnPauseY, btnPauseRadius;
    public float btnMapX, btnMapY, btnMapRadius;

    public TouchLayout() {
        update(800f, 600f, 1.0f, 0.65f, false);
    }

    /**
     * Recomputes all control geometry for screen dimensions, scale, opacity, and handedness.
     */
    public void update(float width, float height, float scale, float opacity, boolean leftHanded) {
        this.screenWidth = Math.max(1f, width);
        this.screenHeight = Math.max(1f, height);
        this.buttonScale = Math.max(0.8f, Math.min(1.3f, scale));
        this.buttonOpacity = Math.max(0.4f, Math.min(1.0f, opacity));
        this.leftHanded = leftHanded;

        float minDim = Math.min(this.screenWidth, this.screenHeight);
        float baseRadius = minDim * 0.10f * buttonScale;

        // Joystick radius and position
        joystickRadius = minDim * 0.16f * Math.min(1.15f, buttonScale);
        if (leftHanded) {
            // Right side joystick for left-handed controls
            joystickBaseX = this.screenWidth * 0.82f;
        } else {
            // Standard left side joystick
            joystickBaseX = this.screenWidth * 0.18f;
        }
        joystickBaseY = this.screenHeight * 0.75f;

        // Base action button radius
        btnJumpRadius = baseRadius;
        btnVaultRadius = baseRadius * 0.85f;
        btnSlideRadius = baseRadius * 0.85f;
        btnSprintRadius = baseRadius * 0.75f;
        btnCrouchRadius = baseRadius * 0.75f;
        btnCamRadius = baseRadius * 0.65f;
        btnPauseRadius = baseRadius * 0.55f;
        btnMapRadius = baseRadius * 0.55f;
        btnInteractRadius = baseRadius * 0.90f;

        if (leftHanded) {
            // Mirrored layout: Action buttons on the LEFT, Joystick on the RIGHT
            btnJumpX = btnJumpRadius * 1.5f;
            btnJumpY = this.screenHeight - btnJumpRadius * 1.5f;

            btnVaultX = btnJumpX + btnJumpRadius * 1.95f;
            btnVaultY = this.screenHeight - btnVaultRadius * 1.4f;

            btnSlideX = btnJumpX;
            btnSlideY = btnJumpY - btnJumpRadius * 1.95f;

            btnSprintX = btnVaultX;
            btnSprintY = btnSlideY;

            // Dedicated Crouch button placed adjacent to Sprint
            btnCrouchX = btnSprintX + btnJumpRadius * 1.85f;
            btnCrouchY = btnSprintY;

            // Top-left camera toggle, pause and map
            btnCamX = btnCamRadius * 1.8f;
            btnCamY = btnCamRadius * 1.8f;

            btnPauseX = btnCamX + btnCamRadius * 2.4f;
            btnPauseY = btnCamY;

            btnMapX = btnPauseX + btnPauseRadius * 2.4f;
            btnMapY = btnCamY;

            // Interact floats mid-left above the cluster
            btnInteractX = btnInteractRadius * 2.0f;
            btnInteractY = this.screenHeight * 0.46f;
        } else {
            // Standard right-handed layout: Buttons on RIGHT, Joystick on LEFT
            btnJumpX = this.screenWidth - btnJumpRadius * 1.5f;
            btnJumpY = this.screenHeight - btnJumpRadius * 1.5f;

            btnVaultX = btnJumpX - btnJumpRadius * 1.95f;
            btnVaultY = this.screenHeight - btnVaultRadius * 1.4f;

            btnSlideX = btnJumpX;
            btnSlideY = btnJumpY - btnJumpRadius * 1.95f;

            btnSprintX = btnVaultX;
            btnSprintY = btnSlideY;

            // Dedicated Crouch button placed adjacent to Sprint
            btnCrouchX = btnSprintX - btnJumpRadius * 1.85f;
            btnCrouchY = btnSprintY;

            // Top-right camera toggle, pause and map
            btnCamX = this.screenWidth - btnCamRadius * 1.8f;
            btnCamY = btnCamRadius * 1.8f;

            btnPauseX = btnCamX - btnCamRadius * 2.4f;
            btnPauseY = btnCamY;

            btnMapX = btnPauseX - btnPauseRadius * 2.4f;
            btnMapY = btnCamY;

            // Interact floats mid-right above the cluster
            btnInteractX = this.screenWidth - btnInteractRadius * 2.0f;
            btnInteractY = this.screenHeight * 0.46f;
        }
    }

    /**
     * Hit tests a screen coordinate against all active buttons.
     */
    public ButtonType testButton(float px, float py, boolean interactVisible) {
        if (isInside(px, py, btnJumpX, btnJumpY, btnJumpRadius)) return ButtonType.JUMP;
        if (isInside(px, py, btnCrouchX, btnCrouchY, btnCrouchRadius)) return ButtonType.CROUCH;
        if (isInside(px, py, btnVaultX, btnVaultY, btnVaultRadius)) return ButtonType.VAULT;
        if (isInside(px, py, btnSlideX, btnSlideY, btnSlideRadius)) return ButtonType.SLIDE;
        if (isInside(px, py, btnSprintX, btnSprintY, btnSprintRadius)) return ButtonType.SPRINT;
        if (isInside(px, py, btnCamX, btnCamY, btnCamRadius)) return ButtonType.CAM;
        if (interactVisible && isInside(px, py, btnInteractX, btnInteractY, btnInteractRadius)) return ButtonType.INTERACT;
        if (isInside(px, py, btnPauseX, btnPauseY, btnPauseRadius)) return ButtonType.PAUSE;
        if (isInside(px, py, btnMapX, btnMapY, btnMapRadius)) return ButtonType.MAP;
        return ButtonType.NONE;
    }

    /**
     * True if the touch falls into the virtual joystick region of the screen.
     */
    public boolean isJoystickZone(float px, float py) {
        if (leftHanded) {
            return px >= screenWidth * 0.55f;
        } else {
            return px < screenWidth * 0.45f;
        }
    }

    /**
     * True if the touch falls into the camera look/swipe region of the screen.
     */
    public boolean isCameraZone(float px, float py) {
        if (leftHanded) {
            return px < screenWidth * 0.55f;
        } else {
            return px >= screenWidth * 0.45f;
        }
    }

    public static boolean isInside(float px, float py, float cx, float cy, float radius) {
        float dx = px - cx;
        float dy = py - cy;
        return (dx * dx + dy * dy) <= (radius * radius);
    }
}
