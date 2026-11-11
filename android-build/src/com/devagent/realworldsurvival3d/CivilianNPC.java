package com.devagent.realworldsurvival3d;

import java.util.Random;

/**
 * CivilianNPC - Pure Java autonomous civilian AI and conversational entity.
 *
 * Implements Phase 8 AI requirements:
 *  - Idle, patrol, talk, and react state machine
 *  - Conversational interaction when player approaches within 2.5m
 *  - Staggered AI performance throttling (<40m every frame, 40-100m every 5th frame, >100m frozen)
 *  - Local patrol containment within home zone
 *  - Articulated character animation (walking gait and idle breathing)
 */
public class CivilianNPC {

    public enum State {
        IDLE,
        PATROL,
        TALKING,
        FLEEING
    }

    public final String name;
    public final String role;
    public float x, y, z;
    public float homeX, homeZ;
    public float patrolRadius;
    public float yaw = 0f;
    public State state = State.IDLE;

    public float moveSpeed = 2.2f;
    public float idleTimer = 2.0f;
    public float targetX, targetZ;
    public boolean isMoving = false;

    // Dialogue system
    private final String[] dialogues;
    private int dialogueIndex = 0;
    public String currentDialogue = "";
    public float talkCooldown = 0f;

    // Articulated animator (Phase 8 visual fidelity)
    public final CharacterAnimator animator = new CharacterAnimator();
    private final Random rng = new Random();

    // Staggered update bookkeeping
    public int updateCount = 0;
    public boolean wasThrottled = false;
    public boolean wasFrozen = false;

    public CivilianNPC(String name, String role, float x, float z, float patrolRadius, String[] dialogues) {
        this.name = name;
        this.role = role;
        this.x = x;
        this.z = z;
        this.homeX = x;
        this.homeZ = z;
        this.targetX = x;
        this.targetZ = z;
        this.patrolRadius = Math.max(1.0f, patrolRadius);
        this.dialogues = (dialogues != null && dialogues.length > 0)
                ? dialogues : new String[]{"Stay safe out there, runner."};
        this.currentDialogue = this.dialogues[0];
        this.y = Terrain.heightAt(x, z);
    }

    /**
     * Staggered update loop complying with performance budget (TECHNICAL_ARCHITECTURE §2.4):
     *  - < 40m: updated every frame
     *  - 40m - 100m: updated every 5th frame (with 5x dt)
     *  - > 100m: frozen (skip behavior & physics)
     */
    public void update(float dt, float playerX, float playerY, float playerZ, int frameNumber) {
        float dx = playerX - x;
        float dz = playerZ - z;
        float distSq = dx * dx + dz * dz;

        // Frozen zone (> 100m)
        if (distSq > 100f * 100f) {
            wasFrozen = true;
            wasThrottled = false;
            return;
        }
        wasFrozen = false;

        // Throttled zone (40m - 100m): run only on frameNumber % 5 == 0
        if (distSq > 40f * 40f) {
            wasThrottled = true;
            if (frameNumber % 5 != 0) return;
            dt *= 5.0f;
        } else {
            wasThrottled = false;
        }

        updateCount++;
        float dist = (float) Math.sqrt(distSq);

        if (talkCooldown > 0f) talkCooldown -= dt;

        // Interaction proximity: If player is within 2.5m, face player and enter TALKING
        if (dist <= 2.5f) {
            state = State.TALKING;
            isMoving = false;
            // Face player
            float targetYaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            this.yaw = targetYaw;
            animator.update(dt, Player.ParkourState.STAND, 0f, true);
            return;
        }

        // Normal autonomous state machine
        switch (state) {
            case TALKING:
            case IDLE:
                isMoving = false;
                idleTimer -= dt;
                if (idleTimer <= 0f) {
                    pickNewPatrolTarget();
                    state = State.PATROL;
                }
                animator.update(dt, Player.ParkourState.STAND, 0f, true);
                break;

            case PATROL:
                float tDx = targetX - x;
                float tDz = targetZ - z;
                float tDist = (float) Math.sqrt(tDx * tDx + tDz * tDz);

                if (tDist <= 0.4f) {
                    // Arrived at patrol point
                    state = State.IDLE;
                    idleTimer = 1.5f + rng.nextFloat() * 2.5f;
                    isMoving = false;
                    animator.update(dt, Player.ParkourState.STAND, 0f, true);
                } else {
                    isMoving = true;
                    float step = Math.min(tDist, moveSpeed * dt);
                    x += (tDx / tDist) * step;
                    z += (tDz / tDist) * step;
                    y = Terrain.heightAt(x, z);

                    this.yaw = (float) Math.toDegrees(Math.atan2(tDx, -tDz));
                    animator.update(dt, Player.ParkourState.WALK, moveSpeed, true);
                }
                break;

            case FLEEING:
                // Move away from danger point
                break;
        }
    }

    private void pickNewPatrolTarget() {
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        double r = rng.nextDouble() * patrolRadius;
        targetX = homeX + (float) (Math.cos(angle) * r);
        targetZ = homeZ + (float) (Math.sin(angle) * r);
    }

    /**
     * Interacting with the NPC cycles through dialogue and resets talk cooldown.
     */
    public String interact() {
        currentDialogue = dialogues[dialogueIndex];
        dialogueIndex = (dialogueIndex + 1) % dialogues.length;
        talkCooldown = 3.0f;
        state = State.TALKING;
        return currentDialogue;
    }

    public boolean isNear(float px, float pz, float maxDist) {
        float dx = px - x, dz = pz - z;
        return (dx * dx + dz * dz) <= (maxDist * maxDist);
    }
}
