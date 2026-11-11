package com.devagent.realworldsurvival3d;

/**
 * Mission - Data and state model for a story quest in "The Long Signal" (Phase 9).
 *
 * Supports objective types:
 *  - REACH_LOCATION: Navigate to designated coordinates within targetRadius.
 *  - COLLECT_ITEMS: Gather required count of mission items (radio parts, etc.).
 *  - TALK_NPC: Speak with designated NPC.
 *  - REACH_HIGH_GROUND: Climb high elevation structure (watchtower / relay mast).
 *  - SURVIVE_WAVE: Hold position against infected waves for a duration or kill count.
 *  - TRANSMIT_SIGNAL: Activate final radio transmitter console at safehouse.
 */
public class Mission {

    public enum ObjectiveType {
        REACH_LOCATION,
        COLLECT_ITEMS,
        TALK_NPC,
        REACH_HIGH_GROUND,
        SURVIVE_WAVE,
        TRANSMIT_SIGNAL
    }

    public final int id;
    public final String title;
    public final String summary;
    public final String objectiveDesc;
    public final ObjectiveType type;

    public float targetX, targetY, targetZ;
    public float targetRadius;
    public String targetNpcName;

    public int targetCount;
    public int currentCount = 0;

    public float timer = 0f;
    public float maxTimer = 0f;

    public final int rewardScore;
    public final int rewardCurrency;

    public boolean isActive = false;
    public boolean isCompleted = false;
    public boolean isFailed = false;

    public String sideObjectiveDesc = null;
    public boolean sideCompleted = false;

    public Mission(int id, String title, String summary, String objectiveDesc,
                   ObjectiveType type, float targetX, float targetY, float targetZ,
                   float targetRadius, String targetNpcName, int targetCount,
                   float maxTimer, int rewardScore, int rewardCurrency,
                   String sideObjectiveDesc) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.objectiveDesc = objectiveDesc;
        this.type = type;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.targetRadius = targetRadius;
        this.targetNpcName = targetNpcName;
        this.targetCount = targetCount;
        this.maxTimer = maxTimer;
        this.timer = maxTimer;
        this.rewardScore = rewardScore;
        this.rewardCurrency = rewardCurrency;
        this.sideObjectiveDesc = sideObjectiveDesc;
    }

    public String getStatusString() {
        if (isCompleted) return "COMPLETED";
        if (isFailed) return "FAILED";
        switch (type) {
            case COLLECT_ITEMS:
                return currentCount + "/" + targetCount + " Collected";
            case SURVIVE_WAVE:
                if (maxTimer > 0f) return (int) Math.ceil(timer) + "s Remaining";
                return currentCount + "/" + targetCount + " Defeated";
            default:
                return objectiveDesc;
        }
    }

    public void reset() {
        currentCount = 0;
        timer = maxTimer;
        isCompleted = false;
        isFailed = false;
    }
}
