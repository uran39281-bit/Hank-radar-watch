# Equipment profiles — v0.8

Edit `assets/equipment.properties` and rebuild to change balance. The APK loads these profiles at startup. The menu's **EQUIPMENT / STATS** page displays the loaded battery and all seven missile profiles; browsing does not change the loadout. These are fictional arcade values. No purchases or tech-tree research are added in this update.

## Radar

| Property | Meaning / accepted values |
| --- | --- |
| `radar.name` | Display name, 1–28 characters |
| `radar.detectionKm` | Maximum detection radius, whole kilometres, 1–100 |
| `radar.trackingKm` | Maximum track radius; cannot exceed detection |
| `radar.lockKm` | Maximum measured slant lock range; cannot exceed tracking |
| `radar.tracks` | Shared aircraft and incoming-missile track slots, 1–72 |
| `radar.channels` | Shared target datalink / fire-control channels, 0 through track capacity |
| `radar.scanSpeed` | 1–10 rating; full sweep takes `11 - scanSpeed` seconds |
| `radar.infrared` | Whether the battery supports IR missiles (`true` / `false`) |
| `radar.irLockKm` | Maximum platform IR acquisition range |

Two observations can create a track inside the tracking envelope if a slot is free. Contacts outside that envelope can still appear as detections. A coasting track holds its slot for up to 22 seconds; death, track expiry or leaving the tracking envelope frees it. TRACK selects priority updates; it never bypasses the capacity limit. Priority updates run at 30% of the sweep period. Incoming missile measurements update at 20% of that period. Fresh-track time is at least seven seconds and scales for slow radars. Terrain, altitude clutter, radar damage and the selected display range still affect observations.

The current battery detects to 40 km, tracks to 32 km, locks to 25 km, supports 12 tracks and two channels, and scans in five seconds. Damage caps detection, tracking and lock ranges. A lost or out-of-range track releases its channel. Channels are reserved per locked target, shared by multiple shots at that target, matching the existing game's engagement controls. IR locks use no datalink channels; heat, aspect, ceiling and terrain can limit acquisition before the platform maximum is reached.

## Missiles

Each profile uses `missile.<id>.<property>`. IDs: `hawk`, `ir6`, `kh25`, `kh29`, `kh23`, `kh27`, `kh58`.

| Property | Gameplay effect |
| --- | --- |
| `name` | Display name |
| `guidance` | `RADAR`, `INFRARED`, `COMMAND` or `LASER` |
| `radarMode` | `ACTIVE`, `SEMI_ACTIVE`, `PASSIVE` for radar; `NONE` otherwise |
| `guidanceSeconds` | Maximum time homing can operate |
| `maxSpeedKmh` | Speed cap |
| `maxG` | Maximum turn acceleration in the arcade flight step |
| `massKg` | More mass means less acceleration at the same thrust |
| `maxAoADeg` | Angle-of-attack allowance limits turn rate and turn-related speed loss |
| `thrustN` | Maximum motor force; acceleration scales with thrust divided by mass |
| `burnSeconds` | Motor duration, no longer than guidance time |
| `rangeKm` | Maximum flight path and launch range |
| `minRangeKm` | Minimum launch range |
| `ceilingM` | Launch altitude gate |
| `seekerKm` | Active radar acquisition / IR heat-signature range scale where applicable |

MissileMotion.java applies the same finite acceleration, speed cap, G/AOA-limited steering, coast drag and turn energy loss to player radar, IR and enemy missiles. It is an approximate pursuit game mechanic, not an aerodynamic or operational guidance model. Homing stops at the guidance deadline; an unguided missile coasts briefly before expiry. Collision checks use swept proximity and terrain.

Semi-active and command player missiles need the ground target link. Active radar needs it until the missile's seeker acquires the target within its configured range; takeover releases the channel when no other dependent shot needs that target. Passive radar follows an emitting target without a ground channel. Aircraft emission is a fictional per-type game flag; active enemy missile seekers emit after acquisition. IR continues independent flight after launch and can be diverted by flares. Laser and command enemy missiles need a living, suitably oriented source aircraft with site visibility; passive enemy missiles need the surviving radar's emission. The radar always emits while alive; an emission-off control is not added.

The shipped loadout is still semi-active MIM-23 and IR-6, plus the existing five enemy weapons. Active and command player guidance can be selected in the primary profile for future equipment; passive needs an emitting target. The primary slot accepts RADAR or COMMAND; the IR slot must remain INFRARED. The enemy slots accept all defined guidance types. Values are configurable in the source asset, not through an in-game editor.

## Validation and progress

Numeric values must be finite and in their declared bounds. Whole-number fields, booleans, enum combinations, unknown property names and inconsistent ranges are rejected. Missing properties use defaults. An invalid or unreadable asset triggers the complete default loadout and a visible menu notice, avoiding a partially applied profile.

Run `./test.sh` using a Java JDK, or set `RADAR_ECJ` to an ECJ jar. EquipmentTest changes profiles and verifies effects on detection, tracking, capacity, channels, scanning, IR compatibility, acceleration, speed, turn limits, energy and guidance. Existing tests cover economy, IR, all five moving incoming weapons, ammunition and mission completion. Android preference names and signing identity are unchanged; saved Dollars and BP remain compatible with an in-place update. Device installation and playtesting remain unverified.
