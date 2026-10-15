# Settings organization (#24 and #35)

Approved October 15, 2026. Search stays on the root page and only enters editing after Select.

## Main groups

| Group | Existing controls |
| --- | --- |
| Playback | Player, photos, Autoplay, trailers, quality, subtitles, intro/segment skipping, Advanced |
| Appearance & Home | Libraries, Home rows, Screensaver, theme, clock, backgrounds, badges, motion and spoiler controls |
| Accounts & Servers | Saved servers/users, sign-in behavior and account ordering |
| Help & Maintenance | Artwork cache, crash reporting, About and licenses |

Existing route identifiers and preference keys remain intact. Search links directly to existing destinations, including screens nested under Help. Login entry points keep their Login heading.

## First implementation pass

- Four root groups with navigation descriptions.
- Home, Libraries and Screensaver placed before individual appearance controls.
- Support tools grouped in a Help page.
- Playback navigation descriptions and explanations for display matching, zoom, output, buffering, video delay and Live TV direct playback.
- Subtitle brightness reset explicitly describes its single-setting scope.
- Debug build and four settings search tests pass.

## Remaining review

- User confirmed main grouping screenshots, return focus and preserved cache searches after returning from Maintenance.
- Advanced Playback is now a hub for Seeking & Resume, Video, Audio, Subtitle Compatibility, Live TV and Troubleshooting. Existing controls and preference mutations are retained; buffering belongs in Video.
- Resume pre-roll is now Rewind when resuming, and Media segments is Intro, recap & credits throughout settings and search.
- Search includes new advanced groups and current values for subtitle compatibility and audio toggles. New submenu navigation still needs user validation.
- Complete the remaining current-value coverage audit.
- Subtitle appearance/position controls and playback timing-dialog consistency.
- Subtitle pass implemented: labeled Appearance and Position groups, separate scoped appearance/position resets, playback tracks/timing guidance and a compatibility link. Timing dialog descriptions now explain adjustment steps, bounds, reset scope and Back; playback behavior is unchanged. Build and four search tests pass; manual subtitle/reset validation is pending.
- Intro, recap and credits grouping, respecting existing server/player capabilities.
- Review all supported reset descriptions against their actual preference mutations; validate reset scope without resetting unrelated user settings.
- Visual review at TV distance, including scrolling and selected states.

Neither roadmap item is complete after this first pass. No user preference is migrated or reset by the navigation changes.

## Bitrate reset verification

Inspected Video, the bitrate picker/reset, Streaming Quality and search in the emulator. Renamed the ambiguous Reset to Default action to Reset bitrate limit, with an explicit explanation that it restores the default limit and switches quality to Custom.

Verified in the app: changed 100 Mbit/s to 110 Mbit/s, reopened the picker, reset to 100 Mbit/s, and observed both the selected radio option and refreshed search value. Streaming Quality showed Custom after reset. Restored the original Original Quality selection and confirmed search showed it. Debug build passed. Other setting resets were not exercised during this pass.

## Final coverage and consistency review

Existing playback timing checks are accepted; do not repeat them because explanatory text changed. The user confirmed those were already completed.

Search now carries route parameters for individual HDR formats, passthrough formats and server segment types. Each result includes the current preference/action. Added values for skip lengths, video delay, resume rewind, next-up countdown, screensaver timeout/rating, software codecs and subtitle colors. Added discoverable countdown/skip/bitrate reset entries. Result activation still opens a page rather than performing a reset. Parameter preservation has a regression test; five search tests and the debug build pass.

HDR and passthrough overview rows observe their preferences. Recap, credits and other segment detail pages explain supported segment requirements and all three actions consistently with Intro.

Reset implementation audit:

| Action | Actual scope |
| --- | --- |
| Home layout | Visible sections/order, with existing undo |
| Home item limits | Four row limits only, each defaulting to 50 |
| Skip lengths | Back 10 seconds and forward 30 seconds |
| Next-up timer | Countdown timeout only, 7 seconds |
| Subtitle brightness | Brightness only, 100% |
| Subtitle appearance | Six style values; excludes position and timing |
| Subtitle position | Bottom offset only, 8% |
| Playback subtitle/audio timing | Corresponding current-video offset only, zero |
| Bitrate limit | Default 100 Mbit/s plus Custom quality mode, now explicitly described |
| HDR/passthrough/codec Auto | Removes that individual manual override |

Reset descriptions match these mutations. No global reset was introduced and no user settings were changed during this source audit. Advanced pages display their existing values or selection markers; non-scalar destination pages such as library/account choosers do not have a single current value.

Final emulator verification passed: HDR10 search opened the HDR10 override page with Auto selected; Bitstream Dolby Digital audio opened that format with Auto selected; Recaps opened its segment action page with Do nothing selected. Search values matched the destination selections. Back restored the same focused result and preserved each query. No preference changes were made. Prior focus, persistence, readability, reset and playback checks remain accepted; the final search-link check is complete.
