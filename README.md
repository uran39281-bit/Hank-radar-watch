# AIR DEFENSE 0.20 — aircraft, receiver and AI update

Offline Android radar-defense game in terminal green. Native landscape Canvas UI, Android 8+, versionCode 20. No network/account permissions.

## Download and installation

[Air-Defense-v0.20.apk](artifacts/Air-Defense-v0.20.apk)

**This build installs separately as `com.jb.radar.next`, labeled AIR DEFENSE 0.20.** The original `com.jb.radar` signing key was not recoverable after temporary workspace cleanup. The new APK therefore cannot update v0.19 in place. It leaves the old installation and its Dollars/BP/research untouched; progress starts fresh in the new app and does not transfer automatically. Do not uninstall the old app to install this one. A private backup of the new signing key is saved separately from this repository for future updates.

## This update

The two supplied guides now explicitly specify Su-27, MiG-29, MiG-25 and Tu-160. These four profiles replace the previous campaign roster. Their speed caps depend on altitude, load and damage; acceleration is finite, turns obey individual G limits and consume energy. WP-1 bomb strikes carry four/two/eight bombs respectively; MiG-25 uses the unarmed WP-0 intrusion route. Bomb release requires stable aim, heading, altitude, speed and a ballistic impact solution; released bombs continue after the aircraft is destroyed. WP-2/WP-3 and the supplied weapon catalogue are inactive references, with no default hostile missile spawns.

RF warnings now come from actual emitted signals tested against receiver bands, waveforms, beam/antenna coverage, terrain and sensitivity. A receiver may hear search before player detection. TWS and menu actions create no warning. Fire control, suspected illumination and active seeker emissions have separate delayed, imperfect recognition; IR remains RF-silent. Independent visual detection needs line of sight, a finite viewing sector and acquisition time, with pre-dawn/weather penalties. MAWS is absent from all default profiles.

Pilots react to anonymous perceived bearings and confidence rather than hidden missile positions, launch counts or player selections. They have reaction delays, threat memory, cautious/defensive/reassessment states, finite combined countermeasures, limited retries and exit routes. Rookie Mission 1 remains slower and imperfect. Countermeasures affect radar and heat sensors through geometry, age and configurable resistance; they never delete a missile automatically.

The Watchpost, player missiles, economy, USA tree, radar console, supplied icons, animated menus, Digital-7 introduction and music remain. The beta-only Horizon Shield equipment swap remains excluded. A recognized aircraft's INFO header opens a paused profile panel; mission task remains unconfirmed until observed, and the full roster is never shown before gameplay. Tu-160 figures and its bomb role are explicitly provisional game abstractions.

## Play

PLAY → STORY → BEFORE THE DAWN → LOADOUT → START MISSION. SURVIVAL and later missions are unavailable. Mission time and enemies start only after the introduction finishes or is skipped.

Select a detected contact and wait for a usable automatic track. PRIORITIZE can request a slot without displacing protected engagements. Rampart/Stonebolt require a completed radar LOCK; Sentinel can launch from a usable track without a hard lock. Ember uses its own passive seeker, cued by radar on the current battery. Selection alone does not create identification or a lock.

Watch age/quality, ammunition and guidance state. Untracked contacts show only their last radar position/range. Tracked contacts show measured estimates and bounded motion prediction; stale data is labeled and cannot provide a fresh launch solution. Recognizing a model does not establish allegiance.

L1–L4 hold three rounds each, with twelve shared reserve rounds. Empty healthy launchers reload in 90 simulation seconds; damage slows or disables them. Command destruction defeats the site. Pause and 1×/2×/4× time affect the full simulation consistently.

## Progression and compatibility

USA is the only nation. ADS-201 Watchpost/MIM-301 Rampart are starters. Stonebolt requires 600 BP research and $1,500 purchase. Existing free Sentinel/Ember prototype options remain; no extra beta equipment is introduced. Saved loadout and purchases are atomic, with retry on save failure.

Aircraft interceptions earn $250 / 100 BP, missile interceptions $150 / 75 BP, victory $500 / 250 BP, plus $100 / 50 BP for each 25% battery-condition tier on victory. Ammunition and repairs are free. Combat rewards remain after defeat or withdrawal.

Work is backed up on `radar-rebuild-v0.17`; `main` preserves v0.16. Earlier guides/APKs remain historical references, not the current rules.

## Development

See [Update-Guide-v0.20.txt](Update-Guide-v0.20.txt), [PROJECT_STATUS.md](PROJECT_STATUS.md), [EQUIPMENT.md](EQUIPMENT.md), [TRACKING.md](TRACKING.md), [COMBAT-AI.md](COMBAT-AI.md) and [INTRO.md](INTRO.md). Balance settings live in `assets/equipment.properties`, `assets/combat.properties`, `assets/aircraft.properties` and `assets/rwr.properties`; prices are in `TechTree.java`. All values are fictional game balance.

Build with Java 17, Android platform 35, build-tools 35.0.0 and zip. Set `RADAR_ANDROID_JAR`, `RADAR_BUILD_TOOLS`, `RADAR_KEYSTORE`, `RADAR_KEY_PASSWORD`, then run `./build.sh`. Optional `RADAR_ECJ` selects ECJ when javac is unavailable. Never publish private signing keys/passwords.

Run `./test.sh` for model regressions and seeded missions. Desktop Android stubs exercise actual Canvas drawing and touch/navigation. These checks do not replace installation, audio/performance and human balance testing on a real Android phone.
