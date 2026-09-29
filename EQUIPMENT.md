# USA equipment — v0.17

All numbers are fictional game balance. `assets/equipment.properties` controls sensor/missile profiles; `TechTree.java` controls ownership, prerequisites and prices. Invalid values or unknown keys reject the profile as a whole and the UI reports fallback to defaults.

## Watchpost

| ADS-201 Watchpost | Value |
| --- | ---: |
| Effective / absolute detection | 30 / 30 km |
| Effective / absolute tracking | 24 / 24 km |
| Effective / absolute radar locking | 16 / 16 km |
| Simultaneous tracks | 5 |
| Illumination channels (targets) | 2 |
| Midcourse support channels (missiles) | 2 |
| Semi-active supported missile cap | 6 |
| Observations to establish track | 2 |
| Minimum track / lock quality | 0.25 / 0.30 |
| Scan speed | 1.00 / 10.00 |
| Battery IRST | Not installed |

A complete sweep takes `11 - scanSpeed` seconds. Aircraft and incoming missiles share track slots; support channels are distinct from track slots, launcher readiness and ammunition. `radar.channels` is only the legacy fallback default for the two new pools.

Effective ranges express nominal quality and absolute ranges set hard outer boundaries. The stock Watchpost has no beyond-effective extension. Other configurations may extend an absolute limit; sensor quality declines toward the boundary. Terrain, clutter and radar damage still apply. Lock range cannot exceed track range and track range cannot exceed detection range. Configuration also enforces the corresponding absolute-limit order.

`radar.infrared` denotes an IRST sensor, not whether the launcher can use an IR missile. Radar cueing can point Ember's own seeker without an IRST. Standalone IRST search is not supplied by this battery/build.

## Player missile profiles

| Characteristic | Rampart | Stonebolt | Sentinel | Ember |
| --- | ---: | ---: | ---: | ---: |
| Full name | MIM-301 Rampart | FIM-352 Stonebolt | MIM-303 Sentinel | FIM-306 Ember |
| Guidance | Semi-active radar | Semi-active radar | Active radar | Infrared |
| Guidance duration | 35 s | 40 s | 40 s | 28 s |
| Maximum speed | 2,400 km/h | 2,700 km/h | 2,600 km/h | 2,592 km/h |
| Maximum G | 14 | 12 | 16 | 22 |
| Mass | 140 kg | 180 kg | 160 kg | 90 kg |
| Maximum AOA | 12° | 10° | 16° | 35° |
| Thrust | 18 kN | 22 kN | 21 kN | 14.4 kN |
| Motor burn | 5 s | 6 s | 6 s | 4 s |
| Maximum flight path | 24 km | 28 km | 26 km | 12 km |
| Minimum launch range | 0 km | 0 km | 0.8 km | 0.6 km |
| Ceiling | 13,700 m | 13,700 m | 13,700 m | 6,000 m |
| Seeker range | Support-dependent | Support-dependent | 8 km | 9 km |
| Research | Free | 600 BP | Free prototype | Free prototype |
| Purchase | Free | $1,500 | Free prototype | Free prototype |

Rampart and Stonebolt preserve the creator's specified characteristics. Additional flight parameters and the new Sentinel/Ember profiles are initial playtest choices. Their free access lets the player exercise all three guidance categories without altering saved currency. Stonebolt keeps its existing research/purchase progression.

The equipped missile supplies every launcher next mission. L1–L3 each hold three rounds, with nine shared reserve rounds. An empty healthy launcher reloads in 90 simulation seconds; damage changes reload performance. IR now uses this same inventory, rather than the earlier separate IR magazine. No guidance-family switching grants ammunition mid-mission.

Semi-active missiles share one illumination channel per locked target, subject to the separate six-missile support cap. Each active missile reserves its own midcourse channel even when multiple missiles share a target. Its reservation is freed on acquisition, termination, or explicit release. Basic IR consumes no illumination or midcourse channel after launch.

## Configuration and failure rules

New radar keys: `detectionAbsoluteKm`, `trackingAbsoluteKm`, `lockAbsoluteKm`, `illuminationChannels`, `midcourseChannels`, `supportedMissiles`, `acquireObservations`, `trackQualityMinimum`, `lockQualityMinimum` (all prefixed `radar.`).

New weapon keys: `supportRecoverySeconds` (default 4), `seekerSearchSeconds` (6), `seekerFovDeg` (60), `earlyActivation` (false), `retargeting` (false), prefixed `missile.<id>.`. Existing `seekerKm`, guidance time and physical flight limits remain separate. Optional retargeting is not enabled by this build; switching selected contacts or locks never silently redirects an in-flight missile. Overflow launch eviction is not implemented; insufficient support blocks FIRE.

MissileMotion applies thrust/mass acceleration, speed limits, finite turns, coast drag and turn energy loss. Support loss uses stored estimates rather than random steering. Seeker search has finite range, field of view and timeout. A successful seeker search, not merely activation, produces the acquired indication. Charge (`explosiveKg`) and blast radius remain separate from total mass.

## Save compatibility

Existing Dollars, BP, lifetime earnings, partial Stonebolt research, ownership, selected missile and best score are preserved. Normalization adds the new free prototype nodes without resetting earned funds. Research, purchase and equip remain separate atomic transactions; save failure leaves the previous state and offers retry. Purchase/equip remains unavailable during an active mission. Schema/key compatibility is unchanged.

Build/install with the original certificate to update in place. Uninstalling or clearing app data deletes local progress. Real-device playtesting is still required.
