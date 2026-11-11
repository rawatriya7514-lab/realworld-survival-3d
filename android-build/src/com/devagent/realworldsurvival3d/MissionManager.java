package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.List;

/**
 * MissionManager - Quest progression and objectives engine for "The Long Signal" (Phase 9).
 *
 * Implements:
 *  - 6-mission story arc: Static, Salvage, The Engineer, High Ground, Nightfall, The Long Signal.
 *  - 5 hidden Data Slate side collectibles across landmarks.
 *  - Dynamic objective tracking (locations, collection, dialogue, survival timer, wave defense).
 *  - Auto-waypoint navigation sync with NavigationTracker.
 *  - Safe failure/retry persistence without losing character progress.
 *  - Pure Java (100% JVM-testable without Android runtime).
 */
public class MissionManager {

    private final List<Mission> missions = new ArrayList<>();
    private final List<Item> missionPickups = new ArrayList<>();
    private final List<Item> dataSlates = new ArrayList<>();

    private int currentMissionIndex = 0;
    public boolean storyCompleted = false;
    public String lastToast = null;
    public float toastTimer = 0f;
    public int dataSlatesCollected = 0;

    public MissionManager() {
        initMissions();
        initCollectibles();
    }

    private void initMissions() {
        missions.add(new Mission(
                1,
                "Mission 1: Static",
                "The city has fallen silent. Make your way to the Safehouse to the east.",
                "Reach the Safehouse (210, 60)",
                Mission.ObjectiveType.REACH_LOCATION,
                210f, 0f, 60f, 12.0f,
                null, 1, 0f, 250, 50,
                "Pick up the Data Slate atop Central Plaza"
        ));

        missions.add(new Mission(
                2,
                "Mission 2: Salvage",
                "Radio relay parts are scattered across ruins. Collect all 4 parts.",
                "Collect 4 Radio Parts across the districts",
                Mission.ObjectiveType.COLLECT_ITEMS,
                0f, 0f, 0f, 0f,
                null, 4, 0f, 350, 75,
                "Retrieve all parts without dying"
        ));

        missions.add(new Mission(
                3,
                "Mission 3: The Engineer",
                "Jack knows how to tune the transceiver kit. Return to the Safehouse and talk to him.",
                "Talk to Jack at the Safehouse",
                Mission.ObjectiveType.TALK_NPC,
                212f, 0f, 62f, 3.5f,
                "Jack", 1, 0f, 400, 100,
                "Safehouse supply cache inspected"
        ));

        missions.add(new Mission(
                4,
                "Mission 4: High Ground",
                "The valley blocks transmission. Reach the apex of the North Watchtower.",
                "Climb to the Watchtower apex (120, -360)",
                Mission.ObjectiveType.REACH_HIGH_GROUND,
                120f, 30f, -360f, 18.0f,
                null, 1, 0f, 500, 150,
                "Grab the Watchtower summit data slate"
        ));

        missions.add(new Mission(
                5,
                "Mission 5: Nightfall",
                "An infected swarm blocks the River Bridge. Hold the bridge deck and survive.",
                "Survive at River Bridge (-155, 240)",
                Mission.ObjectiveType.SURVIVE_WAVE,
                -155f, 1.4f, 240f, 22.0f,
                null, 3, 35.0f, 650, 200,
                "Defeat 3 bridge stalkers during the defense"
        ));

        missions.add(new Mission(
                6,
                "Mission 6: The Long Signal",
                "The repeater network is live! Return to the Safehouse roof console and broadcast.",
                "Activate Safehouse broadcast console (210, 60)",
                Mission.ObjectiveType.TRANSMIT_SIGNAL,
                210f, 6.3f, 60f, 8.0f,
                null, 1, 0f, 1000, 500,
                "Broadcast before midnight"
        ));

        if (!missions.isEmpty()) {
            missions.get(0).isActive = true;
        }
    }

