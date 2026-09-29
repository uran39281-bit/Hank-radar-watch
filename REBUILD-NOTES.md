# AIR DEFENSE — Rebuild Design Notes

Rewritten and expanded from “air defance 101v2.”

This document defines the first core systems for rebuilding the game from the beginning. The player operates an air-defense battery, manages limited sensor and engagement capacity, and selects suitable missiles against airborne threats.

The game uses simplified mechanics inspired by real systems. Rules below describe the proposed game implementation; they are not universal claims about real equipment. **Added recommendation** identifies a new design choice. **Correction** identifies a technical misunderstanding in the original notes.

## 1. Detection, tracking, and locking

### Detection

Detection means that the radar has registered an airborne contact. It does not automatically establish the contact’s identity or intentions.

For this game, a detected but untracked contact displays:

- Its last detected position on the radar.
- Its last measured range and bearing.
- `---` for speed and altitude.
- No heading arrow or predicted movement.

Its marker stays at the last detected position until another successful radar sweep updates it. The displayed range is also the last measured value, rather than the contact’s hidden live distance.

**Correction:** A radar needs direction as well as distance to place a contact on a display. Real search radars may also measure altitude or radial velocity without the game’s tracked state. Hiding those values is a deliberate gameplay simplification.

### Automatic tracking — inspired by TWS

Tracking connects successive detections into a continuing estimate of a contact’s motion. Track-while-scan, or TWS, maintains tracks while the radar continues searching. Predicted positions between sweeps are estimates, not new measurements. [1]

The game automatically assigns available tracking slots. A tracked contact displays estimated range, altitude, speed, and heading, with movement predicted between successful sensor updates.

**Example:** Six contacts are detected, but the battery can track only two. The two tracked contacts receive predicted movement and detailed information. The other four remain visible at their last detected positions and update only when detected again.

**Added recommendation:** A new track begins as `ACQUIRING`. It becomes `STABLE` after enough valid observations. Do not reveal perfect speed and heading immediately after the first detection. A missed update changes the track to `COASTING`; repeated misses eventually make it `LOST`.

Prediction becomes less reliable as time passes without a measurement. A coasting marker should look faded and show the age of its last update. A lost track stops providing a firing solution and releases its tracking slot. An old contact marker may remain briefly as a clearly labeled last-known position before disappearing.

### Tracking priority

**Added recommendation:** Use this automatic priority order:

1. Preserve tracks currently needed to support an engagement or an existing lock.
2. Prioritize detected incoming missiles that threaten the battery or defended objective.
3. Prioritize contacts explicitly marked by the player.
4. Assign remaining slots to the closest eligible aircraft.

The player can select `PRIORITIZE`, but there is no manual `TRACK` button. If every slot is protected by an engagement, display `NO FREE TRACK SLOT`. Protection does not prevent a track from expiring when the sensor loses it.

### Lock

A radar lock is the game’s dedicated fire-control state for a tracked contact. It requires sufficient track quality and available fire-control capacity.

Locking allows the battery to support semi-active radar homing missiles. A lock may provide more frequent updates in the game, but it cannot guarantee a hit or make a blocked target visible.

**Correction:** Locking is more than “thinking about the contact a little harder.” It represents an appropriate fire-control function. Radar locking and target illumination are related but distinct; this game combines them into one player action for simplicity.

Selecting a different contact must not silently cancel an existing lock. Use an explicit `UNLOCK` or `CHANGE LOCK` action. If support is interrupted, show which missiles are affected.

## 2. Radar warnings and aircraft awareness

An aircraft’s radar warning receiver, or RWR, detects suitable radar emissions. It does not read the battery’s internal contact list. Separate missile-warning equipment can detect threats that do not produce a radar warning. [4]

Use the following simplified warning model:

