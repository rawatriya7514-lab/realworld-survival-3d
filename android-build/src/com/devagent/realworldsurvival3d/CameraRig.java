package com.devagent.realworldsurvival3d;

import java.util.List;

/**
 * CameraRig - Production third-person and first-person camera controller.
 *
 * Implements:
 * 1. Target Tracking: Smooth exponential damping following player target.
 * 2. Crouch Eye-Level Adaptation: Dynamically transitions focus height between
 *    standing (1.45m) and crouching (0.65m), smoothly resolving low-headroom clipping.
 * 3. Orbit Controls: 360-degree yaw, pitch clamped strictly to [-80, +80] degrees.
 * 4. Dutch Angle Roll: Smooth camera tilt for wall-runs (-15 to +15 deg).
 * 5. Zoom System: Continuous zoom factor (0.8x - 1.6x) and presets (CLOSE 2.5m, NORMAL 4.8m, FAR 7.2f).
 * 6. Obstruction Raycast Pull-In: Slab-based AABB ray-box intersection against all solid
 *    obstacles with a 0.25m safety collision margin; pulls in immediately on obstruction
 *    and smoothly returns outward when clear.
 * 7. Terrain-Aware Min Height: Open-world terrain clearance clamp (never penetrates terrain
 *    or river surface).
 * 8. First-Person Mode: Seamless toggle with eye-level positioning and pitch-aligned orientation.
 * 9. Screen Shake: Trauma-based shake with exponential attenuation.
 * 10. Pure Java LookAt View Matrix: 4x4 column-major matrix computation for JVM testing.
 */
public class CameraRig {
    // Zoom limits and defaults
    public static final float BASE_DISTANCE = 4.8f;
    public static final float MIN_DISTANCE = 1.0f;
    public static final float MAX_DISTANCE = 8.0f;
    public static final float MIN_ZOOM = 0.8f;
    public static final float MAX_ZOOM = 1.6f;

    public enum Preset {
        CLOSE(2.5f),
        NORMAL(4.8f),
        FAR(7.2f);

        public final float distance;
        Preset(float d) { this.distance = d; }
    }

    // Camera world position & orientation
    public float camX = 0f;
    public float camY = 26f;
    public float camZ = 10f;
    public float camTX = 0f;
    public float camTY = 25f;
    public float camTZ = 0f;
    public float camUpX = 0f;
    public float camUpY = 1f;
    public float camUpZ = 0f;

    // View angles (degrees)
    public float yaw = 0f;
    public float pitch = 0f;
    public float roll = 0f;
    public float targetRoll = 0f;

    // Eye / focus height
    public float standingEyeHeight = 1.45f;
    public float crouchingEyeHeight = 0.65f;
    public float currentFocusY = 1.45f;

    // Distance & Zoom
    public float zoomMultiplier = 1.0f;
    public float targetDistance = BASE_DISTANCE;
    public float currentDistance = BASE_DISTANCE;
    public Preset currentPreset = Preset.NORMAL;

    // Obstruction & Collision
    public float collisionMargin = 0.25f;
    public float minGroundClearance = 0.80f;
    public boolean isObstructed = false;
    public float lastHitDistance = BASE_DISTANCE;

    // Follow and response speeds (1/s)
    public float followSpeed = 16.0f;
    public float focusHeightSpeed = 9.0f;
    public float returnSpeed = 6.0f;
    public float rollSpeed = 10.0f;
    public float shakeDecay = 12.0f;

    // First person
    public boolean firstPerson = false;

    // Screen shake
    public float screenShake = 0f;
    private long shakeSeed = 1234567L;

    // Internal state
    private boolean initialized = false;

    public CameraRig() {}

