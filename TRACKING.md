# TRACK, LOCK and missile support — v0.12

TRACK displays a dashed ring around a tracked aircraft or incoming missile. LOCK replaces it with a solid ring and a separate five-point star. The icon stays visible and retains its gray/orange/red identification color; the star is white. A small white caret beneath a selected contact identifies selection without resembling a hard-lock box. Releasing LOCK returns to the dashed ring if the radar track remains valid. Lost or unavailable tracks have neither tracking marker. Priority requests can remain queued for reacquisition, but are not drawn as valid tracks while support is lost.

TRACK requests TWS-style priority updates and needs valid radar observations. Search detection alone is not a player-designated TRACK. An active missile can fire from TRACK without a hard lock, up to the smaller of the battery's tracking envelope and missile range. A semi-active missile requires a hard lock, within the smaller of the battery's locking envelope and missile range. Other launch gates (minimum range, ceiling, available launcher/ammunition and power) still apply.

## Datalink allocation

The existing channel convention is retained: one channel per supported target, shared by shots to that target. Explicit hard locks and active missiles awaiting seeker acquisition share the same battery budget. TWS tracking without a launch consumes a track slot but no datalink channel. Launching at a new active-missile target needs a free channel; launching at an already supported target shares that channel. FIRE is disabled when a requirement fails, with messages including TRACK REQUIRED, LOCK REQUIRED, OUT OF RANGE and NO DATALINK CHANNEL AVAILABLE.

Active missiles receive updates from a valid TRACK before onboard seeker acquisition. They do not require LOCK during this phase. A lost track or power interruption removes support and frees the channel. Reacquisition can restore updates while the missile still survives, provided a channel is available; it cannot steal a channel from an existing hard lock or earlier supported target. More than four continuous seconds without guidance still loses the missile under the existing arcade model.

When an active missile acquires its target within its configured seeker range and line of sight, it becomes autonomous. It then ignores battery tracking/lock/power loss. Its midcourse allocation ends; a shared allocation remains if another missile still needs updates to that target. Missile destruction, target destruction and expired guidance also release allocations. An explicitly selected hard lock remains until the player releases it or loses radar support, even after the active missile becomes autonomous. This avoids silently changing the player's selected radar mode. Radar missile labels show DATALINK, ACTIVE or LINK LOST.

Semi-active guidance requires a maintained hard lock throughout flight. Releasing LOCK can leave the target in TRACK but immediately interrupts semi-active guidance. Reestablishing the lock before the projectile is lost can restore guidance. Semi-active flight labels show SARH or LOCK LOST.

## Equipment and AI

The supplied MIM-301 Rampart and FIM-352 Stonebolt profiles remain semi-active, as originally specified. No new missile or research purchase was invented for this update. Active behavior applies to profiles with `guidance=RADAR` and `radarMode=ACTIVE`; `seekerKm` defines acquisition distance. Tests exercise active profiles through the existing validated equipment configuration. Guidance labels on the UI use the selected profile rather than a hard-coded semi-active label.

TRACK and LOCK continue to produce distinct PilotAI warning events. Silent TRACK is hidden by default; when `ai.trackingCue=true`, it can produce only a TRACK warning. Explicit hard locking produces LOCK, while an active TWS launch produces LAUNCH without implicitly producing LOCK. Datalink allocation or seeker handoff never creates a hard-lock warning.

## Checks

TrackingTest covers track designation, active firing beyond hard-lock range, shared channel limits, blocked-launch ammunition conservation, multiple missiles sharing a target, seeker handoff and independence, track/power loss, channel recovery without stealing a lock, guidance expiry, manual-lock preservation, semi-active interruption/recovery, state loss and separate AI cues. A simulated active TWS engagement acquires its seeker and intercepts geometrically. Existing equipment/economy/tech-tree/AI suites and ten full seeded missions pass. Desktop renders check the three states, identity colors, guide and feedback. APK signing/build is verified; physical Android installation/touch and balance playtesting remain unverified.
