package com.devagent.realworldsurvival3d;

/**
 * GameState - Persistent game state for save/load.
 */
public class GameState {
    public float playerHealth = 100f;
    public float playerStamina = 100f;
    public int score = 0;
    public int currency = 0;
    public int currentMission = 0;
    public int missionProgress = 0;
    public int enemiesEliminated = 0;
    public int itemsCollected = 0;
    public float playerX = 0, playerY = 2, playerZ = 0;
}
