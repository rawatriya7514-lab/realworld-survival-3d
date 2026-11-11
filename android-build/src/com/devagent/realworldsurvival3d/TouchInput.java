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

    // Pinch Zoom
    public int zoomPointerId = -1;
    public float lastZoomX = 0f;
    public float lastZoomY = 0f;
    public float lastPinchDist = 0f;
    public float deltaZoom = 0f;

    // Action Buttons
    public boolean jumpPressed = false;
    public boolean vaultPressed = false;
    public boolean slidePressed = false;
    public boolean sprintToggled = false;
    public boolean camTogglePressed = false;
    public boolean interactPressed = false;
    public boolean pausePressed = false;

    /** Contextual visibility of the interact button (set by GameView). */
    public boolean interactVisible = false;

    /** Base look sensitivity, scaled by the player's settings (0.4 - 2.0). */
    public float sensitivityScale = 1.0f;
    public boolean invertLookY = false;

    // Button Bounding Boxes (Initialized in updateScreenSize)
    public float btnJumpX, btnJumpY, btnJumpRadius;
    public float btnVaultX, btnVaultY, btnVaultRadius;
    public float btnSlideX, btnSlideY, btnSlideRadius;
    public float btnSprintX, btnSprintY, btnSprintRadius;
    public float btnCamX, btnCamY, btnCamRadius;
    public float btnInteractX, btnInteractY, btnInteractRadius;
    public float btnPauseX, btnPauseY, btnPauseRadius;

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

        // Pause button sits just left of the camera toggle (top-right)
        btnPauseRadius = btnJumpRadius * 0.55f;
        btnPauseX = btnCamX - btnCamRadius * 2.4f;
        btnPauseY = btnCamY;

        // Interact button floats mid-right, above the action cluster
        btnInteractRadius = btnJumpRadius * 0.9f;
        btnInteractX = width - btnInteractRadius * 2.0f;
        btnInteractY = height * 0.46f;
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
                if (interactVisible && isInsideCircle(px, py, btnInteractX, btnInteractY, btnInteractRadius)) {
                    interactPressed = true;
                    return;
                }
                if (isInsideCircle(px, py, btnPauseX, btnPauseY, btnPauseRadius)) {
                    pausePressed = true;
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
                // Right side = Camera look & pinch zoom
                else if (px >= screenWidth * 0.45f) {
                    if (cameraPointerId == -1) {
                        cameraPointerId = pointerId;
                        lastCameraX = px;
                        lastCameraY = py;
                    } else if (zoomPointerId == -1 && pointerId != cameraPointerId) {
                        zoomPointerId = pointerId;
                        lastZoomX = px;
                        lastZoomY = py;
                        float pDx = px - lastCameraX;
                        float pDy = py - lastCameraY;
                        lastPinchDist = (float) Math.sqrt(pDx * pDx + pDy * pDy);
                    }
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
                        if (zoomPointerId == -1) {
                            float yawStep = (px - lastCameraX) * sensitivity * sensitivityScale;
                            float pitchStep = (py - lastCameraY) * sensitivity * sensitivityScale;
                            if (invertLookY) pitchStep = -pitchStep;
                            deltaYaw += yawStep;
                            deltaPitch += pitchStep;
                        }
                        lastCameraX = px;
                        lastCameraY = py;
                    } else if (pId == zoomPointerId) {
                        lastZoomX = px;
                        lastZoomY = py;
                    }
                }

                if (cameraPointerId != -1 && zoomPointerId != -1) {
                    float pDx = lastZoomX - lastCameraX;
                    float pDy = lastZoomY - lastCameraY;
                    float curPinchDist = (float) Math.sqrt(pDx * pDx + pDy * pDy);
                    if (lastPinchDist > 10f) {
                        float pinchDelta = (curPinchDist - lastPinchDist) / Math.max(1f, screenWidth);
                        deltaZoom += pinchDelta * 1.5f;
                    }
                    lastPinchDist = curPinchDist;
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
                } else if (pointerId == zoomPointerId) {
                    zoomPointerId = -1;
                } else if (pointerId == cameraPointerId) {
                    if (zoomPointerId != -1) {
                        cameraPointerId = zoomPointerId;
                        lastCameraX = lastZoomX;
                        lastCameraY = lastZoomY;
                        zoomPointerId = -1;
                    } else {
                        cameraPointerId = -1;
                    }
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
        interactPressed = false;
        pausePressed = false;
        deltaYaw = 0f;
        deltaPitch = 0f;
        deltaZoom = 0f;
    }
}
