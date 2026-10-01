# AIR DEFENSE 0.19 — radar console update

Offline Android radar-defense game in terminal green. Native landscape Canvas UI, Android 8+, package `com.jb.radar`, versionCode 19. No network/account permissions. The existing development certificate permits in-place upgrades.

## Download

[Air-Defense-v0.19.apk](artifacts/Air-Defense-v0.19.apk)

Install over the previous version to retain Dollars, BP, research, ownership and best score. Uninstalling or clearing app data removes local progress. Active battles do not persist after process termination.

## This update

The supplied radar guide and icon pack drive a larger native console: central radar, left TWS list/missile stats/inventory, right sensor/contact/fire controls, four launchers and a visible battle log. Detection, tracking, hard lock, recognition and allegiance remain separate. AUTO TRACK, radar switching and radar-only RESET have explicit consequences for guidance.

Radar missiles share a per-missile DATALINKS pool. Overflow requires a warning/confirmation before the oldest missile loses support. Active seekers release support at activation, while SEARCHING and SEEKER LOCK remain distinct. Semi-active guidance can recover or retarget within its bounded support window. IR needs its own thermal acquisition and no channel after firing. Independent bearing-only IRST is implemented for fitted profiles; the existing Watchpost remains without IRST.

Missile maneuverability uses a configurable 1.00–10.00 rating. Configurable warhead/fuze/damage settings support partial blast damage and direct-impact kinetic profiles. Supplied icons preserve identification colors, with separate tracking/lock overlays and no heading leak on untracked plots.

As requested, the beta-only Horizon Shield/MIM-225A equipment and replacement test aircraft roster are omitted. The original campaign, USA progression, animated menus, Digital-7 introduction, soundtrack and Smart Level 1 Mission 1 remain. The original guide is retained as [UPDATE-REFERENCE-v019.txt](UPDATE-REFERENCE-v019.txt); implemented rules/defaults are in [Update-Guide-v0.19.txt](Update-Guide-v0.19.txt), [TRACKING.md](TRACKING.md) and [EQUIPMENT.md](EQUIPMENT.md).

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

See [PROJECT_STATUS.md](PROJECT_STATUS.md), [EQUIPMENT.md](EQUIPMENT.md), [TRACKING.md](TRACKING.md), [COMBAT-AI.md](COMBAT-AI.md) and [INTRO.md](INTRO.md). Balance settings live in `assets/equipment.properties` and `assets/combat.properties`; prices are in `TechTree.java`. All values are fictional game balance.

Build with Java 17, Android platform 35, build-tools 35.0.0 and zip. Set `RADAR_ANDROID_JAR`, `RADAR_BUILD_TOOLS`, `RADAR_KEYSTORE`, `RADAR_KEY_PASSWORD`, then run `./build.sh`. Optional `RADAR_ECJ` selects ECJ when javac is unavailable. Never publish private signing keys/passwords.

Run `./test.sh` for model regressions and seeded missions. Desktop Android stubs exercise actual Canvas drawing and touch/navigation. These checks do not replace installation, audio/performance and human balance testing on a real Android phone.
