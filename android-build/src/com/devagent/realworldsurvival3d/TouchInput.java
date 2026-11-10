package com.devagent.realworldsurvival3d;

import android.view.MotionEvent;

/**
 * TouchInput - Robust multi-touch controller handling:
 *   - Virtual analog joystick on bottom-left for 360-degree locomotion
 *   - Swipe camera look on right screen
 *   - On-screen touch buttons: JUMP, VAULT, SLIDE/ROLL, SPRINT, CAMERA TOGGLE
 */
public class TouchInput {
    // Screen Dimensions
    public float screenWidth = 800f;
    public float screenHeight = 600f;

    // Virtual Joystick
    public int joystickPointerId = -1;
    public float joystickBaseX = 160f;
    public float joystickBaseY = 460f;
    public float joystickCurrentX = 160f;
    public float joystickCurrentY = 460f;
    public float joystickRadius = 90f;
    public float moveX = 0f; // -1.0 to 1.0
    public float moveY = 0f; // -1.0 to 1.0
    public boolean isJoystickActive = false;

    // Camera Look
    public int cameraPointerId = -1;
    public float lastCameraX = 0f;
    public float lastCameraY = 0f;
    public float deltaYaw = 0f;
    public float deltaPitch = 0f;
    public float sensitivity = 0.22f;

    // Action Buttons
    public boolean jumpPressed = false;
    public boolean vaultPressed = false;
    public boolean slidePressed = false;
    public boolean sprintToggled = false;
    public boolean camTogglePressed = false;

    // Button Bounding Boxes (Initialized in updateScreenSize)
    public float btnJumpX, btnJumpY, btnJumpRadius;
    public float btnVaultX, btnVaultY, btnVaultRadius;
    public float btnSlideX, btnSlideY, btnSlideRadius;
    public float btnSprintX, btnSprintY, btnSprintRadius;
    public float btnCamX, btnCamY, btnCamRadius;

    public void updateScreenSize(float width, float height) {
        this.screenWidth = width;
        this.screenHeight = height;

        joystickBaseX = width * 0.18f;
        joystickBaseY = height * 0.75f;
        joystickCurrentX = joystickBaseX;
        joystickCurrentY = joystickBaseY;
        joystickRadius = Math.min(width, height) * 0.16f;

        // Button positions
        btnJumpRadius = Math.min(width, height) * 0.10f;
        btnJumpX = width - btnJumpRadius * 1.5f;
        btnJumpY = height - btnJumpRadius * 1.5f;

        btnVaultRadius = btnJumpRadius * 0.85f;
        btnVaultX = btnJumpX - btnJumpRadius * 1.7f;
        btnVaultY = height - btnVaultRadius * 1.3f;

        btnSlideRadius = btnJumpRadius * 0.85f;
        btnSlideX = btnJumpX;
        btnSlideY = btnJumpY - btnJumpRadius * 1.7f;

        btnSprintRadius = btnJumpRadius * 0.75f;
        btnSprintX = btnVaultX;
        btnSprintY = btnSlideY;

        btnCamRadius = btnJumpRadius * 0.65f;
        btnCamX = width - btnCamRadius * 1.8f;
        btnCamY = btnCamRadius * 1.8f;
    }

    public void handleTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                float px = event.getX(pointerIndex);
                float py = event.getY(pointerIndex);

                // Check Action Buttons first
                if (isInsideCircle(px, py, btnJumpX, btnJumpY, btnJumpRadius)) {
                    jumpPressed = true;
                    return;
                }
                if (isInsideCircle(px, py, btnVaultX, btnVaultY, btnVaultRadius)) {
                    vaultPressed = true;
                    return;
                }
                if (isInsideCircle(px, py, btnSlideX, btnSlideY, btnSlideRadius)) {
                    slidePressed = true;
                    return;
                }
                if (isInsideCircle(px, py, btnSprintX, btnSprintY, btnSprintRadius)) {
                    sprintToggled = true;
                    return;
                }
                if (isInsideCircle(px, py, btnCamX, btnCamY, btnCamRadius)) {
                    camTogglePressed = true;
                    return;
                }

                // Left side = Joystick
                if (px < screenWidth * 0.45f && joystickPointerId == -1) {
                    joystickPointerId = pointerId;
                    joystickBaseX = px;
                    joystickBaseY = py;
                    joystickCurrentX = px;
                    joystickCurrentY = py;
                    isJoystickActive = true;
                    updateJoystickValues();
                }
                // Right side = Camera look
                else if (px >= screenWidth * 0.45f && cameraPointerId == -1) {
                    cameraPointerId = pointerId;
                    lastCameraX = px;
                    lastCameraY = py;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    float px = event.getX(i);
                    float py = event.getY(i);

                    if (pId == joystickPointerId) {
                        joystickCurrentX = px;
                        joystickCurrentY = py;
                        updateJoystickValues();
                    } else if (pId == cameraPointerId) {
                        deltaYaw += (px - lastCameraX) * sensitivity;
                        deltaPitch += (py - lastCameraY) * sensitivity;
                        lastCameraX = px;
                        lastCameraY = py;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (pointerId == joystickPointerId) {
                    joystickPointerId = -1;
                    isJoystickActive = false;
                    moveX = 0f;
                    moveY = 0f;
                } else if (pointerId == cameraPointerId) {
                    cameraPointerId = -1;
                }
                break;
            }
        }
    }

    private void updateJoystickValues() {
        float dx = joystickCurrentX - joystickBaseX;
        float dy = joystickCurrentY - joystickBaseY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > joystickRadius) {
            float scale = joystickRadius / dist;
            dx *= scale;
            dy *= scale;
            joystickCurrentX = joystickBaseX + dx;
            joystickCurrentY = joystickBaseY + dy;
            dist = joystickRadius;
        }

        if (dist < 10f) {
            moveX = 0f;
            moveY = 0f;
        } else {
            moveX = dx / joystickRadius;
            moveY = dy / joystickRadius;
        }
    }

    private boolean isInsideCircle(float px, float py, float cx, float cy, float radius) {
        float dx = px - cx;
        float dy = py - cy;
        return (dx * dx + dy * dy) <= (radius * radius);
    }

    public void resetFrameButtons() {
        jumpPressed = false;
        vaultPressed = false;
        slidePressed = false;
        sprintToggled = false;
        camTogglePressed = false;
        deltaYaw = 0f;
        deltaPitch = 0f;
    }
}