    private void initCollectibles() {
        // Mission 2 radio parts
        missionPickups.add(new Item(Item.Type.RADIO_PART, 110f, Terrain.heightAt(110f, -50f) + 0.6f, -50f));
        missionPickups.add(new Item(Item.Type.RADIO_PART, -190f, Terrain.heightAt(-190f, 220f) + 0.6f, 220f));
        missionPickups.add(new Item(Item.Type.RADIO_PART, -310f, Terrain.heightAt(-310f, 70f) + 0.6f, 70f));
        missionPickups.add(new Item(Item.Type.RADIO_PART, 200f, Terrain.heightAt(200f, -260f) + 0.6f, -260f));

        // 5 Hidden Data Slates
        dataSlates.add(new Item(Item.Type.DATA_SLATE, 150f, 14.5f, 20f));     // Central Plaza rooftop
        dataSlates.add(new Item(Item.Type.DATA_SLATE, 210f, 1.5f, 58f));      // Safehouse interior
        dataSlates.add(new Item(Item.Type.DATA_SLATE, -150f, 1.0f, 300f));    // Old Docks pier end
        dataSlates.add(new Item(Item.Type.DATA_SLATE, 120f, 36.5f, -360f));   // Watchtower summit
        dataSlates.add(new Item(Item.Type.DATA_SLATE, -280f, Terrain.heightAt(-280f, 120f) + 0.6f, 120f)); // West Forest glade
    }

    public List<Mission> getMissions() {
        return missions;
    }

    public List<Item> getMissionPickups() {
        return missionPickups;
    }

    public List<Item> getDataSlates() {
        return dataSlates;
    }

    public int getCurrentMissionIndex() {
        return currentMissionIndex;
    }

    public Mission getCurrentMission() {
        if (currentMissionIndex >= 0 && currentMissionIndex < missions.size()) {
            return missions.get(currentMissionIndex);
        }
        return null;
    }

    public boolean isStoryCompleted() {
        return storyCompleted;
    }

    public void update(float dt, Player player, OpenWorld openWorld) {
        if (toastTimer > 0f) {
            toastTimer -= dt;
            if (toastTimer <= 0f) lastToast = null;
        }

        // Update items (bobbing & rotation)
        for (Item item : missionPickups) {
            item.update(dt);
            if (!item.collected && isNearItem(player, item)) {
                item.onCollected(player);
                onPickupCollected(item, player);
            }
        }
        for (Item item : dataSlates) {
            item.update(dt);
            if (!item.collected && isNearItem(player, item)) {
                item.onCollected(player);
                onPickupCollected(item, player);
            }
        }

        Mission m = getCurrentMission();
        if (m == null || m.isCompleted) return;

        switch (m.type) {
            case REACH_LOCATION:
                float dx = player.x - m.targetX;
                float dz = player.z - m.targetZ;
                if (dx * dx + dz * dz <= m.targetRadius * m.targetRadius) {
                    completeCurrentMission(player);
                }
                break;

            case COLLECT_ITEMS:
                if (m.currentCount >= m.targetCount) {
                    completeCurrentMission(player);
                }
                break;

            case TALK_NPC:
                // Handled in onPlayerTalk
                break;

            case REACH_HIGH_GROUND:
                float hdx = player.x - m.targetX;
                float hdz = player.z - m.targetZ;
                float distSq = hdx * hdx + hdz * hdz;
                if (distSq <= m.targetRadius * m.targetRadius && player.y >= m.targetY) {
                    completeCurrentMission(player);
                }
                break;

            case SURVIVE_WAVE:
                float bdx = player.x - m.targetX;
                float bdz = player.z - m.targetZ;
                boolean onBridge = (bdx * bdx + bdz * bdz <= m.targetRadius * m.targetRadius);
                if (onBridge) {
                    if (m.currentCount >= m.targetCount) {
                        completeCurrentMission(player);
                    } else if (m.timer > 0f) {
                        m.timer -= dt;
                        if (m.timer <= 0f) {
                            completeCurrentMission(player);
                        }
                    }
                }
                break;

            case TRANSMIT_SIGNAL:
                float fdx = player.x - m.targetX;
                float fdz = player.z - m.targetZ;
                if (fdx * fdx + fdz * fdz <= m.targetRadius * m.targetRadius && player.y >= 5.5f) {
                    completeCurrentMission(player);
                }
                break;
        }
    }

