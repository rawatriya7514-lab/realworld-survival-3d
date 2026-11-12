package com.devagent.realworldsurvival3d;

/**
 * CombatTouchLayout - Dedicated Touch Combat HUD and Control Geometry Engine (Phase 11).
 * Features:
 *   - Movement joystick on left, action buttons on right (or mirrored for left-handed)
 *   - Dedicated Wuxia buttons: Light Attack, Heavy Attack, Block/Parry, Dodge, Jump,
 *     Weapon Switch, Qi Special Skill, Lock-On Target, Interact
 *   - Non-overlapping hit-testing with Euclidean circle distance
 *   - Scalable sizing and opacity
 *   - Pure Java (100% JVM testable)
 */
public class CombatTouchLayout {

    public enum CombatButton {
        NONE,
        LIGHT_ATTACK,
        HEAVY_ATTACK,
        BLOCK_PARRY,
        DODGE,
        JUMP,
        WEAPON_SWITCH,
        QI_SKILL,
        LOCK_ON,
        INTERACT,
        SPRINT,
        MAP,
        PAUSE
    }

    public float screenWidth = 800f;
    public float screenHeight = 600f;
    public float buttonScale = 1.0f;
    public float buttonOpacity = 0.65f;
    public boolean leftHanded = false;

    // Virtual Joystick
    public float joystickBaseX = 144f;
    public float joystickBaseY = 450f;
    public float joystickRadius = 96f;

    // Button Geometry
    public float btnLightX, btnLightY, btnLightRadius;
    public float btnHeavyX, btnHeavyY, btnHeavyRadius;
    public float btnBlockX, btnBlockY, btnBlockRadius;
    public float btnDodgeX, btnDodgeY, btnDodgeRadius;
    public float btnJumpX, btnJumpY, btnJumpRadius;
    public float btnWeaponSwitchX, btnWeaponSwitchY, btnWeaponSwitchRadius;
    public float btnQiSkillX, btnQiSkillY, btnQiSkillRadius;
    public float btnLockOnX, btnLockOnY, btnLockOnRadius;
    public float btnInteractX, btnInteractY, btnInteractRadius;
    public float btnSprintX, btnSprintY, btnSprintRadius;
    public float btnMapX, btnMapY, btnMapRadius;
    public float btnPauseX, btnPauseY, btnPauseRadius;

    public CombatTouchLayout() {
        update(800f, 600f, 1.0f, 0.65f, false);
    }

