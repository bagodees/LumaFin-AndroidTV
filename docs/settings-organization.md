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