    private boolean isNearItem(Player p, Item item) {
        float dx = p.x - item.x;
        float dy = p.y - item.y;
        float dz = p.z - item.z;
        return (dx * dx + dz * dz < 2.0f * 2.0f) && Math.abs(dy) < 2.5f;
    }

    public void onPlayerTalk(CivilianNPC npc) {
        Mission m = getCurrentMission();
        if (m != null && m.type == Mission.ObjectiveType.TALK_NPC) {
            if (m.targetNpcName != null && m.targetNpcName.equalsIgnoreCase(npc.name)) {
                m.currentCount = 1;
                // Will be completed on next update frame or directly:
                // We don't have player ref here, mark for completion:
                m.isCompleted = true;
                advanceMission();
            }
        }
    }

    public boolean onPickupCollected(Item item, Player player) {
        if (item.type == Item.Type.RADIO_PART) {
            Mission m = getCurrentMission();
            if (m != null && m.type == Mission.ObjectiveType.COLLECT_ITEMS) {
                m.currentCount++;
                lastToast = "Radio Part Collected (" + m.currentCount + "/" + m.targetCount + ")";
                toastTimer = 2.5f;
                if (m.currentCount >= m.targetCount) {
                    completeCurrentMission(player);
                }
                return true;
            }
        } else if (item.type == Item.Type.DATA_SLATE) {
            dataSlatesCollected++;
            lastToast = "Data Slate Found (" + dataSlatesCollected + "/5)";
            toastTimer = 2.5f;
            return true;
        }
        return false;
    }

    public void onHostileDefeated(HostileAI hostile) {
        Mission m = getCurrentMission();
        if (m != null && m.type == Mission.ObjectiveType.SURVIVE_WAVE) {
            m.currentCount++;
        }
    }

    public void onPlayerDeath() {
        Mission m = getCurrentMission();
        if (m != null) {
            m.reset();
            lastToast = "Mission Failed - Retrying from Safehouse";
            toastTimer = 3.0f;
        }
    }

    public void completeCurrentMission(Player player) {
        Mission m = getCurrentMission();
        if (m == null || m.isCompleted) return;

        m.isCompleted = true;
        m.isActive = false;
        if (player != null) {
            player.score += m.rewardScore;
            player.currency += m.rewardCurrency;
        }

        lastToast = "Mission Complete: " + m.title + " (+" + m.rewardScore + " pts)";
        toastTimer = 4.0f;

        advanceMission();
    }

    private void advanceMission() {
        currentMissionIndex++;
        if (currentMissionIndex < missions.size()) {
            missions.get(currentMissionIndex).isActive = true;
        } else {
            storyCompleted = true;
            lastToast = "THE LONG SIGNAL TRANSMITTED — City Restored! (+1000 pts)";
            toastTimer = 6.0f;
        }
    }

    /** Automatically feeds objective coordinates to the HUD/minimap nav tracker. */
    public void syncNavWaypoint(NavigationTracker navTracker) {
        if (navTracker == null) return;
        // Don't override if player has manually selected custom waypoint
        if (navTracker.hasActiveWaypoint) return;

        Mission m = getCurrentMission();
        if (m == null || m.isCompleted) return;

        if (m.type == Mission.ObjectiveType.REACH_LOCATION
                || m.type == Mission.ObjectiveType.TALK_NPC
                || m.type == Mission.ObjectiveType.REACH_HIGH_GROUND
                || m.type == Mission.ObjectiveType.SURVIVE_WAVE
                || m.type == Mission.ObjectiveType.TRANSMIT_SIGNAL) {
            navTracker.setDestination(m.title, m.targetX, m.targetZ);
        }
    }

    public String getHudTitle() {
        if (storyCompleted) return "The Long Signal: Transmitted";
        Mission m = getCurrentMission();
        return (m != null) ? m.title : "Free Roam";
    }

    public String getHudObjective() {
        if (storyCompleted) return "Network live. City communications restored.";
        Mission m = getCurrentMission();
        return (m != null) ? m.getStatusString() : "Explore the world.";
    }
}