    /**
     * Resets camera position directly to the player focus position with zero lag.
     */
    public void snapToTarget(Player player) {
        if (player == null) return;
        yaw = player.yaw;
        pitch = Math.max(-80.0f, Math.min(80.0f, player.pitch));
        targetRoll = player.cameraRoll;
        roll = targetRoll;

        boolean crouched = player.isCrouching || player.state == Player.ParkourState.CROUCH;
        currentFocusY = player.y + (crouched ? crouchingEyeHeight : standingEyeHeight);

        camTX = player.x;
        camTY = currentFocusY;
        camTZ = player.z;

        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float fwdX = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        float fwdY = (float) -Math.sin(radPitch);
        float fwdZ = (float) (-Math.cos(radYaw) * Math.cos(radPitch));

        if (firstPerson) {
            camX = camTX + fwdX * 0.15f;
            camY = camTY;
            camZ = camTZ + fwdZ * 0.15f;
        } else {
            float vertOffset = crouched ? 0.45f : 0.85f;
            camX = camTX - fwdX * currentDistance;
            camY = camTY + vertOffset - fwdY * 1.2f;
            camZ = camTZ - fwdZ * currentDistance;
        }

        float rollRad = (float) Math.toRadians(roll);
        camUpX = (float) Math.sin(rollRad);
        camUpY = (float) Math.cos(rollRad);
        camUpZ = 0f;

        initialized = true;
    }

    /**
     * Core update loop called each frame.
     */
    public void update(float dt, Player player, List<Obstacle> obstacles, boolean terrainAware) {
        if (player == null) return;
        if (!initialized) {
            snapToTarget(player);
        }

        // Clamp delta time to avoid large physics steps
        if (dt <= 0f) dt = 0.016f;
        if (dt > 0.1f) dt = 0.1f;

        // Synchronize view angles from player
        yaw = player.yaw;
        pitch = Math.max(-80.0f, Math.min(80.0f, player.pitch));
        targetRoll = player.cameraRoll;
        firstPerson = player.firstPersonView;

        // Smooth roll transition
        float rollLerp = 1.0f - (float) Math.exp(-rollSpeed * dt);
        roll += (targetRoll - roll) * rollLerp;

        // Smooth focus height transition (standing vs crouching)
        boolean crouched = player.isCrouching || player.state == Player.ParkourState.CROUCH;
        float targetEyeY = player.y + (crouched ? crouchingEyeHeight : standingEyeHeight);
        float focusLerp = 1.0f - (float) Math.exp(-focusHeightSpeed * dt);
        currentFocusY += (targetEyeY - currentFocusY) * focusLerp;

        camTX = player.x;
        camTY = currentFocusY;
        camTZ = player.z;

        // Forward look vector
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float fwdX = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        float fwdY = (float) -Math.sin(radPitch);
        float fwdZ = (float) (-Math.cos(radYaw) * Math.cos(radPitch));

        // Screen shake evaluation
        float shakeX = 0f;
        float shakeY = 0f;
        if (player.state == Player.ParkourState.HARD_LAND) {
            shake(0.40f);
        }
        if (screenShake > 0.0001f) {
            shakeX = (nextRand() * 2f - 1f) * screenShake;
            shakeY = (nextRand() * 2f - 1f) * screenShake;
            screenShake *= (float) Math.exp(-shakeDecay * dt);
        } else {
            screenShake = 0f;
        }

        if (firstPerson) {
            camX = camTX + fwdX * 0.15f + shakeX;
            camY = camTY + shakeY;
            camZ = camTZ + fwdZ * 0.15f;

            camTX = camX + fwdX * 5.0f;
            camTY = camY + fwdY * 5.0f;
            camTZ = camZ + fwdZ * 5.0f;
        } else {
            // Third-Person Camera Logic
            targetDistance = BASE_DISTANCE * zoomMultiplier;

            float vertOffset = crouched ? 0.45f : 0.85f;
            float idealOffX = -fwdX * targetDistance;
            float idealOffY = vertOffset - fwdY * 1.2f;
            float idealOffZ = -fwdZ * targetDistance;

            float idealRayLen = (float) Math.sqrt(idealOffX * idealOffX + idealOffY * idealOffY + idealOffZ * idealOffZ);
            float rayDx = idealOffX / Math.max(1e-6f, idealRayLen);
            float rayDy = idealOffY / Math.max(1e-6f, idealRayLen);
            float rayDz = idealOffZ / Math.max(1e-6f, idealRayLen);

            // Obstruction raycast against solid obstacles
            float allowedDist = idealRayLen;
            boolean obstructedThisFrame = false;

            if (obstacles != null) {
                for (int i = 0; i < obstacles.size(); i++) {
                    Obstacle o = obstacles.get(i);
                    if (!o.solid) continue;

                    float hitT = rayBoxIntersect(camTX, camTY, camTZ,
                            rayDx, rayDy, rayDz,
                            o.getMinX() - collisionMargin, o.getMaxX() + collisionMargin,
                            o.getMinY() - collisionMargin, o.getMaxY() + collisionMargin,
                            o.getMinZ() - collisionMargin, o.getMaxZ() + collisionMargin,
                            allowedDist);

                    if (hitT > 0.08f && hitT < allowedDist) {
                        allowedDist = hitT;
                        obstructedThisFrame = true;
                    }
                }
            }

            // Pull-in response vs smooth recovery
            if (obstructedThisFrame) {
                float clampedDist = Math.max(MIN_DISTANCE, allowedDist);
                currentDistance = Math.min(currentDistance, clampedDist);
                isObstructed = true;
                lastHitDistance = allowedDist;
            } else {
                isObstructed = false;
                float returnFactor = 1.0f - (float) Math.exp(-returnSpeed * dt);
                currentDistance += (idealRayLen - currentDistance) * returnFactor;
            }

            // Ideal position after obstruction clamp
            float targetCamX = camTX + rayDx * currentDistance;
            float targetCamY = camTY + rayDy * currentDistance;
            float targetCamZ = camTZ + rayDz * currentDistance;

            // Terrain clamp for ideal target
            if (terrainAware) {
                float groundY = Terrain.heightAt(targetCamX, targetCamZ);
                float minAllowedY = groundY + minGroundClearance;
                if (groundY < Terrain.WATER_LEVEL) {
                    minAllowedY = Math.max(minAllowedY, Terrain.WATER_LEVEL + 0.45f);
                }
                if (targetCamY < minAllowedY) {
                    targetCamY = minAllowedY;
                }
            }

            // Position follow smoothing (immediate pull-in when obstructed)
            if (obstructedThisFrame) {
                camX = targetCamX;
                camY = targetCamY;
                camZ = targetCamZ;
            } else {
                float fLerp = 1.0f - (float) Math.exp(-followSpeed * dt);
                camX += (targetCamX - camX) * fLerp;
                camY += (targetCamY - camY) * fLerp;
                camZ += (targetCamZ - camZ) * fLerp;
            }

            // Terrain clamp after follow interpolation
            if (terrainAware) {
                float groundY = Terrain.heightAt(camX, camZ);
                float minAllowedY = groundY + minGroundClearance;
                if (groundY < Terrain.WATER_LEVEL) {
                    minAllowedY = Math.max(minAllowedY, Terrain.WATER_LEVEL + 0.45f);
                }
                if (camY < minAllowedY) {
                    camY = minAllowedY;
                }
            }

            // Add screen shake
            camX += shakeX;
            camY += shakeY;
        }

        // Up vector calculation with roll
        float rollRad = (float) Math.toRadians(roll);
        camUpX = (float) Math.sin(rollRad);
        camUpY = (float) Math.cos(rollRad);
        camUpZ = 0f;
    }

