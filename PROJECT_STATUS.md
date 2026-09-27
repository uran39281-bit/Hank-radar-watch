# Project status — v0.8

Repository: uran39281-bit/Hank-radar-watch. Android package com.jb.radar, versionCode 8, versionName 0.8.0, title AIR DEFAME 101. Latest APK: artifacts/Air-Defame-101-v0.8.apk.

Read README.md, EQUIPMENT.md, Mission-Guide.txt and Update-Guide-v0.8.txt before changing behavior. Preserve the terminal green palette, supplied launcher illustration, original menu audio, economy_* preferences and existing signing certificate. Never publish signing keys or passwords.

Completed: native landscape radar game; twelve-aircraft mission; uncertain identities and heading glyphs; terrain masking; separate TRACK and LOCK; selectable/interceptable incoming missiles; four-hit base and range damage; three radar launchers; separate IR seeker, ammo and flares; persistent Dollars and BP; receipts, retryable saving and interrupted-mission recovery.

Latest update: validated editable radar/missile profiles in assets/equipment.properties. Detection/tracking/lock range separation, finite track slots, per-target datalink channels, scan timing, IR support/lock range, guidance types/modes and finite guidance time. Equipment.java loads profiles, MissileMotion.java supplies shared arcade acceleration/turn/energy behavior, and the menu EQUIPMENT page shows battery and all seven weapon profiles. Only the existing loadout ships; active/command/passive primary profiles are supported for future equipment. No tech tree or purchases yet.

Verified: six pure Java model suites, ten seeded full missions, desktop rendering and menu/music/economy adapter checks. APK compiled; v2/v3 signature and previous signing identity verified; assets and manifest inspected. Real Android installation, audio playback, touch usability and human balance testing remain unverified. The current mission does not survive process termination; earned currency does.

Build: see build.sh and README; use original private key for in-place updates. Run test.sh with javac or RADAR_ECJ. Tests intentionally use fictional values; do not represent them as real operational missile performance. Future work: owner playtests, equipment selection/research, additional missions and accessibility.
