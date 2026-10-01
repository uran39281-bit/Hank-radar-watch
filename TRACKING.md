# Radar, tracking and guidance — v0.19

The update follows `UPDATE-REFERENCE-v019.txt` while preserving the existing campaign and omitting the beta-only equipment/roster. `EQUIPMENT.md` documents fictional balance and provisional settings.

## Measurements and tracking

Radar detections occur on beam revisits. They store measured position, bearing, range and detection time. A detected contact need not own a tracking slot. Untracked contacts remain at their last measured plot between detections, with `---` for altitude, speed and direction. They do not accumulate hidden motion/NCTR history that becomes instantly available when selected.

AUTO TRACK allocates capacity-limited TWS tracks. New assignments need actual tracking observations before detailed motion is usable. Subsequent revisits correct the cached motion estimate. Between them, prediction uses those estimates, never live world coordinates. Missed detections or stale age lower quality and produce COASTING; bounded prediction can continue as an estimate while clearly stale. Insufficient quality blocks a new radar firing solution. Expiry releases the slot; a last-detection plot can fade later.

Protected locks/engagements, incoming missiles, explicit player priority, retained tracks, then nearby contacts determine allocation. Existing-track retention is configurable. PRIORITIZE cannot displace a protected engagement. Selection itself does not track, recognize or lock anything.

AUTO TRACK OFF stops new radar-track acquisition and detailed track refresh. Existing tracks coast and expire while the search radar may still update detection-only plots. RADAR OFF uses a switching delay, then stops radar measurements and illumination. RESET clears radar contact/track/lock state but preserves world aircraft, missiles, ammunition and independent IRST observations. Later sweeps repopulate plots. These actions warn through the controls/log and can interrupt missile support.

Display range does not affect sensor quality, detection range or refresh frequency. Scan-speed rating only changes revisit time. Effective ranges taper toward separately configurable outer limits; terrain, clutter, radar damage and freshness affect usable quality.

## Lock and identification

LOCK requires a usable track and takes a configurable acquisition time. A solid ring and star replace the dotted track ring when the lock is established. UNLOCK releases illumination. A configurable short loss tolerance handles degraded observations; sensor shutdown, hard envelope loss and track loss interrupt the lock. TWS may continue on other contacts.

Allegiance is UNKNOWN/HOSTILE/FRIENDLY; recognition is independently unrecognized/recognized. NCTR matches measured characteristics against its configured coverage, with history/confidence required. Recognition alone does not prove hostility. Scenario evidence such as an observed attack can establish hostile allegiance. Missing IFF does not. Unknown contacts use generic stable IDs and icons regardless of hidden aircraft/missile identity.

Canonical supplied icons retain their original colors and aspect ratio. Aircraft/missile orientation is drawn only when a heading estimate exists. Untracked diamonds, dotted tracks, solid locks, selection caret and IR seeker brackets are distinct. Black-backed originals are composited at runtime rather than recolored. No unassigned artwork is used to invent threat categories.

## Shared missile support

DATALINKS is a game abstraction: each externally supported SARH or active-midcourse missile occupies one channel, even when multiple missiles share a target. TWS alone and hard locks without a missile use no missile channel. IR uses none.

If the pool is full, FIRE shows a confirmation identifying the oldest supported missile and its consequence. Cancel preserves the engagement and ammunition. Confirm transfers that channel to the new missile. The old missile remains in flight: an active missile enters early seeker search; a semi-active missile starts its guidance-loss window. Destruction, expiry, explicit support release and active seeker activation free capacity.

Semi-active radar launches require a usable track and completed hard lock. Continued illumination supplies guidance. Loss retains the last estimate through a finite recovery period, then self-destructs if support does not recover. Explicitly unlocking the old target and locking a new one can retarget eligible missiles only inside the recovery window, seeker cone and remaining flight envelope. Selection alone never retargets.

Active radar launches require a usable track, but not a hard lock. MIDCOURSE uses cached measurements refreshed through its reserved channel. Lost updates or displaced support trigger early seeker search. The channel is released at SEARCHING activation, before acquisition. Only actual range/FOV/LOS plus acquisition dwell yields SEEKER LOCK. Search/reacquisition has a finite timeout and cannot magically find a distant or masked target. Guidance lines reflect battery support or confirmed onboard acquisition.

## IRST and infrared

IRST is a separate passive sensor when fitted, with its own power/switching, update, acquisition, capacity, thermal sensitivity, aspect, visibility/contrast, terrain obstruction and expiry rules. It can work with radar off. Its stored observations are angular; IRST-only plots sit on a labeled bearing ring and show RANGE UNKNOWN. Radar can cue IRST; IRST can request a radar revisit. Cueing does not guarantee detection or bypass obstruction.

The current Watchpost has no IRST. A fresh radar plot can point Ember's own thermal sensor, which must physically acquire a valid heat cue before firing. A fitted IRST can supply its own usable cue. With lock-after-launch enabled, a missile starts SEARCHING and acquires a physically valid heat source in range/FOV/sensitivity after its own acquisition dwell. The provisional simple search encounters the nearest valid source first, including flares; the selected contact is not guaranteed. No battery channels or updates are used after launch. Seeker loss removes acquired status and permits a bounded search.

## Effects and warnings

Missiles have finite acceleration, speed, turning, energy and lifetime. Proximity detonation occurs after closest approach, preserving the fix for premature inbound explosions. Warhead mass/type, detonation distance, falloff and target vulnerability determine partial damage or destruction. Kinetic profiles require direct impact. Enemy laser/command support uses the attacking aircraft's line of sight, not the player's radar state; passive anti-radar guidance needs radar emissions.

TWS allocation is not a fire-control warning. Pilot warning equipment, visibility/emissions, probability and delay determine awareness. Passive IR provides no automatic radar-lock/launch warning. Mission 1 retains Rookie pilots, slower warning reactions, no advanced notching and easier approach altitudes.
