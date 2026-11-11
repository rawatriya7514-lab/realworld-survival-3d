#!/bin/sh
# Compile and run the JVM game-logic tests (pure classes only - no Android).
set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
OUT="tests/build"
rm -rf "$OUT"
mkdir -p "$OUT"

# Compile the Android-free game logic classes plus the tests
S=android-build/src/com/devagent/realworldsurvival3d
javac -Xlint:-options -d "$OUT" \
    "$S/Obstacle.java" \
    "$S/Terrain.java" \
    "$S/OpenWorld.java" \
    "$S/WorldMap.java" \
    "$S/CharacterAnimator.java" \
    "$S/Player.java" \
    "$S/HunterEnemy.java" \
    "$S/Item.java" \
    "$S/CameraRig.java" \
    "$S/TouchLayout.java" \
    "$S/RoadNetwork.java" \
    "$S/NavigationTracker.java" \
    "$S/MapViewController.java" \
    "$S/DayNightCycle.java" \
    "$S/BuildingInterior.java" \
    "$S/CivilianNPC.java" \
    "$S/HostileAI.java" \
    "$S/Mission.java" \
    "$S/MissionManager.java" \
    "$S/Inventory.java" \
    "$S/Vehicle.java" \
    "$S/AudioMixer.java" \
    "$S/SettingsConfig.java" \
    "$S/SaveState.java" \
    "$S/SaveFileManager.java" \
    "$S/CullingOptimizer.java" \
    "$ROOT/tests/GameLogicTest.java" \
    "$ROOT/tests/OpenWorldTest.java" \
    "$ROOT/tests/MovementTest.java" \
    "$ROOT/tests/AnimationTest.java" \
    "$ROOT/tests/CameraTest.java" \
    "$ROOT/tests/TouchLayoutTest.java" \
    "$ROOT/tests/RoadNetworkTest.java" \
    "$ROOT/tests/MapTest.java" \
    "$ROOT/tests/EnvironmentTest.java" \
    "$ROOT/tests/AITest.java" \
    "$ROOT/tests/MissionTest.java" \
    "$ROOT/tests/InventoryVehicleTest.java" \
    "$ROOT/tests/AudioSettingsTest.java" \
    "$ROOT/tests/SaveLoadTest.java" \
    "$ROOT/tests/OptimizationTest.java" \
    "$ROOT/tests/FinalQATest.java" 2>&1 | grep -v "^Note:" || true

java -cp "$OUT" GameLogicTest
java -cp "$OUT" OpenWorldTest
java -cp "$OUT" MovementTest
java -cp "$OUT" AnimationTest
java -cp "$OUT" CameraTest
java -cp "$OUT" TouchLayoutTest
java -cp "$OUT" RoadNetworkTest
java -cp "$OUT" MapTest
java -cp "$OUT" EnvironmentTest
java -cp "$OUT" AITest
java -cp "$OUT" MissionTest
java -cp "$OUT" InventoryVehicleTest
java -cp "$OUT" AudioSettingsTest
java -cp "$OUT" SaveLoadTest
java -cp "$OUT" OptimizationTest
java -cp "$OUT" FinalQATest
