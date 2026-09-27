# Air Defame 101 0.4 — Mission 1

Android game based on the creator's radar sketch and supplied "HAWK Air Defense Radar Game — Mission 1" guide (included as Mission-Guide.txt). Offline, landscape, Android 8+. No network or account permissions. Package com.jb.radar; version code 4; signed with the same development key as 0.1 for an in-place update.

## Play
Defend the command site from twelve incoming aircraft during a mission of up to ten simulation minutes. Tap a blip or NEXT TARGET. Two observations establish a track. Read its estimated identity and telemetry, press TRACK, LOCK, and FIRE when within the measured engagement envelope. Maintain illumination until interception. RELEASE LOCK frees a channel but risks missiles already in flight. Protect the site; intercepts award 250 points, aircraft reaching the site cost 100; enemy missile impacts reduce radar range.

The radar starts at 40 km coverage, independent of the 25 km missile envelope. RANGE LIMIT cycles 10/20/30/40 km, with shorter presets improving measurements and identification. Each enemy missile impact removes 2–8 km cumulatively; damaged presets include the remaining maximum (for example 10/20/27). Zero remaining coverage loses the mission. The pale ring marks 25 km. Shaded patches are fictional hills. Unknown aircraft use heading-oriented diamonds; sufficiently confident estimated identities use aircraft silhouettes. Small arrows distinguish friendly and incoming missiles. Position and telemetry are radar observations, not perfect live data.

Select launcher L1, L2 or L3 along the bottom. Each starts with three missiles; nine more are in reserve. Only completely empty launchers reload. A reload takes 90 simulation seconds. Three reserve rounds are allocated when reloading starts and transferred when it finishes; the reserve display includes allocated rounds until completion. A full three-round reserve allocation is required. A maximum of two aircraft may be illuminated; multiple missiles may target one illuminated aircraft.

1x / 2x / 4x changes all simulation timing, including reloads and missile flight. Pause, guide, or backgrounding freezes the mission. Gameplay beeps are optional. The supplied menu theme loops only on the title screen, and pauses for gameplay, backgrounding and audio-focus loss. Best completed mission score persists. Current missions do not survive Android process termination.

## Implemented model
- Five profiles: Su-25, MiG-21bis, MiG-23M, Su-22M3 and Su-17M4, using supplied speed, dimension and ceiling references.
- Different cruise speeds, acceleration, altitude preferences, turn limits and defensive reactions; turn-related speed loss and variable-wing size approximations.
- Unknown contacts, noisy range/speed/altitude/size/heading measurements, weighted aircraft-profile estimates, track history and confidence that can rise or fall. Su-17 and Su-22 have deliberately similar profiles. True types are revealed only in the debrief.
- Simplified radar horizon, sampled line-of-sight terrain obstruction, altitude-dependent clutter, coasting and reacquisition.
- Two illumination channels, 25 km measured slant-range launch gate, and a 13,700 m launch ceiling.
- Accelerating missiles, altitude-adjusted approximate speed cap, later energy loss, finite steering, terrain collision and guidance loss after four seconds without illumination.
- Hits use swept proximity geometry, not a random hit roll. A 120 m game collision tolerance is used for accessibility; it is not a claim about real missile lethality.

This is a fictional game simulation, not a validated representation of HAWK or aircraft performance. Missile motion is simplified arcade pursuit. Terrain consists of two generated ridge features. Size is an identification proxy, not radar cross section. Reference values are taken from the supplied guide, not independently verified War Thunder data.

## Build
Requires Java 17 JDK, Android platform 35, build-tools 35.0.0 and zip. Set RADAR_ANDROID_JAR, RADAR_BUILD_TOOLS, RADAR_KEYSTORE and RADAR_KEY_PASSWORD; run ./build.sh. Optional RADAR_ECJ supplies an Eclipse ECJ compiler jar if javac is absent. Signing alias: radar. The signing key is deliberately excluded from this public repository. Use your own local keystore for a fresh installation; updating the existing APK requires the original private development key retained in the owner’s source backup.

## Validation
- Compiled and verified APK signing and manifest; same signing certificate as version 0.1.
- Deterministic tests: inventory, two channels, partial-launcher restriction, 90-second reload, launch gates, coasting, terrain, missile acceleration, illumination loss, geometric interception, probability normalization, mission ending, restart.
- Ten seeded full mission simulations checked moving-target hits, mission completion, channel limit and ammunition conservation.
- Briefing and gameplay layouts inspected using a desktop rendering harness executing the app's actual drawing code against Android API stubs. This checks layout, not Android runtime behavior.
- No actual Android device/emulator installation or touch test was available. Initial gameplay balance is unvalidated by a human player.

## Download
The compiled version is in [artifacts/Air-Defame-101-v0.4.apk](artifacts/Air-Defame-101-v0.4.apk).

## Continue development
Read [PROJECT_STATUS.md](PROJECT_STATUS.md), then the mission guide and source before making changes.

## Version 0.3 UI update
User-supplied launcher illustration displayed in L1/L2/L3 cards, with separate ammunition indicators and reload status. Terminal green (#00FF00) on black replaces the teal palette; selected states use white. The original image is bundled unchanged in assets/hawk_launcher.png and decoded once at a reduced resolution. Game rules are unchanged.

## Version 0.4
The next-update guide is preserved in Update-Guide-v0.4.txt. AIR DEFAME 101 opens to a title and PLAY button. The bottom bar has NEXT TARGET / TRACK / LOCK / FIRE / RANGE LIMIT. TRACK adds priority surveillance without warning the aircraft. LOCK uses one of two channels and may trigger evasion. Identification remains uncertain; tracked aircraft receive extra measurements.

Enemy aircraft can launch simplified air-to-ground missiles after approach: Su-25 Kh-25ML/Kh-29L, MiG-23M Kh-23M, Su-22M3 Kh-27PS, Su-17M4 Kh-58U. MiG-21 has no guided ground attack. Non-ARM weapons require their source aircraft to maintain guidance; ARM weapons require radar emission. Detection, incoming warnings, cumulative range damage and impact flashes are implemented. These are fictional balancing models, including engagement distances and damage, not operational weapon simulations.

Weapon/profile reference pages: https://wiki.warthunder.com/unit/su_17m4 , https://wiki.warthunder.com/unit/su_22m3 , https://wiki.warthunder.com/unit/mig_23m , https://wiki.warthunder.com/unit/su_25 . No claim of exact real-world performance is made.

The original user-supplied menu audio is bundled unchanged as assets/menu_theme.mp3. New deterministic tests cover track/lock separation, target cycling, damaged presets, launch delay, guidance loss and radar destruction. Desktop media stubs verify lifecycle decisions, not actual sound playback. The APK still needs a real Android device installation/playtest.
