package com.devagent.realworldsurvival3d;

import java.util.List;

/**
 * Vehicle - Drivable land vehicle physics and interaction engine (Phase 10).
 *
 * Implements:
 *  - Vehicle types: SUV (Car) and Motorcycle
 *  - Realistic steering, throttle, braking, reverse, and handbrake drift physics
 *  - Obstacle collision, impact damage, and terrain height clamping
 *  - Hostile ramming / roadkill combat mechanics
 *  - Player enter/exit transitions and follow-camera positioning
 *  - Headlight illumination and fuel state
 *  - Pure Java (100% JVM-testable without Android runtime).
 */
public class Vehicle {

    public enum Type {
        SUV,
        MOTORCYCLE
    }

    public final Type type;
    public final String name;
    public float x, y, z;
    public float vx = 0f, vy = 0f, vz = 0f;
    public float speed = 0f;
    public float yaw = 0f;

    public float maxSpeed;
    public float reverseSpeed;
    public float acceleration;
    public float brakeForce;
    public float handbrakeForce;
    public float steerRate;

    public float health;
    public float maxHealth;
    public float fuel = 100f;
    public boolean isOccupied = false;
    public boolean isEngineOn = false;
    public boolean headlightsOn = true;

    public final float width;
    public final float length;
    public final float height;

    public Vehicle(Type type, String name, float x, float z, float initialYaw) {
        this.type = type;
        this.name = (name != null) ? name : (type == Type.SUV ? "Scout SUV" : "Trail Bike");
        this.x = x;
        this.z = z;
        this.yaw = initialYaw;
        this.y = Terrain.heightAt(x, z);

        if (type == Type.SUV) {
            this.maxSpeed = 22.0f;       // ~80 km/h
            this.reverseSpeed = -7.5f;
            this.acceleration = 11.5f;
            this.brakeForce = 18.0f;
            this.handbrakeForce = 26.0f;
            this.steerRate = 85.0f;
            this.health = 220.0f;
            this.maxHealth = 220.0f;
            this.width = 2.0f;
            this.length = 4.2f;
            this.height = 1.6f;
        } else {
            this.maxSpeed = 28.0f;       // ~100 km/h
            this.reverseSpeed = -6.0f;
            this.acceleration = 16.0f;
            this.brakeForce = 22.0f;
            this.handbrakeForce = 32.0f;
            this.steerRate = 120.0f;
            this.health = 110.0f;
            this.maxHealth = 110.0f;
            this.width = 0.9f;
            this.length = 2.2f;
            this.height = 1.2f;
        }
    }

    public void update(float dt, float throttle, float steer, boolean handbrake,
                       OpenWorld openWorld, Player player) {
        if (!isOccupied) {
            // Apply rolling friction when parked
            if (Math.abs(speed) > 0.05f) {
                speed -= Math.signum(speed) * 8.0f * dt;
                if (Math.abs(speed) < 0.05f) speed = 0f;
            }
            integratePosition(dt, openWorld);
            return;
        }

        // Throttle & Braking
        if (throttle > 0.05f) {
            if (speed < 0f) {
                // Forward throttle brakes reverse motion
                speed += brakeForce * throttle * dt;
            } else {
                speed = Math.min(maxSpeed, speed + acceleration * throttle * dt);
            }
        } else if (throttle < -0.05f) {
            float revInput = -throttle;
            if (speed > 0.5f) {
                // Reverse input brakes forward motion
                speed -= brakeForce * revInput * dt;
            } else {
                speed = Math.max(reverseSpeed, speed - acceleration * 0.6f * revInput * dt);
            }
        } else {
            // Passive rolling drag
            if (Math.abs(speed) > 0.1f) {
                speed -= Math.signum(speed) * 4.5f * dt;
                if (Math.abs(speed) < 0.1f) speed = 0f;
            }
        }

        // Handbrake
        if (handbrake) {
            if (Math.abs(speed) > 0.1f) {
                speed -= Math.signum(speed) * handbrakeForce * dt;
                if (Math.abs(speed) < 0.1f) speed = 0f;
            }
        }

        // Steering: Turning speed is proportional to forward velocity
        if (Math.abs(steer) > 0.05f && Math.abs(speed) > 0.2f) {
            float turnFactor = Math.min(1.0f, Math.abs(speed) / 4.0f);
            if (handbrake) turnFactor *= 1.4f; // Handbrake drift rotation
            float dir = (speed >= 0f) ? 1.0f : -1.0f;
            yaw += steer * steerRate * turnFactor * dir * dt;
            if (yaw > 360f) yaw -= 360f;
            if (yaw < 0f) yaw += 360f;
        }

        integratePosition(dt, openWorld);

        // Hostile collision / roadkill
        if (openWorld != null && openWorld.hostiles != null && Math.abs(speed) > 4.0f) {
            resolveHostileCollisions(openWorld.hostiles);
        }

        // Sync player position while driving
        if (player != null) {
            player.x = x;
            player.y = y;
            player.z = z;
            player.vx = vx;
            player.vy = vy;
            player.vz = vz;
        }
    }

