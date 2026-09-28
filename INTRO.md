# Mission 1 introduction — v0.14

Source: creator-supplied `Air_Defense_Digital7_Intro.zip`, retained at `docs/reference/Air_Defense_Digital7_Intro.zip`. Native Android Canvas implementation follows its `intro.css` and `intro.js`; no network, browser or WebView dependency is added.

## Supplied behavior

All twelve strings match the archive exactly, including single line breaks, straight quotation marks and three-period ellipsis. Character delays are unscaled: regular 38ms, space 26ms, comma/semicolon/colon 110ms, sentence punctuation 190ms, line break 220ms. First letter appears after 300ms and each letter's delay follows its reveal, matching the JavaScript. Holds: first section 1.8s, intermediate 2.1s, final 2.8s, after the final character delay. Total: 75.64 seconds.

Digital-7 Regular and #58ff7e on black; a narrower 896px text column, 41px font, 1.65 line height and slight character spacing on the 1280x720 design canvas. Full sections are laid out before typing and centered vertically as complete blocks. Visible letters never reflow. A small green cursor follows the newest letter. Footer controls use Digital-7 without visible borders: PAUSE/RESUME left, MUSIC ON/OFF center, SKIP INTRO > right. Touch areas remain large. Layout fits landscape safe insets.

The supplied font and soundtrack are used byte-for-byte. The soundtrack lasts 75.708 seconds (including MP3 padding), covering the full 75.64s story. Playback volume is 65%. The prepared fades and repeat are used as supplied, without applying a second fade or re-encoding.

## Android integration

Since v0.15, PLAY opens mode selection, STORY opens missions, and Mission 01 / LOADOUT opens the loadout screen. START MISSION calls the intro; RESTART replays it directly. The standalone HTML demo's landing, read-story view and RADAR ONLINE/replay page are not inserted into the game flow. Normal completion or skip immediately releases music and calls Game.start once; mission time is zero and no enemies move or spawn during the intro. No threat roster, names, numbers or schedules are added before gameplay.

MediaPlayer position drives typing. Pause stops both; background/focus loss freezes both and resuming continues from the same position. Manual pause survives backgrounding. Mute keeps playback synchronized silently. Replay creates a fresh player from zero. If audio fails, foreground-only text timing continues with MUSIC UNAVAILABLE. No narration or typing sounds are added. Process termination returns to the menu; mid-intro or mission state is not persisted.

The reference archive is the source of truth for audio. `tools/build-intro-music.sh /path/to/Air_Defense_Digital7_Intro.zip` restores its original soundtrack, font and license into assets without transcoding.

## Credits and validation

Digital-7 by Sizenko Alexander / Style-7. The original license is bundled as assets/fonts/Digital-7-LICENSE.txt; font credit remains in Equipment / Stats and README. It provides credited freeware-software use and separate commercial terms.

Verified exact reference story/music/font parity; 75.64s timeline and section holds; all twelve desktop layouts with actual Digital-7; mission gating, pause/background/focus, mute, replay, completion, skip and audio-error fallback; existing combat/economy tests. APK manifest, assets and existing signing identity checked. Real-phone playback, installation and touch behavior remain unverified.
