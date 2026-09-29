# Story and mission selection — v0.15

PLAY → SELECT MODE → STORY → MISSION SELECT → Mission 01 / LOADOUT → START MISSION → existing intro → radar gameplay.

SURVIVAL: unavailable, visibly labeled and disabled. STORY currently has exactly one playable mission, BEFORE THE DAWN (06 OCT 1973 / 0445). Numbered 02–08 tiles follow the supplied reference but are locked placeholders with no missions or unlock rewards attached.

STORE opens the existing USA tech tree for BP research, Dollar purchases and equip actions. Back returns to mission selection. LOADOUT shows Watchpost and the two current USA missiles; only owned missiles can be equipped. START MISSION uses the saved selection for all launchers and is disabled after a failed save. Return through MISSIONS → STORE → RETRY SAVE to recover. Selecting Mission 01 also opens its loadout.

Android Back and visible Back controls return through loadout → missions → modes → main menu. Store has its own Back first. Menu music continues through selection/store/loadout and yields to the intro soundtrack on deployment. Foreground/background behavior is preserved. Pause/resume during gameplay returns directly to combat; RESTART replays the existing Mission 1 introduction. MAIN MENU clears the selection hierarchy.

Visuals use a native 1280x720 design canvas fitted to landscape safe insets: quiet grid, radar circles/ticks, drifting sweep/scan line, green chamfered borders, white type, separate cards without images, large STORE and LOADOUT controls. Existing main-menu title AIR DEFENSE is retained. No threat names, schedules or enemy roster are revealed.

MissionMenuCheck drives actual touch hit targets and verifies disabled cards, store and owned-loadout selection, parent return paths, background behavior, no early combat, intro-to-game deployment, resume/restart, and save-failure rollback/retry. IntroRenderCheck and TechTreeRenderCheck pass. Screens were visually reviewed and signed APK assets checked. Real Android installation/touch testing remains unavailable.

## v0.17 loadout expansion

The same navigation now presents four missile cards in a 2×2 grid. Sentinel (active radar) and Ember (IR) are free prototype options; Stonebolt retains paid research. All four use the same generic launchers/ammunition and preserve the intro gate. Equipment browsing shows only the four player profiles, never the enemy roster.
