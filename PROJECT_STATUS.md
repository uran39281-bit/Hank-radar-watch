# Project status — v0.9

Repository: uran39281-bit/Hank-radar-watch. Android package com.jb.radar; versionCode 9 / versionName 0.9.0; title AIR DEFAME 101. Latest APK: artifacts/Air-Defame-101-v0.9.apk.

Latest request: USA-only War Thunder-style tech tree. Replaced player starter equipment with ADS-201 Watchpost, MIM-301 Rampart and researchable FIM-352 Stonebolt using the supplied stats exactly (speed interpreted as km/h, thrust kN). Read Update-Guide-v0.9.txt and EQUIPMENT.md for the exact values and supporting balance choices.

Progression: Watchpost/Rampart free; Stonebolt 600 BP research / $1,500 purchase. Apply banked BP in partial amounts, buy, then equip. All three launchers use the selected missile next mission. No IR loadout is available for Watchpost. Previous enemy profiles, music, terminal-green UI, launcher illustration and missions remain.

TechTree.java contains catalog, prerequisites, status and serialization. Economy.java owns atomic research/purchase/equip transactions. EconomyStore schema 2 saves new fields with balances; old economy_* and hawk_best are preserved. Equipment.java exposes playerWeapon; Game.weapon is latched at mission start and each projectile carries its profile. Shared MissileMotion governs friendly and enemy movement. Do not reset balances or lifetime totals during future migrations.

Verified: seven pure Java suites and ten seeded full missions. Tests cover migration, exact stats, partial research, insufficient funds, duplicate prevention, save failures/retries, persistent equip state, and actual interceptions with both new missiles. Desktop stubs checked tree navigation/actions, preference persistence, menu audio lifecycle and visual layout. APK v2/v3 signature and existing signing certificate match verified; actual Android installation/playback/touch and human balance testing remain outstanding.

Source, guides, tests, previews and APK are backed up here. Build with original private development key for in-place updates; never publish the key or password. Active missions do not survive process termination, but earned currency and research do. Future work: creator-supplied battery/missile branches, ranks/nations as requested, additional missions and accessibility.
