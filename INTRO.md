# Mission 1 introduction — v0.13

The twelve story sections in `IntroSequence.SECTIONS` preserve the creator's wording, punctuation and paragraph breaks. Black background, #00ff00 text, Digital-7 Regular, letter reveal and underscore cursor. The font's missing ellipsis glyph is drawn as three Digital-7 periods at one character position; the story retains its Unicode ellipsis. Entire sections are word-wrapped in advance, so visible letters never change position as words reveal. Text uses 42 design pixels, 58px line height and a 1280x720 landscape canvas with safe-inset fitting. All three controls remain visible below the text.

## Timing and transition

Exactly 76 seconds of active playback, excluding pauses and initial asynchronous audio preparation. Starting character delays are 38ms regular / 26ms space / 110ms comma-colon / 190ms sentence punctuation or ellipsis / 220ms line break. Typing delays are uniformly scaled by 0.959158 to meet the requested overall duration. Each section retains its 300ms pre-delay and 2.1s completed hold; final hold is 2.8s.

PLAY and RESTART enter the intro; resume of an already-started mission does not replay it. Game.start is called only when the intro completes or SKIP INTRO is pressed. It begins at zero simulation seconds with normal radar gameplay, no intermediate briefing, threat roster or schedule. Pre-mission equipment browsing only exposes USA equipment.

## Audio

Source: user-supplied `the_watcher_s_desk.mp3`, 43.415458 seconds. Source SHA256 is recorded in `Update-Guide-v0.13.txt`. `assets/intro_music.mp3` is a 76-second music-only mix, built by crossfading that same track into a repeat for 3 seconds, then trimming. Fade in 0–0.8s; fade out 73.5–76s. Playback gain is 0.65, or zero when MUSIC OFF is selected. No narration or typing sound is added. Original menu music remains separate.

The crossfade and fades are rendered into one track to avoid two Android players drifting or restarting across pauses. MediaPlayer's position is the story clock. Muting keeps the score running silently in sync, so unmuting restores the correct point. Manual pause pauses audio and text; activity backgrounding and audio-focus loss do the same without adding wall-clock time. A manual pause remains paused after returning. Skip releases the player immediately; replay prepares a fresh player from zero. Audio errors fall back to a foreground-only story clock and show MUSIC UNAVAILABLE.

Regenerate the bundled mix with `tools/build-intro-music.sh /path/to/the_watcher_s_desk.mp3`. FFmpeg with libmp3lame is required for this optional asset-generation step; Android builds use the already bundled mix.

## Font

Actual `digital-7.ttf` Regular from the Digital-7 distribution by Sizenko Alexander, Style-7: https://www.dafont.com/digital-7.font (download https://dl.dafont.com/dl/?f=digital_7). Original `Digital-7-LICENSE.txt` is included alongside it. Its freeware-software permission requires credit; credit is included here, in README and in the game's Equipment / Stats screen. The included license separately describes commercial use.

## Verification and limits

IntroTest covers duration, section ordering/holds, pause, skip and replay. Desktop rendering/lifecycle checks cover all twelve layouts using the actual bundled font, no pre-intro mission activity, direct zero-time gameplay transition, audio state/65% gain, background/focus/manual pause, mute/unmute, replay and audio failure fallback. Existing economy/tech-tree persistence and combat suites also pass. APK manifest, v2/v3 signing identity and packaged assets are checked. Android device installation, speaker playback, touch behavior and decoder timing have not been tested on hardware. Process termination returns to the main menu; mid-intro and mission state are not persisted.
