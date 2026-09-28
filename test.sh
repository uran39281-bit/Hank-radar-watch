#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build/test
sources=(src/com/jb/radar/{Game,Infrared,Economy,Equipment,MissileMotion,TechTree,CombatRules,Battery,PilotAI}.java tests/*.java)
if [ -n "${RADAR_ECJ:-}" ]; then
 java -jar "$RADAR_ECJ" -8 -d build/test "${sources[@]}"
else
 javac --release 8 -d build/test "${sources[@]}"
fi
for test in GameTest InfraredTest InterceptionTest UpdateTest EconomyTest EquipmentTest TechTreeTest DamageTest AIBehaviorTest MissionSmoke; do
 java -cp build/test "com.jb.radar.$test"
done
