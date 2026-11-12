package com.devagent.realworldsurvival3d;

import android.view.MotionEvent;
import java.util.List;

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
    public boolean mapPressed = false;
    public boolean mapClosed = false;
    public boolean interactPressed = false;
    public boolean pausePressed = false;

    // Wuxia Combat Touch Controls (Phase 11)
    public final CombatTouchLayout combatLayout = new CombatTouchLayout();
    public boolean wuxiaControls = false;
    public boolean lightAttackPressed = false;
    public boolean heavyAttackPressed = false;
    public boolean blockPressed = false;
    public boolean dodgePressed = false;
    public boolean weaponSwitchPressed = false;
    public boolean qiSkillPressed = false;
    public boolean lockOnPressed = false;
    private int blockPointerId = -1;

    // Fullscreen Map overlay state & bindings (Phase 6)
    public boolean mapOverlayVisible = false;
    public MapViewController mapController = null;
    public RoadNetwork roadNetwork = null;
    public NavigationTracker navigationTracker = null;
    public List<OpenWorld.Poi> pois = null;
    public float playerX = 0f, playerZ = 0f;

    private int mapDragPointerId = -1;
    private float lastMapDragX = 0f;
    private float lastMapDragY = 0f;
    private boolean mapHasDragged = false;

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
    public float btnMapX, btnMapY, btnMapRadius;
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

        this.btnMapX = layout.btnMapX;
        this.btnMapY = layout.btnMapY;
        this.btnMapRadius = layout.btnMapRadius;

        this.btnInteractX = layout.btnInteractX;
        this.btnInteractY = layout.btnInteractY;
        this.btnInteractRadius = layout.btnInteractRadius;

        combatLayout.update(width, height, buttonScale, buttonOpacity, leftHanded);
    }

    public void handleTouchEvent(MotionEvent event) {
        // If the fullscreen interactive map is open, route all touches to MapViewController
        if (mapOverlayVisible && mapController != null) {
            handleMapTouchEvent(event);
            return;
        }

        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                float px = event.getX(pointerIndex);
                float py = event.getY(pointerIndex);

                // Tapping inside HUD Minimap disc opens the fullscreen map
                if (mapController != null && mapController.isInsideMinimap(px, py)) {
                    mapPressed = true;
                    return;
                }

                // Check Action Buttons via TouchLayout or CombatTouchLayout
                if (wuxiaControls) {
                    CombatTouchLayout.CombatButton cBtn = combatLayout.hitTest(px, py);
                    switch (cBtn) {
                        case LIGHT_ATTACK: lightAttackPressed = true; return;
                        case HEAVY_ATTACK: heavyAttackPressed = true; return;
                        case BLOCK_PARRY:
                            blockPressed = true;
                            blockPointerId = pointerId;
                            return;
                        case DODGE: dodgePressed = true; return;
                        case JUMP: jumpPressed = true; return;
                        case WEAPON_SWITCH: weaponSwitchPressed = true; return;
                        case QI_SKILL: qiSkillPressed = true; return;
                        case LOCK_ON: lockOnPressed = true; return;
                        case INTERACT: interactPressed = true; return;
                        case SPRINT: sprintToggled = true; return;
                        case MAP: mapPressed = true; return;
                        case PAUSE: pausePressed = true; return;
                        case NONE: break;
                    }
                } else {
                    TouchLayout.ButtonType btn = layout.testButton(px, py, interactVisible);
                    switch (btn) {
                        case JUMP: jumpPressed = true; return;
                        case CROUCH: crouchToggled = true; return;
                        case VAULT: vaultPressed = true; return;
                        case SLIDE: slidePressed = true; return;
                        case SPRINT: sprintToggled = true; return;
                        case CAM: camTogglePressed = true; return;
                        case MAP: mapPressed = true; return;
                        case INTERACT: interactPressed = true; return;
                        case PAUSE: pausePressed = true; return;
                        case NONE: break;
                    }
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
                if (pointerId == blockPointerId) {
                    blockPointerId = -1;
                    blockPressed = false;
                }
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

    private void handleMapTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);

        // When map is open, halt any player locomotion
        moveX = 0f;
        moveY = 0f;
        isJoystickActive = false;
        joystickPointerId = -1;
        cameraPointerId = -1;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                float px = event.getX(pointerIndex);
                float py = event.getY(pointerIndex);

                MapViewController.ButtonAction btn = mapController.testButtonClick(px, py);
                switch (btn) {
                    case CLOSE:
                        mapClosed = true;
                        return;
                    case CENTER_PLAYER:
                        mapController.centerOnPlayer(playerX, playerZ);
                        return;
                    case CLEAR_ROUTE:
                        if (navigationTracker != null) {
                            navigationTracker.clearDestination();
                        }
                        return;
                    case ZOOM_IN:
                        mapController.zoomIn();
                        return;
                    case ZOOM_OUT:
                        mapController.zoomOut();
                        return;
                    case NONE:
                        break;
                }

                if (mapDragPointerId == -1) {
                    mapDragPointerId = pointerId;
                    lastMapDragX = px;
                    lastMapDragY = py;
                    mapHasDragged = false;
                } else if (zoomPointerId == -1 && pointerId != mapDragPointerId) {
                    zoomPointerId = pointerId;
                    lastZoomX = px;
                    lastZoomY = py;
                    float pDx = px - lastMapDragX;
                    float pDy = py - lastMapDragY;
                    lastPinchDist = (float) Math.sqrt(pDx * pDx + pDy * pDy);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mapDragPointerId != -1 && zoomPointerId != -1) {
                    int idx1 = event.findPointerIndex(mapDragPointerId);
                    int idx2 = event.findPointerIndex(zoomPointerId);
                    if (idx1 != -1 && idx2 != -1) {
                        float x1 = event.getX(idx1), y1 = event.getY(idx1);
                        float x2 = event.getX(idx2), y2 = event.getY(idx2);
                        float pDx = x2 - x1, pDy = y2 - y1;
                        float curDist = (float) Math.sqrt(pDx * pDx + pDy * pDy);
                        if (lastPinchDist > 10f && curDist > 10f) {
                            float factor = curDist / lastPinchDist;
                            float focusX = (x1 + x2) * 0.5f;
                            float focusY = (y1 + y2) * 0.5f;
                            mapController.zoomBy(factor, focusX, focusY);
                            mapHasDragged = true;
                        }
                        lastPinchDist = curDist;
                    }
                } else if (mapDragPointerId != -1) {
                    int idx = event.findPointerIndex(mapDragPointerId);
                    if (idx != -1) {
                        float px = event.getX(idx);
                        float py = event.getY(idx);
                        float dx = px - lastMapDragX;
                        float dy = py - lastMapDragY;
                        if (Math.abs(dx) > 3f || Math.abs(dy) > 3f) {
                            mapHasDragged = true;
                            mapController.pan(dx, dy);
                            lastMapDragX = px;
                            lastMapDragY = py;
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (pointerId == mapDragPointerId) {
                    if (!mapHasDragged && zoomPointerId == -1) {
                        float px = event.getX(pointerIndex);
                        float py = event.getY(pointerIndex);
                        if (mapController.testButtonClick(px, py) == MapViewController.ButtonAction.NONE) {
                            mapController.handleMapTap(px, py, pois, navigationTracker, playerX, playerZ);
                        }
                    }
                    if (zoomPointerId != -1) {
                        mapDragPointerId = zoomPointerId;
                        int zIdx = event.findPointerIndex(zoomPointerId);
                        if (zIdx != -1) {
                            lastMapDragX = event.getX(zIdx);
                            lastMapDragY = event.getY(zIdx);
                        }
                        zoomPointerId = -1;
                    } else {
                        mapDragPointerId = -1;
                    }
                    mapHasDragged = false;
                } else if (pointerId == zoomPointerId) {
                    zoomPointerId = -1;
                }
                break;
            }
        }
    }

    public void resetFrameButtons() {
        jumpPressed = false;
        crouchToggled = false;
        vaultPressed = false;
        slidePressed = false;
        sprintToggled = false;
        camTogglePressed = false;
        mapPressed = false;
        mapClosed = false;
        interactPressed = false;
        pausePressed = false;
        lightAttackPressed = false;
        heavyAttackPressed = false;
        dodgePressed = false;
        weaponSwitchPressed = false;
        qiSkillPressed = false;
        lockOnPressed = false;
        deltaYaw = 0f;
        deltaPitch = 0f;
        deltaZoom = 0f;
    }
}
