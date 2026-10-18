# Settings backup and restore

Open **Settings → Help & Maintenance → Back up and restore settings** (also searchable).

Save exports a UTF-8 JSON file to Downloads/LumaFin on Android 10 and later. Earlier Android versions use the system document picker. Copy the file elsewhere before uninstalling or replacing the TV; this feature does not upload or synchronize it.

Restore a saved backup lists up to 100 recent, app-accessible backups from Downloads. Import a backup file uses Android's document picker for files from other locations or TVs. Some Android TV devices lack that picker; the app explains this and retains the local chooser. No broad storage permission is requested. Files copied by another app may require Import rather than the local chooser because of scoped-storage ownership.

## Supported scope

Version 1 includes 28 explicitly allowed local values: Home row order/visibility, four Home item limits, the two hide-watched filters, theme/background/card size/clock/watched indicators, reduced motion, episode spoiler controls, series thumbnails, rating badges, autoplay, cinema prerolls, skip lengths, and subtitle brightness/font size/weight/position/colors. Defaults are included, so restoring a backup can reset these supported preferences to the backed-up defaults.

Excluded: credentials/tokens, usernames, user/server IDs and addresses, library-specific server display preferences, pinned shortcuts, recommendations, watch history, playlists, screensaver preferences, other playback settings, player selection, device/decoder/HDR/audio capability overrides, diagnostics and migration flags. The readable screen describes the supported subset before saving or restoring.

## Format and compatibility

The root has `format: "lumafin-settings"`, integer `version: 1`, and a `settings` object. Names are stable local preference keys; values retain their boolean, number or string type. No source-account mapping information is exported. A backup is explicitly mapped to the signed-in destination account/server displayed on the restore screen. It never signs in, creates users or writes server settings.

The reader rejects files larger than 64 KiB, nesting deeper than eight levels, wrong types, invalid numeric ranges, unknown enum/Home-row values, duplicate Home rows and unsupported format versions. Unknown setting keys in version 1 are skipped and counted. Missing settings remain unchanged. A malformed known setting rejects the entire restore before any write. Future format versions require explicit support rather than guessing.

Review displays every changed setting with its current and incoming values, plus matching/skipped counts. A matching backup offers no Apply action. Cancel/back before confirmation makes no changes. Apply requires a second confirmation naming the destination. Switching accounts (even away and back) or changing supported preferences invalidates a pending review. The write boundary revalidates keys and values, commits all changes to one account file, and rolls in-memory values back if the disk commit fails. A persistent storage failure can also prevent rollback from being persisted; the UI reports failure rather than success.

## Validation

Focused unit tests cover complete round trips, secret/device-key exclusion, partial compatibility, bad types/ranges/versions/rows, size/nesting limits, account and guest guards, stale reviews, recreation persistence and failed-commit rollback. Emulator checks use the test account: save/local chooser, matching backup, cancelled review and confirmation, Home visibility and subtitle brightness restoration, remote scrolling, restart persistence and missing document-provider feedback. External document providers and physical TVs require device checks.
