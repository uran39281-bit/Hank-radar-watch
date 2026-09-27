# Project status — v0.7

Repository: uran39281-bit/Hank-radar-watch. Android package com.jb.radar, versionCode 7, title AIR DEFAME 101.

Read README.md, Mission-Guide.txt and Update-Guide-v0.7.txt before changing behavior. The original sketch, terminal-green palette and launcher image are retained. Latest APK is artifacts/Air-Defame-101-v0.7.apk, signed with the existing private development certificate. Never publish the keystore or its password.

Completed: simple PLAY menu; original menu song with lifecycle control; bottom five controls; passive priority tracking separate from two-channel locking; uncertain identification and heading glyphs; enemy guided missiles, radar damage and dynamic range presets. Source, tests, guides, media and APK are backed up here.

Verified: compilation, APK signature, assets and manifest, deterministic model tests, ten seeded mission smoke runs, desktop drawing and music lifecycle logic. No Android device/emulator was available: real playback, installation, touch usability and balancing require owner testing.

Future work: additional missions, balance based on playtests, accessibility/settings. Current missions do not survive process termination. Aircraft loadout selection is a simplified subset of guide examples.

Latest additions: selectable/interceptable enemy missiles; contact ID colors and labels; right-side controls; left priority list; four-hit base limit; dim animated menu radar. tests/InterceptionTest.java covers the new mechanics.

Latest: separate IR-6 heat-seeking launcher with independent ammo/reload, warmup/acquisition, passive lock, post-launch self-guidance, heat/aspect/terrain gates and moving flares. See Infrared.java, tests/InfraredTest.java and Update-Guide-v0.7.txt. Radar MIM-23 behavior remains separate.

Latest: persistent Dollars and BP balances, combat/victory/base rewards, detailed debrief and economy page, duplicate-payout checks, save retry and interrupted-mission handling. Tests: EconomyTest.java. New classes: Economy.java / EconomyStore.java. Tech tree and spending are explicitly future work. Preserve economy_* preferences and lifetime totals in future migrations.