    private void integratePosition(float dt, OpenWorld openWorld) {
        float rad = (float) Math.toRadians(yaw);
        vx = (float) Math.sin(rad) * speed;
        vz = -(float) Math.cos(rad) * speed;

        float nx = x + vx * dt;
        float nz = z + vz * dt;

        // Collision check against solid obstacles
        if (openWorld != null) {
            boolean collided = false;
            for (Obstacle o : openWorld.activeObstacles()) {
                if (!o.solid) continue;
                if (o.containsXZ(nx, nz, width * 0.45f)) {
                    collided = true;
                    // Impact damage if hitting at speed > 4 m/s
                    if (Math.abs(speed) > 4.0f) {
                        float dmg = Math.abs(speed) * 3.2f;
                        takeDamage(dmg);
                    }
                    speed = -speed * 0.25f; // Rebound bounce
                    break;
                }
            }
            if (!collided) {
                x = nx;
                z = nz;
            }
        } else {
            x = nx;
            z = nz;
        }

        // Ground height snap
        if (openWorld != null) {
            y = openWorld.floorAt(x, z, y);
        } else {
            y = Terrain.heightAt(x, z);
        }
    }

    private void resolveHostileCollisions(List<HostileAI> hostiles) {
        float hitRadius = (type == Type.SUV) ? 2.4f : 1.8f;
        for (HostileAI h : hostiles) {
            if (h.state == HostileAI.State.DEFEATED) continue;
            float dx = h.x - x;
            float dz = h.z - z;
            if (dx * dx + dz * dz <= hitRadius * hitRadius) {
                float impactDmg = Math.abs(speed) * 9.0f;
                h.takeDamage(impactDmg);
                // Push hostile back
                h.vx = vx * 0.8f;
                h.vz = vz * 0.8f;
            }
        }
    }

    public boolean enter(Player player) {
        if (isOccupied || player == null || health <= 0f) return false;
        isOccupied = true;
        isEngineOn = true;
        player.x = x;
        player.y = y;
        player.z = z;
        return true;
    }

    public void exit(Player player, OpenWorld openWorld) {
        if (!isOccupied) return;
        isOccupied = false;
        isEngineOn = false;

        if (player != null) {
            // Place player to the left of the vehicle
            float rad = (float) Math.toRadians(yaw + 90.0f);
            player.x = x + (float) Math.sin(rad) * (width * 0.5f + 1.0f);
            player.z = z - (float) Math.cos(rad) * (width * 0.5f + 1.0f);
            if (openWorld != null) {
                player.y = openWorld.floorAt(player.x, player.z, y);
            } else {
                player.y = Terrain.heightAt(player.x, player.z);
            }
            player.vx = 0f;
            player.vz = 0f;
            player.isGrounded = true;
        }
    }

    public void takeDamage(float amount) {
        health = Math.max(0f, health - amount);
        if (health <= 0f) {
            isEngineOn = false;
            speed = 0f;
        }
    }

    public boolean isNear(float px, float pz, float maxDist) {
        float dx = px - x;
        float dz = pz - z;
        return (dx * dx + dz * dz <= maxDist * maxDist);
    }
}
