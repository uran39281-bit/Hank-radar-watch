> Current aircraft/RWR/AI rules are in [Update-Guide-v0.20.txt](Update-Guide-v0.20.txt). Sections below retain prior system details where not superseded.

# Battery damage and pilot behavior — v0.19

Implements the supplied `Update-Guide-v0.10.txt` and `AI-Decision-Flow.mmd`. These are simplified fictional game rules. Existing player equipment, research, Dollars/BP, terminal-green interface, launcher artwork and menu music remain.

## Component damage

Each of seven components starts with 100 HP: radar, L1, L2, L3, L4, power and command. For each component in the blast footprint:

`damage = explosiveKg * damage.multiplier * max(0, 1 - distance / radius) * protection`

Distances and radii are in game km. Direct hits have a distance factor of one; the boundary and points outside the radius receive zero damage. Lower protection factors reduce damage. Each component is damaged once per explosion. Average condition is only a display/reward statistic; it is not a second shared health pool.

`assets/combat.properties` configures the multiplier (default 0.55), each category's explosive kg and radius, component protection, release-area radius, and optional AI sharing/tracking cues. Invalid or unknown values are rejected; the UI reports a configuration fallback if bundled data cannot load. Defaults are:

| Bomb category | Explosive kg | Radius km |
| --- | ---: | ---: |
| Super-small | 24.5 | 0.08 |
| Small | 44.4 | 0.13 |
| Medium | 87.1 | 0.20 |
| Large | 201.8 | 0.32 |
| Super-large | 428.6 | 0.48 |
| Mega-large | 898.8 | 0.68 |
| Nuclear (fictional) | 1,554.5 | 0.90 |

The current twelve-aircraft mission uses the first five categories. Mega-large and nuclear are configuration-ready but are not assigned to current mission aircraft. The nuclear number is the supplied fictional balance value, not a real yield.

| Component | Protection | Effect |
| --- | ---: | --- |
| Radar | 0.65 | Health scales detection, track and lock range; lower signal quality; zero HP disables tracking/locking |
| L1 / L2 / L3 / L4 | 0.75 | Reload work slows with damage, up to nearly 3x healthy time; destroyed launcher cannot fire or reload |
| Power | 0.80 | A damaging hit interrupts power for 0.7–6 seconds; zero HP disables connected equipment |
| Command | 0.55 | Damage increases minimum observation spacing from 0.25 to 4.25 seconds; zero HP defeats the battery |

Lost launcher rounds are removed from inventory. A destroyed reloading launcher releases its reserve allocation; unused reserve is retained for surviving launchers. Damage is repaired free at the next mission. Opening BATTERY pauses the simulation and shows all components, reload effects, power interruptions and warnings.

Hostile missiles use `missile.<id>.explosiveKg` and `.blastRadiusKm` from `equipment.properties`. Total `.massKg` affects flight acceleration and the optional kinetic-impact model. Site and nearby terrain impacts use the same component explosion calculation as bombs. Current hostile warheads are fictional balance values: Kh-25 24.5kg/0.20km, Kh-29 87.1kg/0.32km, Kh-23 and Kh-27 44.4kg/0.20km, Kh-58 87.1kg/0.32km. Air-target warhead damage is separate from this battery formula. v0.19 uses explosive content, warhead type, distance falloff and target vulnerability for partial damage; kinetic profiles require direct impact. See EQUIPMENT.md for provisional coefficients.

Victory still awards $500/250 BP. The condition bonus awards $100/50 BP per 25% tier, rounded up (1–4 for a surviving battery). The old saved `remainingHits` field now holds this tier count for compatibility. No currency/research migration or reset is required.

## Bombing passes

Aircraft must enter the designated overhead circle, default 0.35km from the battery center, while in APPROACH and carrying bombs. Evasion, notch, abort, retreat and critical damage prohibit release. Su-25 carries two bombs; other current aircraft carry one. An overhead release drops the remaining payload and sets EXIT; an empty aircraft cannot begin another attack pass.

Released bombs take 2–10 simulation seconds to fall. Heading, speed, altitude and random scatter set a fixed endpoint at release; later aircraft movement cannot steer the bomb. Hits are not guaranteed. Observed releases get BOMB markers on the radar and impact rings. Bombs are not selectable radar-missile targets.

