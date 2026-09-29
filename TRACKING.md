# Radar tracking and missile support — v0.17

This implements the approved sensor/guidance rebuild on the existing game. See REBUILD-NOTES.md for the supplied design and EQUIPMENT.md for configurable fictional balance.

## Detection and track quality

The sensor observes a contact only when the sweep crosses it. Display zoom does not limit sensor range. A detection stores a noisy position/range and bearing. Untracked contacts remain at that measurement, show `---` for altitude/speed/heading and do not predict movement.

An assigned track begins ACQUIRING. Successive observations establish estimated motion; one detection does not reveal perfect speed or direction. Sufficient observations and quality produce STABLE. Tracking does not identify a contact automatically and never establishes a hard lock. Tracked motion is predicted from cached measurements, never from the hidden live position.

A missed detection or excessive update age produces COASTING. Cached estimates are visibly aged and faded; prediction is bounded and a coasting track cannot provide a new radar firing solution. After the expiry period the track is LOST, releasing its slot. A last-known plot can remain briefly before disappearing. Reacquisition must rebuild useful observations and does not silently restore a hard lock.

Watchpost's normal sweep is ten seconds. Freshness is 1.15 sweep periods; track expiry is at least 22 seconds or 2.2 sweep periods; plot expiry is at least 32 seconds or 3.2 sweep periods. Actual failed sweeps mark coasting immediately. Radar outage removes radar support while preserving recent last-known plots.

## Allocation

Allocation order is protected support/locks, incoming missiles, player-prioritized contacts, then nearest eligible aircraft. PRIORITIZE never displaces a protected engagement or higher-priority incoming missile. Protection does not prevent expiry, loss of visibility to the sensor or sensor failure. Autonomous missile seekers do not need a protected battery track.

There is no TRACK button. An explicit LOCK/UNLOCK action changes fire-control support. Changing selection does not unlock another contact. Lock loss reports the affected supporting engagement. Identity-colored icons remain inside the track/lock geometry; radar locks use solid circles/star markers, tracks dotted/dashed circles, IR seeker locks distinct brackets. Stale/lost labels also distinguish status without color alone.

## Separate resources

Watchpost provides five track slots, two illumination-target channels and two active midcourse-missile channels. Tracking alone consumes no guidance channel.

Semi-active missiles sharing one illuminated target share that illumination channel. The independent support cap is six missiles. Launches still require a ready compatible selected launcher and remaining ammunition.

Active missiles each reserve a midcourse channel, including missiles fired at the same target. A temporary missed track does not silently give their reservation to another missile; recovery uses their existing reservation. Acquisition, expiry/destruction or explicit RELEASE SUPPORT frees it. The player sees a clear block when capacity is full. Basic IR uses neither pool after launch.

## Missile states

Semi-active missiles require a sufficiently accurate track and radar LOCK to launch. Continued illumination supplies guidance. Loss produces SUPPORT LOST; the missile continues toward its stored estimate during a configurable recovery period. Reestablishing support in time can recover guidance. No random direction change occurs, and unsupported missiles cannot score a guided hit.

Active missiles may launch from a stable track without LOCK. MIDCOURSE follows stored estimates refreshed through the reserved datalink. Losing updates leaves the last estimate; it does not automatically force early seeker activation. SEARCHING means the onboard radar is looking in its bounded acquisition area. Only actual range/FOV/LOS acquisition produces ACTIVE — TARGET ACQUIRED, independent guidance and channel release. Failure to acquire or reacquire before the configured timeout ends the missile. A manual radar lock is not silently released by seeker handoff.

IR missiles acquire their own passive seeker lock before launch. Radar cues the seeker; a battery IRST is not required. Terrain, aspect, heat strength, seeker range/cone and flares affect acquisition. IR can guide after battery tracking, locking or power is lost. Seeker loss removes the confirmed guidance line and allows a limited search; it cannot see every contact on the map. All three families use the equipped profile and L1–L3 inventory.

Guidance lines are limited to selected engagements and can be hidden: solid battery-to-missile for supported semi-active flight, dotted battery-to-missile for active updates, solid missile-to-target only for acquired active/IR seekers. No confirmed target line is shown during search or support loss. These are status indicators, not literal radio-beam geometry.

## Warnings and scope

TWS allocation never generates a special tracking warning, including when a legacy trackingCue configuration is present. Pilot warning profiles independently specify search-radar, fire-control, active-radar and optical/visual warning capabilities. Emission/visibility checks, probability and reaction delays determine delivery. Search presence can alert without forcing evasive action; incompatible receivers cannot read hidden player commands.

The stock Watchpost has no IRST, so independent IRST search and unknown-range IRST-only plots remain future equipment work. Optional semi-active retargeting and overflow eviction are disabled. This build is an arcade game abstraction, not an operational simulation.