| Battery or missile activity | Possible aircraft warning |
|---|---|
| Search radar is transmitting | Radar presence and possibly its approximate direction, if the aircraft can detect it. |
| A contact is tracked in TWS | No automatic separate “you are being tracked” warning. Detection depends on emissions and receiver capability. |
| Dedicated fire-control lock or illumination | A compatible RWR may recognize an increased threat or lock indication. |
| An active radar missile begins transmitting | A compatible RWR may detect the missile’s radar. |
| IRST observes a contact or an IR seeker tracks it | No radar warning from that passive observation alone. Other warning sensors or visual observation may still reveal a launch. |

**Correction:** Detecting a target does not automatically notify it. Likewise, track, lock, and launch warnings are not guaranteed universal steps.

**Added recommendation:** Give each aircraft a warning-equipment profile and an AI reaction delay. Aircraft without suitable warning equipment must not react to hidden player actions. Awareness can increase from `UNAWARE` to `ALERTED` to `DEFENSIVE`, but a lock should not automatically force every pilot into “panic mode.”

## 3. Missile categories

The initial game supports three homing categories:

| Category | Sensor family | Launch requirement in this game | Support after launch |
|---|---|---|---|
| Semi-active radar homing — S-A / SARH | Radar | Valid radar track, fire-control lock, and illumination capacity. | Continued target illumination. |
| Active radar homing — A-H / ARH | Radar | A sufficiently accurate track and an available midcourse support channel when required. | Target updates until autonomous seeker acquisition. |
| Infrared homing — IR | Infrared | A valid missile-seeker lock; radar or IRST may help point the seeker. | No battery guidance after launch in the basic model. |

These are the three categories supported by the game, not an exhaustive list of real missile guidance methods.

### Semi-active radar homing

A semi-active missile homes on radar energy reflected from an illuminated target. The missile does not supply its own target-illuminating radar transmission. [5]

In the game:

- The battery must maintain the required lock and illumination.
- Loss of illumination changes missile status to `SUPPORT LOST`.
- **Added recommendation:** The missile briefly continues toward its last estimated intercept area. It can resume homing if valid support returns within a configurable recovery window.
- If support does not return, the engagement fails. Self-destruction after a configured timeout is a game rule, not a universal real-world response.

Do not make a missile fly randomly when support disappears. Show a clear loss-of-guidance state.

**Retargeting — preserved as an optional game mechanic:** Quickly changing the illuminated target can give an in-flight S-A missile an opportunity to acquire the new target. It succeeds only if the fictional missile supports retargeting and the new target is within its acquisition and maneuvering limits. Changing the lock does not guarantee redirection, and it can affect every missile relying on the old illumination.

### Active radar homing

Active radar missiles carry their own radar seeker. A representative real system, AMRAAM, combines inertial flight, target updates over a datalink, and autonomous radar homing near the target. [2]

Use these game states:

1. `MIDCOURSE`: the missile follows an estimated intercept while receiving available updates.
2. `SEARCHING`: its seeker is transmitting and searching near the predicted target position.
3. `ACTIVE — TARGET ACQUIRED`: its own seeker has acquired a valid target and guides independently.

If battery updates stop, the missile continues using its last available estimate. **Correction:** Loss of the battery track does not inherently force every active missile to switch its seeker on immediately. In this game, early activation may be a missile-specific feature; otherwise, the seeker activates according to its normal game logic.

The seeker has its own limited detection range and viewing area. Turning it on does not guarantee acquisition. If it never finds a valid target before its configured timeout, the missile fails and is removed or self-destructs according to the game’s failure rule.

Use `ACTIVE — TARGET ACQUIRED` for the player’s successful handoff indication. Do not display that message merely because the seeker has started searching.

### Infrared homing

An IR missile uses its own infrared seeker. An IRST is a separate search-and-track sensor that can help locate a target and cue the missile toward it. IRST sensing is passive. [3]

**Correction:** IR missiles do not universally require an IRST, and “launch first, then turn on the seeker” is not the standard rule for every IR missile.

**Added recommendation:** Use lock-before-launch as the basic IR mechanic:

