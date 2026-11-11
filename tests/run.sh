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
    "$S/Player.java" \
    "$S/HunterEnemy.java" \
    "$S/Item.java" \
    "$ROOT/tests/GameLogicTest.java" \
    "$ROOT/tests/OpenWorldTest.java" \
    "$ROOT/tests/MovementTest.java" 2>&1 | grep -v "^Note:" || true

java -cp "$OUT" GameLogicTest
java -cp "$OUT" OpenWorldTest
java -cp "$OUT" MovementTest