    /**
     * Ray vs Axis-Aligned Bounding Box (AABB) intersection using the slab method.
     * Returns entry distance t if hit within (0, maxDist], or -1 if miss.
     */
    public static float rayBoxIntersect(float ox, float oy, float oz,
                                       float dx, float dy, float dz,
                                       float minX, float maxX,
                                       float minY, float maxY,
                                       float minZ, float maxZ,
                                       float maxDist) {
        float tNear = -Float.MAX_VALUE;
        float tFar = Float.MAX_VALUE;

        // X slab
        if (Math.abs(dx) > 1e-7f) {
            float invD = 1.0f / dx;
            float t1 = (minX - ox) * invD;
            float t2 = (maxX - ox) * invD;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            if (t1 > tNear) tNear = t1;
            if (t2 < tFar) tFar = t2;
            if (tNear > tFar || tFar < 0f) return -1f;
        } else {
            if (ox < minX || ox > maxX) return -1f;
        }

        // Y slab
        if (Math.abs(dy) > 1e-7f) {
            float invD = 1.0f / dy;
            float t1 = (minY - oy) * invD;
            float t2 = (maxY - oy) * invD;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            if (t1 > tNear) tNear = t1;
            if (t2 < tFar) tFar = t2;
            if (tNear > tFar || tFar < 0f) return -1f;
        } else {
            if (oy < minY || oy > maxY) return -1f;
        }

        // Z slab
        if (Math.abs(dz) > 1e-7f) {
            float invD = 1.0f / dz;
            float t1 = (minZ - oz) * invD;
            float t2 = (maxZ - oz) * invD;
            if (t1 > t2) { float tmp = t1; t1 = t2; t2 = tmp; }
            if (t1 > tNear) tNear = t1;
            if (t2 < tFar) tFar = t2;
            if (tNear > tFar || tFar < 0f) return -1f;
        } else {
            if (oz < minZ || oz > maxZ) return -1f;
        }

        float entryT = (tNear < 0f) ? 0f : tNear;
        if (entryT > maxDist) return -1f;
        return entryT;
    }

