# Project checkpoint — 2026-09-27

## Current version
HAWK Radar Watch 0.2.0 (Android versionCode 2), package `com.jb.radar`.
This repository is the project backup and source of truth for continuing development.
The initial sketch has an altitude-vs-speed display at left and a circular search radar at right.
The detailed requirements are in `Mission-Guide.txt` supplied by the project owner.

## Files
- `src/com/jb/radar/Game.java`: standalone game model, aircraft, radar measurements, identification, terrain, missiles, channels and reloads.
- `src/com/jb/radar/MainActivity.java`: Android Activity, custom Canvas interface, controls and lifecycle.
- `tests/GameTest.java`: deterministic rule and simulation tests.
- `tests/MissionSmoke.java`: ten seeded whole-mission runs with inventory/channel invariants.
- `build.sh`: build without Gradle, using Android SDK tools and Java.
- `artifacts/`: signed APK and SHA-256 checksum.

## Completed
Native offline landscape game, five aircraft reference profiles, uncertain identification, terrain/clutter detection, coasting, up to two illuminated targets, three triple launchers, 9 ready + 9 reserve missiles, 90-second empty-launcher reloads, simplified missile motion and interception, scoring, mission ending, pause, optional sound and time acceleration.

## Verification and remaining work
Compilation, signing verification, model tests and ten seeded mission simulations passed. Desktop drawing-harness layout inspection passed. No actual phone/emulator install or touchscreen testing has been completed. Human gameplay balance remains untested. Missiles use arcade pursuit and a generous game collision tolerance; these are not validated real-world models. Resume by testing installation, touch sizing and gameplay on the user's phone, then implement requested changes.

## Signing and privacy
The repository is public. Do not commit signing keys, passwords, tokens or private source archives containing them. The original development key is preserved separately in the owner's saved source ZIP, not here. Preserve the application ID and use that same key for in-place updates. Increment versionCode for the next release.

## Suggested checks
Compile the standalone Game.java and tests with javac, then run `com.jb.radar.GameTest` and `com.jb.radar.MissionSmoke`. See README for Android build prerequisites.
