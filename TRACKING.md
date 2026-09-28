# Automatic tracking, LOCK and missile support — v0.16

Detection provides a measured position and range; tracking provides detailed estimates and predicted movement. TRACK is removed from the UI. The radar assigns tracks automatically after detection, within the battery's tracking range and simultaneous-track cap. Watchpost retains its configured five tracks and two datalink channels; the three-of-six example is covered by a configured test battery, not a change to Watchpost stats.

## Radar observations and display

All aircraft and incoming missiles are observed only when the search beam reaches them. Detection is bounded by the battery's detection range, terrain/horizon, clutter/probability and radar/command damage. Changing RANGE LIMIT zooms the display; it no longer prevents detection outside the displayed radius. No player-priority fast refreshes or independent frequent incoming-missile refreshes remain.

A successful observation stores range/position and noisy estimated altitude, speed and heading. Only assigned tracks expose the detailed estimates. Untracked contacts display their last measured position/range and `---` for altitude, speed and heading; their icons have no direction. Internal simulation truth is never used for displayed prediction. Track icons move using the last observed speed/heading until a fresh detection corrects the estimate. Stored altitude/speed remain estimates from the last detection, not continuously sampled live data.

Tracked contacts have identity-colored dashed rings; hard locks have a solid ring and white star. Untracked aircraft use a fixed diamond, and untracked missiles a directionless circle. Existing gray/orange/red identification rules are retained. A white caret marks the selected contact. Scope labels include range; selection/hit-testing uses the same predicted coordinates as drawing.

## Allocation and player priority

Existing locked tracks and tracks with any live friendly missile engagement are protected from priority replacement. Protection lasts through an active engagement (including independent seeker flight) but does not override real track loss, expiry, radar outage or tracking-range limits.

Remaining slots sort by explicit player priority (most recent request first), incoming missiles, then closest aircraft, using measured range and contact ID for deterministic ties. Player requests override ordinary automatic ordering. No friendly aircraft are currently spawned.

PRIORITIZE marks the selected contact and replaces the lowest-ranked unprotected track if needed. CLEAR PRIORITY removes that override and returns the contact to normal automatic ranking; it does not manually disable automatic tracking. If every allocated track is protected, the request is refused with ALL TRACKS PROTECTED / NO SLOT. Stale untracked contacts must be detected again before acquiring a new track slot. Contacts beyond tracking range can still be detected but cannot be prioritized into a track.

Destroyed or expired tracks immediately free capacity for the next eligible detection. Player priority preferences remain with a surviving contact for reacquisition. Slot allocation consumes no datalink channels and never establishes LOCK.

## Missed detections and expiry

A failed beam pass immediately marks STALE; without an explicit failed pass, age above 1.15 sweep periods also marks STALE. A small S appears by stale scope plots and the selected readout shows LAST SEEN age. Predictions stop at the first missed pass or freshness deadline, whichever is earlier. Stale assigned tracks retain their cached detail, explicitly labeled stale; new launches and ground guidance updates require a fresh track. Hard locks break when their track becomes stale.

Track expiry is max(22 seconds, 2.2 sweep periods). Old detection plots remain until max(32 seconds, 3.2 sweep periods), as untracked stale contacts with masked detail. Radar outage drops assigned tracks/locks but leaves old plots temporarily visible. Reacquisition updates observations and can regain an automatic slot without silently reestablishing a hard lock.

## Datalink allocation

The existing channel convention is retained: one channel per supported target, shared by shots to that target. Explicit hard locks and active missiles awaiting seeker acquisition share the same battery budget. TWS tracking without a launch consumes a track slot but no datalink channel. Launching at a new active-missile target needs a free channel; launching at an already supported target shares that channel. FIRE is disabled when a requirement fails, with messages including TRACK REQUIRED, LOCK REQUIRED, OUT OF RANGE and NO DATALINK CHANNEL AVAILABLE.

Active missiles receive updates from a valid TRACK before onboard seeker acquisition. They do not require LOCK during this phase. A lost track or power interruption removes support and frees the channel. Reacquisition can restore updates while the missile still survives, provided a channel is available; it cannot steal a channel from an existing hard lock or earlier supported target. More than four continuous seconds without guidance still loses the missile under the existing arcade model.

When an active missile acquires its target within its configured seeker range and line of sight, it becomes autonomous. It then ignores battery tracking/lock/power loss. Its midcourse allocation ends; a shared allocation remains if another missile still needs updates to that target. Missile destruction, target destruction and expired guidance also release allocations. An explicitly selected hard lock remains until the player releases it or loses radar support, even after the active missile becomes autonomous. This avoids silently changing the player's selected radar mode. Radar missile labels show DATALINK, ACTIVE or LINK LOST.

Semi-active guidance requires a maintained hard lock throughout flight. Releasing LOCK can leave the target in TRACK but immediately interrupts semi-active guidance. Reestablishing the lock before the projectile is lost can restore guidance. Semi-active flight labels show SARH or LOCK LOST.

## Equipment and AI

The supplied MIM-301 Rampart and FIM-352 Stonebolt profiles remain semi-active, as originally specified. No new missile or research purchase was invented for this update. Active behavior applies to profiles with `guidance=RADAR` and `radarMode=ACTIVE`; `seekerKm` defines acquisition distance. Tests exercise active profiles through the existing validated equipment configuration. Guidance labels on the UI use the selected profile rather than a hard-coded semi-active label.

Automatic track acquisition and LOCK produce distinct PilotAI warning events. Silent TRACK is hidden by default; when `ai.trackingCue=true`, it can produce only a TRACK warning. Explicit hard locking produces LOCK, while an active TWS launch produces LAUNCH without implicitly producing LOCK. Datalink allocation or seeker handoff never creates a hard-lock warning.

## Checks

AutoTrackingTest covers the three-tracks/six-contacts case, masked untracked detail, snapshot-only prediction, missile/manual priority, protected lock/engagement replacement, destruction/expiry refill, stale freeze/reacquisition, sweep-only observations and zoom-independent detection. AutoTrackingRenderCheck exercises the actual Canvas output and touch targets, stale feedback and disabled FIRE. TrackingTest covers automatic track support, active firing beyond hard-lock range, shared channel limits, blocked-launch ammunition conservation, multiple missiles sharing a target, seeker handoff and independence, track/power loss, channel recovery without stealing a lock, guidance expiry, manual-lock preservation, semi-active interruption/recovery, state loss and separate AI cues. A simulated active TWS engagement acquires its seeker and intercepts geometrically. Existing equipment/economy/tech-tree/AI suites and ten full seeded missions pass. Desktop renders check the three states, identity colors, guide and feedback. APK signing/build is verified; physical Android installation/touch and balance playtesting remain unverified.