    /**
     * Smoothly adjusts zoom multiplier clamped to [MIN_ZOOM, MAX_ZOOM].
     */
    public void adjustZoom(float delta) {
        zoomMultiplier = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomMultiplier + delta));
        targetDistance = BASE_DISTANCE * zoomMultiplier;
    }

    /**
     * Sets a discrete zoom distance preset.
     */
    public void setPreset(Preset preset) {
        if (preset == null) return;
        this.currentPreset = preset;
        this.targetDistance = preset.distance;
        this.zoomMultiplier = preset.distance / BASE_DISTANCE;
    }

    /**
     * Cycles to the next zoom preset (CLOSE -> NORMAL -> FAR -> CLOSE).
     */
    public void cyclePreset() {
        switch (currentPreset) {
            case CLOSE: setPreset(Preset.NORMAL); break;
            case NORMAL: setPreset(Preset.FAR); break;
            case FAR: default: setPreset(Preset.CLOSE); break;
        }
    }

    /**
     * Trigger a camera trauma screen shake.
     */
    public void shake(float intensity) {
        this.screenShake = Math.max(this.screenShake, intensity);
    }

    /**
     * Computes a 4x4 column-major view matrix (identical to Matrix.setLookAtM).
     * Enables JVM unit test matrix verification without Android dependencies.
     */
    public void computeViewMatrix(float[] m) {
        // Forward F = normalize(Target - Eye)
        float fx = camTX - camX;
        float fy = camTY - camY;
        float fz = camTZ - camZ;
        float flen = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (flen < 1e-6f) { fx = 0f; fy = 0f; fz = -1f; }
        else { fx /= flen; fy /= flen; fz /= flen; }

        // Up vector normalize
        float upx = camUpX, upy = camUpY, upz = camUpZ;
        float uplen = (float) Math.sqrt(upx * upx + upy * upy + upz * upz);
        if (uplen < 1e-6f) { upx = 0f; upy = 1f; upz = 0f; }
        else { upx /= uplen; upy /= uplen; upz /= uplen; }

        // Side vector S = normalize(F x Up)
        float sx = fy * upz - fz * upy;
        float sy = fz * upx - fx * upz;
        float sz = fx * upy - fy * upx;
        float slen = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (slen < 1e-6f) { sx = 1f; sy = 0f; sz = 0f; }
        else { sx /= slen; sy /= slen; sz /= slen; }

        // Orthogonal Up vector U = S x F
        float ux = sy * fz - sz * fy;
        float uy = sz * fx - sx * fz;
        float uz = sx * fy - sy * fx;

        // Column-major 4x4
        m[0]  = sx;
        m[1]  = ux;
        m[2]  = -fx;
        m[3]  = 0f;

        m[4]  = sy;
        m[5]  = uy;
        m[6]  = -fy;
        m[7]  = 0f;

        m[8]  = sz;
        m[9]  = uz;
        m[10] = -fz;
        m[11] = 0f;

        m[12] = -(sx * camX + sy * camY + sz * camZ);
        m[13] = -(ux * camX + uy * camY + uz * camZ);
        m[14] =  (fx * camX + fy * camY + fz * camZ);
        m[15] = 1f;
    }

    private float nextRand() {
        shakeSeed = (shakeSeed * 6364136223846793005L + 1442695040888963407L);
        return ((shakeSeed >>> 33) & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }
}
