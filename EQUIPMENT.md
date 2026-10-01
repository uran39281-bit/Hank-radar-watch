# Equipment and provisional balance — v0.19

These are fictional game values. The update keeps Watchpost, the existing USA tree and campaign aircraft. The supplied guide's beta-only Horizon Shield battery, MIM-225A missile and replacement aircraft roster are intentionally excluded. `UPDATE-REFERENCE-v019.txt` retains the source guide.

## Watchpost

| ADS-201 Watchpost | Value |
| --- | ---: |
| Effective / absolute detection | 30 / 36 km |
| Effective / absolute tracking | 24 / 30 km |
| Effective / absolute radar lock | 16 / 20 km |
| Simultaneous tracks | 5 |
| Shared external-guidance channels | 2 |
| Observations to establish a track | 2 |
| Minimum track / lock quality | 0.25 / 0.30 |
| Scan-speed rating | 1.00 / 6.00 |
| Full sweep | 10 seconds |
| Radar switching / lock acquisition / lock loss tolerance | 1.5 / 1.2 / 1.0 seconds |
| Battery IRST | Not installed |
| Launchers | Four, three rounds each |
| Shared reserve / healthy empty-launcher reload | 12 rounds / 90 seconds |

The effective ranges preserve the original supplied figures. Outer cutoffs are provisional values providing gradual quality decline beyond the effective range. Terrain, clutter, radar health and measurement age affect quality. Zoom only changes the display. Scan speed maps linearly between configurable `sweepSlowSeconds=10` at rating 1.00 and `sweepFastSeconds=5` at 6.00; faster scans do not increase range.

`radar.channels` is now the shared external-guidance pool. Each supported semi-active missile or active missile in midcourse occupies one channel. A hard lock without a missile occupies no missile channel. Active seeker activation releases the channel even if acquisition has not yet succeeded. IR uses no channels. Legacy illumination/midcourse/support fields remain accepted for old configuration files, but do not create separate live budgets. The legacy `earlyActivation` flag is accepted for compatibility; this update always applies the guide's early-search fallback after lost external updates.

## Existing player missiles

| Missile | Guidance | Nominal range | Maneuverability | Explosive content |
| --- | --- | ---: | ---: | ---: |
| MIM-301 Rampart | Semi-active radar | 24 km | 6.00 / 10.00 | 15 kg |
| FIM-352 Stonebolt | Semi-active radar | 28 km | 5.00 / 10.00 | 15 kg |
| MIM-303 Sentinel | Active radar | 26 km | 7.00 / 10.00 | 15 kg |
| FIM-306 Ember | Infrared | 12 km | 10.00 / 10.00 | 15 kg |

All currently use blast-fragmentation warheads and a provisional 0.20 km blast footprint. The 0.12 km proximity-fuze radius and 0.025 km direct-hit radius are separate settings. Guidance duration/speed/mass/AOA/thrust/burn remain the prior profiles: Rampart 35 s / 2,400 km/h / 140 kg / 12° / 18 kN / 5 s; Stonebolt 40 / 2,700 / 180 / 10° / 22 / 6; Sentinel 40 / 2,600 / 160 / 16° / 21 / 6; Ember 28 / 2,592 / 90 / 35° / 14.4 / 4. These values are game balance, not validated weapon specifications.

Maneuverability accepts fractional ratings 1.00–10.00. `missile.turnGAtOne=4` and `missile.turnGAtTen=22` configure its internal turning acceleration, preserving the existing player flight performance. The displayed rating is not itself a G value. Physical turns also obey AOA, speed and energy loss. Legacy `maxG` is retained in old profile files; the new rating controls current turning.

Rampart/Watchpost are starter equipment. Stonebolt retains 600 BP research and $1,500 purchase. Previously owned free Sentinel/Ember options remain. Switching among owned missiles does not add ammunition or change in-flight missiles' captured profiles. No beta-exclusive equipment is added.

## Provisional implementation settings

Additional settings are grouped and commented in `assets/equipment.properties` and `assets/combat.properties`. Defaults are validated in `Equipment.java` / `CombatRules.java` and can be overridden without source changes.

- Sensor quality/freshness: acquire observations, minimum qualities, effective/outer envelopes, fresh/track/contact timers, stable allocation, radar switching and lock delays.
- IRST, when fitted: 12 km nominal sensitivity range, 1 s update, capacity 4, 1.5 s acquisition, 4 s stale timeout, 0.6 s switching, sensitivity 1. IRST is passive and provides bearing/elevation rather than precise range. These settings remain dormant on the current Watchpost.
- Missile seekers: existing 8 km active / 9 km IR range, 60° FOV, 6 s search timeout; IR acquisition 0.35 s and sensitivity 0.5. Radar support recovery is 4 s. Early active search and bounded SARH retargeting are enabled; IR lock-after-launch acquisition is enabled by `irLockAfterLaunch`.
- Damage: explosive content, warhead type, blast footprint, fuze/direct-hit distances, damage scale 12, falloff exponent 1.2 and kinetic coefficient 0.002 are configurable per missile. Default aircraft health is 100 and incoming-missile health 60, vulnerability 1; aircraft types support individual health/vulnerability overrides.
- Mission 1 remains Smart Level 1 / Rookie. Twelve existing aircraft and up to ten simulation minutes remain. Four launchers share the reserve; damage can disable a launcher or slow its reload.

## Compatibility

Package `com.jb.radar`, signing identity, save keys and economy schema remain unchanged. Dollars, BP, research, ownership and selected loadout survive an in-place update. No currency is spent on ammunition or repairs. Active battles do not resume after process termination. Never publish signing keys or passwords.