1. Radar, IRST, or player selection cues the seeker toward a contact.
2. The missile’s seeker attempts to acquire that contact.
3. The interface displays `IR SEEKER LOCK` when acquisition succeeds.
4. The player launches, and the missile guides independently.

Reserve lock-after-launch for fictional missiles explicitly equipped for it. Those missiles require their own launch conditions and failure rules.

### Target selection by missile seekers

**Correction:** Do not make every IR missile lock the first hot object, or every active radar missile lock the first radar return. Seeker capability and acquisition conditions matter.

**Added recommendation:** Seekers attempt to acquire their assigned target within a limited search area. Older or less capable fictional seekers are more vulnerable to decoys or mistaken acquisition. Better seekers resist these effects more effectively, without becoming infallible. A lost seeker may search again for a limited time; it cannot instantly see every contact on the map.

## 4. IRST and radar cooperation

Radar tracks may cue the IRST toward a contact. An IRST observation may also cue the radar toward an area. Cueing means “look here”; it does not guarantee detection.

**Correction:** An IRST cue cannot make radar see through terrain or overcome every radar visibility limitation.

**Added recommendation:** Represent weather, visibility, target heat contrast, viewing angle, and obstruction as simple sensor-quality modifiers. A hot day should not automatically disable IRST. Cloud or poor thermal contrast may reduce performance in the game.

An IRST-only observation should not automatically provide an exact range. Show `RANGE UNKNOWN`, an explicitly labeled estimate, or a radar-derived range when available. Keep radar and IRST observations associated with the same contact ID to avoid duplicate targets.

## 5. Effective ranges and absolute limits

Preserve the original idea that performance becomes unreliable near the edge of a sensor’s useful range. However, separate nominal performance from hard game limits.

| Stat | Meaning |
|---|---|
| Effective detection range | Nominal search performance under the game’s reference conditions. |
| Effective tracking range | Nominal range for maintaining useful track quality. |
| Effective lock range | Nominal range for reliable fire-control support. |
| Absolute sensor limit | The outer boundary beyond which that sensor cannot produce a valid detection, track, or lock in the game. |
| Missile engagement envelope | The missile’s separate launch restrictions, including minimum and maximum usable range. |

**Correction:** A stated 30 km tracking range does not automatically establish that a system can lock targets at 35–40 km. That behavior must be explicitly defined for the fictional battery.

**Added recommendation:** Let favorable conditions occasionally support operation beyond an effective range, up to the relevant absolute limit. Show `MARGINAL` when quality is poor. Use smooth quality changes and a short persistence period so contact states do not flicker continuously at a boundary.

Being inside radar range does not automatically place a target inside missile range. Likewise, a visible contact is not necessarily launchable.

## 6. Tracking, illumination, and datalink capacity

**Correction:** Datalink capacity is not a universal count of all missiles that can be fired. Tracking capacity, illumination capacity, supported missile count, ready ammunition, and launcher readiness are separate constraints.

Use these game resources:

- **Tracking slots:** Number of contacts maintained as radar tracks.
- **Illumination channels:** Number of separate targets receiving S-A support at once.
- **Midcourse support channels:** Number of A-H missiles receiving battery updates at once, using one channel per supported missile as a game simplification.
- **Launchers and ammunition:** Number of missiles ready to launch, subject to reload and firing delays.

**Added recommendation:** Multiple S-A missiles engaging the same illuminated target can share that target’s illumination channel, subject to a separate supported-missile cap. A-H missiles release their midcourse channel after autonomous acquisition. Basic IR missiles use neither illumination nor midcourse channels after launch.

**Example:** A battery has two midcourse support channels. It launches two A-H missiles, which occupy both channels. A third supported launch is unavailable until a channel is released. Once one missile acquires its target autonomously, a channel becomes available even though that missile remains in flight.

**Original overflow idea — optional arcade rule:** Firing another supported missile may deliberately disconnect the oldest supported missile. Label this option clearly. **Recommended default:** Block the extra launch with `NO FREE SUPPORT CHANNEL`, and allow the player to explicitly release an existing missile’s support. Never silently disconnect an engagement.