Missing the overhead opportunity aborts the pass. The pilot turns away, then circles toward a 5–9km waypoint and retries. After three abandoned passes it retreats. Critically damaged aircraft (30% HP or less) immediately retreat. Existing finite aircraft turn, acceleration, speed and altitude limits apply regardless of Smart Level. Radar missile near misses can damage an aircraft and trigger this retreat; only kills pay interception rewards. From v0.18, the graze branch waits until the missile has passed its closest approach, so a still-approaching direct hit is not prematurely converted into a weak graze. Rookie Mission 1 approaches stay at least 1,800 m AGL and do not perform low evasive dives.

## Awareness and decisions

BEFORE THE DAWN now uses only Smart Level 1 / Rookie pilots (configurable via `mission1.smartLevel`, default 1). Levels 1–5 remain supported for other scenarios and tests. Each pilot has a separate fictional warning-equipment fit. The fit is a game profile, not a claim about real aircraft. `PilotAI.WarningProfile` independently enables search radar, fire-control, active missile radar, optical missile warning and visual observation. No-warning profiles cannot react to hidden actions. `assign(..., profile)` allows scenario overrides; the default array assigns fits by aircraft type.

TWS tracking produces no warning, even if an old configuration still includes `ai.trackingCue=true`. Suitable search emissions can produce SEARCH and raise awareness to ALERTED after a delay, without forcing a maneuver. Compatible fire-control receivers can receive LOCK. Nearby transmitting active seekers can produce ACTIVE_SEEKER when range/LOS permits. Launches alone do not universally signal radar warnings; optical/visual fits require an observable launch or missile within their fictional range. Passive IR acquisition produces no warning.

Recognition remains probabilistic and delayed; at most six cues can be pending per pilot. Base delays by Smart Level are 5.0, 3.8, 2.5, 1.4 and 0.7 seconds, with ±15% variation. SEARCH uses a 1.2 delay multiplier; LAUNCH/ACTIVE_SEEKER use 0.8. SEARCH recognition runs from 20% to 94%, LOCK 28% to 97%, LAUNCH 35% to 99%, and ACTIVE_SEEKER 30% to 99% across the five levels. Awareness progresses from UNAWARE through ALERTED to DEFENSIVE when an actual defensive maneuver is chosen. A recognized lock does not guarantee an abort or panic.

IR seeker visibility can be adjusted through `setConditions(thermalContrast, visibility)`. The default factors are 1.0; reduced contrast smoothly reduces sensing and zero visibility blocks it. Watchpost has no IRST. Fitted IRST profiles now support independent switching, search, acquisition, capacity, angular observations, stale expiry and cue controls; IRST-only contacts never expose a precise hidden range. These controls remain unavailable on Watchpost.

Rookies tend to continue; higher levels increasingly consider evasive turns, a tangential notch attempt, abort or retreat. NOTCH decisions and notch effects require Smart Level 3 or above. A notch only intermittently breaks radar guidance after the aircraft actually turns roughly perpendicular to the radar bearing. Missiles retain motion and can reacquire; no maneuver guarantees survival. The supplied launch branch is implemented as a Smart-weighted decision, including missed/late warnings and occasional continuation, as required by the written guide.

## Bounded learning

Each aircraft has a small mission-local memory. Optional side sharing is enabled by default (`ai.sharedMemory=true`). Recognized launches record early (>10km) versus late fire and repeated targeting. Reactions record whether the aircraft survives twelve seconds after a recognized launch, or is destroyed first. This finite observation window is a simplified success signal, not knowledge of all missiles or the player's next input. Aborted/missed passes and resolved bomb damage also inform memory. Successful retreat is recorded on exit; destruction during retreat records failure.

Counters saturate at 32. Evidence gradually changes action weights, with reaction-outcome adjustment capped at 18%, scaled by Smart Level; early-fire/repeated-target/failed-pass caution is bounded. Rookie learning has zero influence; Elite has the full bounded influence. Sharing off confines updates to that aircraft. All memories reset on mission start and never read future actions. This is rule-based adaptation, not trained machine learning. The mission debrief exposes Smart levels and aggregate bombing/abort/retreat outcomes.

## Verification

DamageTest checks charge, radius, falloff, protection, independent component health, power outages, launcher degradation and separate missile mass. AIBehaviorTest checks hidden/delayed warnings, Smart scaling, overhead release, time of flight, fixed scatter, missed hits, critical/empty retreat, repeat passes, gradual bounded/optional sharing, outcome timing and reset. Existing economy, equipment, tech-tree, IR, radar and interception suites remain. Ten seeded complete missions check inventory conservation (including destroyed ammo), channels and completion. Desktop rendering checks the actual Canvas UI with stubs; an Android device/emulator and human balance playtest are still needed.
