#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
test_output="${RADAR_TEST_OUT:-.test-out}"
mkdir -p "$test_output"
sources=(src/com/jb/radar/{Game,Infrared,Economy,Equipment,MissileMotion,TechTree,CombatRules,Battery,PilotAI,IntroSequence,AircraftProfiles,RwrReceiver,GameSensors}.java tests/*.java)
if [ -n "${RADAR_ECJ:-}" ]; then
 java -jar "$RADAR_ECJ" -8 -d "$test_output" "${sources[@]}"
else
 javac --release 8 -d "$test_output" "${sources[@]}"
fi
for test_file in tests/*Test.java tests/MissionSmoke.java; do
 test_name="${test_file##*/}"
 java -cp "$test_output" "com.jb.radar.${test_name%.java}"
done