If a simpler first version is needed, combine these resources under `GUIDANCE CHANNELS`. Label that combined count as an abstraction and define exactly which missiles consume it.

## 7. Visual language

Preserve the original distinction between dotted and solid shapes:

| State | Visual |
|---|---|
| Detected but untracked | Contact marker at last detected position; no tracking circle. |
| Tracked | Dotted circle around the contact and a heading indicator. |
| Radar locked | Solid circle around the contact. |
| Coasting track | Faded dotted circle and a last-update age. |
| Lost track | Brief last-known marker labeled `LOST`; no valid firing solution. |
| IR seeker locked | A distinct small bracket plus `IR LOCK`, so it cannot be confused with radar illumination. |

Missile support lines:

| Missile state | Line |
|---|---|
| S-A receiving support | Solid line from battery to missile, preserving the original requested appearance. |
| A-H receiving midcourse updates | Dotted line from battery to missile. |
| A-H searching without acquisition | No solid missile-to-target line; display `SEARCHING`. |
| A-H target acquired | Solid line from missile to its acquired target. |
| IR seeker tracking | Solid line from missile to its acquired target. |
| Support or seeker track lost | Remove the confirmed guidance line and display the appropriate lost-status label. |

The battery-to-S-A-missile line is a **support indicator**, not a literal radar beam. Semi-active illumination is directed at the target. A selected-engagement overlay may show battery-to-target illumination separately.

**Added recommendation:** Show guidance lines only for selected engagements or provide a toggle to reduce clutter. Sensor and missile status should remain understandable without relying on color alone.

## 8. Launch feedback and first-build scope

Before launch, check the selected missile’s required track or seeker state, compatible launcher, ready ammunition, relevant support capacity, and game engagement limits. Explain a blocked launch with a specific message such as:

- `TRACK TOO WEAK`
- `RADAR LOCK REQUIRED`
- `IR SEEKER NOT LOCKED`
- `NO FREE ILLUMINATION CHANNEL`
- `NO FREE SUPPORT CHANNEL`
- `OUTSIDE ENGAGEMENT ENVELOPE`
- `LAUNCHER RELOADING`

**Added recommendation:** The first playable build should include radar sweeps, limited automatic tracking, explicit locking, the three missile categories, support loss and recovery, aircraft warning profiles, and the visual states defined above. Keep equipment values configurable until playtesting establishes useful balance.

Identity and sensor quality are separate. A stable track does not automatically reveal aircraft type or prove that a contact is hostile. Detailed identification, additional equipment, missions, and progression can be specified later.

These are the initial foundations of the rebuilt game. More mechanics will be added after the basic sensor and missile systems work consistently.

## Reference notes

The sources below support the broad technical distinctions. Capacity rules, timers, UI conventions, priorities, failure behavior, and fictional equipment limits in this document are game-design proposals.

1. U.S. Navy training text, “Track-While-Scan Concepts,” archived by FAS — periodic observations and predicted track positions: https://man.fas.org/dod-101/navy/docs/fun/part06.htm
2. NAVAIR, “AMRAAM” — inertial midcourse flight, datalink updates, and active terminal homing: https://www.navair.navy.mil/product/AMRAAM
3. Lockheed Martin, “5 Reasons IRST21 Is Revolutionizing the Battlespace” — passive infrared observation: https://www.lockheedmartin.com/en-us/news/features/2021/5-Reasons-IRST21-Is-Revolutionizing-The-Battlespace.html
4. U.S. Army, Aircraft Survivability Equipment — separate radar-warning and missile-warning functions: https://cpeisw.army.mil/pm-ase-staging-with-video-banner/
5. U.S. Air Force, “AIM-7 Sparrow” — semi-active radar homing: https://www.af.mil/About-Us/Fact-Sheets/Display/Article/104575/aim-7-sparrow/
