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

    // Layout engine (Phase 5)
    public final TouchLayout layout = new TouchLayout();
    public float buttonScale = 1.0f;
    public float buttonOpacity = 0.65f;
    public boolean leftHanded = false;
    public float joystickSensitivity = 1.0f;

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
    public boolean crouchToggled = false;
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

    // Button Bounding Boxes (Synchronized from TouchLayout)
    public float btnJumpX, btnJumpY, btnJumpRadius;
    public float btnCrouchX, btnCrouchY, btnCrouchRadius;
    public float btnVaultX, btnVaultY, btnVaultRadius;
    public float btnSlideX, btnSlideY, btnSlideRadius;
    public float btnSprintX, btnSprintY, btnSprintRadius;
    public float btnCamX, btnCamY, btnCamRadius;
    public float btnInteractX, btnInteractY, btnInteractRadius;
    public float btnPauseX, btnPauseY, btnPauseRadius;

    public void updateScreenSize(float width, float height) {
        layout.update(width, height, buttonScale, buttonOpacity, leftHanded);

        this.screenWidth = layout.screenWidth;
        this.screenHeight = layout.screenHeight;

        this.joystickBaseX = layout.joystickBaseX;
        this.joystickBaseY = layout.joystickBaseY;
        this.joystickRadius = layout.joystickRadius;

        if (!isJoystickActive) {
            joystickCurrentX = joystickBaseX;
            joystickCurrentY = joystickBaseY;
        }

        this.btnJumpX = layout.btnJumpX;
        this.btnJumpY = layout.btnJumpY;
        this.btnJumpRadius = layout.btnJumpRadius;

        this.btnCrouchX = layout.btnCrouchX;
        this.btnCrouchY = layout.btnCrouchY;
        this.btnCrouchRadius = layout.btnCrouchRadius;

        this.btnVaultX = layout.btnVaultX;
        this.btnVaultY = layout.btnVaultY;
        this.btnVaultRadius = layout.btnVaultRadius;

        this.btnSlideX = layout.btnSlideX;
        this.btnSlideY = layout.btnSlideY;
        this.btnSlideRadius = layout.btnSlideRadius;

        this.btnSprintX = layout.btnSprintX;
        this.btnSprintY = layout.btnSprintY;
        this.btnSprintRadius = layout.btnSprintRadius;

        this.btnCamX = layout.btnCamX;
        this.btnCamY = layout.btnCamY;
        this.btnCamRadius = layout.btnCamRadius;

        this.btnPauseX = layout.btnPauseX;
        this.btnPauseY = layout.btnPauseY;
        this.btnPauseRadius = layout.btnPauseRadius;

        this.btnInteractX = layout.btnInteractX;
        this.btnInteractY = layout.btnInteractY;
        this.btnInteractRadius = layout.btnInteractRadius;
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

                // Check Action Buttons via TouchLayout
                TouchLayout.ButtonType btn = layout.testButton(px, py, interactVisible);
                switch (btn) {
                    case JUMP: jumpPressed = true; return;
                    case CROUCH: crouchToggled = true; return;
                    case VAULT: vaultPressed = true; return;
                    case SLIDE: slidePressed = true; return;
                    case SPRINT: sprintToggled = true; return;
                    case CAM: camTogglePressed = true; return;
                    case INTERACT: interactPressed = true; return;
                    case PAUSE: pausePressed = true; return;
                    case NONE: break;
                }

                // Joystick region (adaptive for handedness)
                if (layout.isJoystickZone(px, py) && joystickPointerId == -1) {
                    joystickPointerId = pointerId;
                    joystickBaseX = px;
                    joystickBaseY = py;
                    joystickCurrentX = px;
                    joystickCurrentY = py;
                    isJoystickActive = true;
                    updateJoystickValues();
                }
                // Camera look & pinch zoom region (adaptive for handedness)
                else if (layout.isCameraZone(px, py)) {
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
            moveX = Math.max(-1f, Math.min(1f, (dx / joystickRadius) * joystickSensitivity));
            moveY = Math.max(-1f, Math.min(1f, (dy / joystickRadius) * joystickSensitivity));
        }
    }

    public void resetFrameButtons() {
        jumpPressed = false;
        crouchToggled = false;
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