    public void update(float width, float height, float scale, float opacity, boolean leftHanded) {
        this.screenWidth = Math.max(1f, width);
        this.screenHeight = Math.max(1f, height);
        this.buttonScale = Math.max(0.8f, Math.min(1.3f, scale));
        this.buttonOpacity = Math.max(0.4f, Math.min(1.0f, opacity));
        this.leftHanded = leftHanded;

        float minDim = Math.min(this.screenWidth, this.screenHeight);
        float baseRadius = minDim * 0.09f * buttonScale;

        joystickRadius = minDim * 0.16f * Math.min(1.15f, buttonScale);
        if (leftHanded) {
            joystickBaseX = this.screenWidth * 0.82f;
        } else {
            joystickBaseX = this.screenWidth * 0.18f;
        }
        joystickBaseY = this.screenHeight * 0.75f;

        // Radii
        btnLightRadius = baseRadius * 1.15f;
        btnHeavyRadius = baseRadius * 0.95f;
        btnBlockRadius = baseRadius * 0.90f;
        btnDodgeRadius = baseRadius * 0.90f;
        btnJumpRadius = baseRadius * 0.95f;
        btnWeaponSwitchRadius = baseRadius * 0.80f;
        btnQiSkillRadius = baseRadius * 1.05f;
        btnLockOnRadius = baseRadius * 0.75f;
        btnInteractRadius = baseRadius * 0.90f;
        btnSprintRadius = baseRadius * 0.75f;
        btnMapRadius = baseRadius * 0.60f;
        btnPauseRadius = baseRadius * 0.60f;

        if (leftHanded) {
            // Mirrored layout (Buttons on Left, Joystick on Right)
            btnLightX = btnLightRadius * 1.8f;
            btnLightY = this.screenHeight - btnLightRadius * 1.8f;

            btnHeavyX = btnLightX + btnLightRadius * 1.85f;
            btnHeavyY = btnLightY - btnLightRadius * 0.85f;

            btnBlockX = btnLightX;
            btnBlockY = btnLightY - btnLightRadius * 1.95f;

            btnDodgeX = btnLightX + btnLightRadius * 1.85f;
            btnDodgeY = btnLightY + btnLightRadius * 0.85f;

            btnJumpX = btnLightX + btnLightRadius * 3.4f;
            btnJumpY = btnLightY;

            btnWeaponSwitchX = btnLightX + btnLightRadius * 2.8f;
            btnWeaponSwitchY = btnLightY - btnLightRadius * 2.2f;

            btnQiSkillX = btnLightX + btnLightRadius * 1.4f;
            btnQiSkillY = btnLightY - btnLightRadius * 2.8f;

            btnLockOnX = btnLightX;
            btnLockOnY = btnLightY - btnLightRadius * 3.4f;

            btnSprintX = joystickBaseX - joystickRadius * 1.4f;
            btnSprintY = joystickBaseY;
        } else {
            // Standard layout (Buttons on Right, Joystick on Left)
            btnLightX = this.screenWidth - btnLightRadius * 1.8f;
            btnLightY = this.screenHeight - btnLightRadius * 1.8f;

            btnHeavyX = btnLightX - btnLightRadius * 1.85f;
            btnHeavyY = btnLightY - btnLightRadius * 0.85f;

            btnBlockX = btnLightX;
            btnBlockY = btnLightY - btnLightRadius * 1.95f;

            btnDodgeX = btnLightX - btnLightRadius * 1.85f;
            btnDodgeY = btnLightY + btnLightRadius * 0.85f;

            btnJumpX = btnLightX - btnLightRadius * 3.4f;
            btnJumpY = btnLightY;

            btnWeaponSwitchX = btnLightX - btnLightRadius * 2.8f;
            btnWeaponSwitchY = btnLightY - btnLightRadius * 2.2f;

            btnQiSkillX = btnLightX - btnLightRadius * 1.4f;
            btnQiSkillY = btnLightY - btnLightRadius * 2.8f;

            btnLockOnX = btnLightX;
            btnLockOnY = btnLightY - btnLightRadius * 3.4f;

            btnSprintX = joystickBaseX + joystickRadius * 1.4f;
            btnSprintY = joystickBaseY;
        }

        // Center / Top UI
        btnInteractX = this.screenWidth * 0.50f;
        btnInteractY = this.screenHeight * 0.70f;

        btnMapX = this.screenWidth - btnMapRadius * 1.8f;
        btnMapY = btnMapRadius * 1.8f;

        btnPauseX = btnPauseRadius * 1.8f;
        btnPauseY = btnPauseRadius * 1.8f;
    }

    /**
     * Hit tests screen coordinates against combat touch buttons.
     */
    public CombatButton hitTest(float px, float py) {
        if (isInside(px, py, btnLightX, btnLightY, btnLightRadius)) return CombatButton.LIGHT_ATTACK;
        if (isInside(px, py, btnHeavyX, btnHeavyY, btnHeavyRadius)) return CombatButton.HEAVY_ATTACK;
        if (isInside(px, py, btnBlockX, btnBlockY, btnBlockRadius)) return CombatButton.BLOCK_PARRY;
        if (isInside(px, py, btnDodgeX, btnDodgeY, btnDodgeRadius)) return CombatButton.DODGE;
        if (isInside(px, py, btnJumpX, btnJumpY, btnJumpRadius)) return CombatButton.JUMP;
        if (isInside(px, py, btnWeaponSwitchX, btnWeaponSwitchY, btnWeaponSwitchRadius)) return CombatButton.WEAPON_SWITCH;
        if (isInside(px, py, btnQiSkillX, btnQiSkillY, btnQiSkillRadius)) return CombatButton.QI_SKILL;
        if (isInside(px, py, btnLockOnX, btnLockOnY, btnLockOnRadius)) return CombatButton.LOCK_ON;
        if (isInside(px, py, btnInteractX, btnInteractY, btnInteractRadius)) return CombatButton.INTERACT;
        if (isInside(px, py, btnSprintX, btnSprintY, btnSprintRadius)) return CombatButton.SPRINT;
        if (isInside(px, py, btnMapX, btnMapY, btnMapRadius)) return CombatButton.MAP;
        if (isInside(px, py, btnPauseX, btnPauseY, btnPauseRadius)) return CombatButton.PAUSE;
        return CombatButton.NONE;
    }

    private static boolean isInside(float px, float py, float cx, float cy, float r) {
        float dx = px - cx;
        float dy = py - cy;
        return (dx * dx + dy * dy) <= (r * r);
    }
}
