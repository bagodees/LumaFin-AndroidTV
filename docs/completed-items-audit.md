# Completed-item regression audit — 2026-10-19

Scope: all 63 closed roadmap issues, #2–#66 excluding open #52 and deferred #60. Baseline was `28126c04d` on `lumafin-main`, including Claude's subtitle lookup, remembered choices, sync and accessibility commits. The worktree was clean before this audit.

This is a source/integration review plus automated regression tests and a representative emulator pass. It does **not** mean every setting, media format, server provider, large library or physical TV was retested. Original hands-on results remain in the issue comments and feature documents. Hardware acceptance remains open in #68; #52 must remain open until its listening/output checks are reviewed.

## Findings and fixes

1. **Sync review could overwrite a newer local setting.** A normal conflict review retained values already merged locally; resolving it checked only conflicting fields, then could replay an old nonconflicting value over a later edit. The review now snapshots all synced local values and refreshes the merge/review if any changed.
2. **Account switch during sync review recheck.** The server read could suspend across an account switch, then apply the old review to the active account. Revalidate account and review after that read and before local commit. Cancellation now propagates rather than becoming a connection failure. Compare the complete remote envelope, not just its revision, to detect competing copies with equal revision numbers.
3. **Android 6 track-picker crash risk (#48).** Track labels directly accessed Android 7's `Configuration.locales` despite minSdk 23. Use AndroidX `ConfigurationCompat.getLocales` with a default-locale fallback. Static NewApi findings for this path are eliminated. No Android 6 device was available for a runtime check.
4. **Tracking/documentation drift.** Some closed issues retained untouched planning descriptions or an empty body. Their current status is reconciled with existing completion comments. #67 incorrectly called completed #35 deferred. Historical pending statements in the settings organization document are labelled as superseded. Actual project access was available on this audit: #52 was incorrectly Pending and was restored to In Progress. Project listings omitted #42; a direct item lookup verified that it belongs to this project, is not archived, and is Done. Other completed cards were already Done.

Four added sync tests cover stale nonconflicting values, account switching during review, cancellation, and different server copies with the same revision. The account-switch and cancellation regressions failed against the pre-fix behavior; the final regression suite passes.

## Current feature map and reviewed integration

Paths below are relative to `app/src/main/java/org/jellyfin/androidtv/`; test names refer to `app/src/test/kotlin/`. All listed automated suites were included in the final app run. Some features rely on source/original manual evidence rather than a dedicated automated UI test.

| Issues | Current location/integration | Automated coverage or evidence |
| --- | --- | --- |
| #2, #29 | Playback → Advanced → Seeking & resume; `DirectionalSeek`, `SeekAcceleration`, seek provider | DirectionalSeek, SeekAcceleration, CustomSeekProvider tests |
| #3 | Details/player track and version menus; `PlaybackChoicePreferences`, playback startup precedence | PlaybackChoices, PlaybackChoicePreferences, PlaybackTrackStartup tests; docs/playback-preferences.md |
| #4, #31–#34, #36–#38 | Appearance & Home → Home; presets/reset/undo, row move, four limits and watched filters; Home helpers consume these values | RecentlyAddedLibraryLimit, AccountPreferences tests; source review of SettingsHomeScreen/HomeRowsFragment/helper |
| #5, #18, #39, #41, #42 | Playback → Autoplay/Next up/Intro, recap & credits; binge state and player handoff | NextUpCountdown, EpisodeBingeState, MediaSegmentRepository tests |
| #6, #30, #44, #45 | Appearance & Home; card size, rating badges and Reduced Motion; card presentation | docs/home-card-appearance.md; preference isolation/backup tests and source bindings |
| #7 | Help & Maintenance → Back up and restore settings; allowlisted format and destination review | SettingsBackup tests; excludes credentials, device overrides and unsupported settings |
| #8 | Home recommendation context menu; Appearance & Home → Home → recommendation management | Source review of recommendation menu/preferences and Home reload |
| #9 | AccountPreferences selected before publishing the authenticated user; per-account stores, queues and display caches | AccountPreferences, SessionSwitch, segment preference tests |
| #10, #20 | Library sort and combined filters; LibraryPreferences/runtime-filter controller | CombinedLibraryFilters, RuntimeFilterScan, RuntimeResultPage tests |
| #11 | Continue Watching context action and confirmation; server history effects | ContinueWatchingRemoval tests |
| #12 | Existing title/library menus and Home shortcut management | HomeShortcutPreferences tests |
| #13 | Appearance & Home → Remote shortcuts; card long-press dispatcher | CardLongPress, RemoteShortcutPreference tests |
| #14, #51 | Standard player's subtitle/audio timing dialogs; separate renderer offsets/reset per video | SubtitleTimingRenderer, AudioTimingRenderer tests; no physical lip-sync claim |
| #15 | Playback → Streaming Quality; device-profile bitrate limits and explanations | StreamingQualityProfile tests |
| #16, #64 | Player Playback information; optional buffer/frame metrics and explicit unavailable states | PlaybackDiagnostics tests; docs/playback-diagnostics.md |
| #17 | Standard player Sleep timer; duration/episode-end state and teardown | SleepTimer tests; source review of container/action/player completion |
| #19, #43 | Appearance & Home spoiler settings; episode description and safe series-artwork helpers | SpoilerProtection tests |
| #21, #22, #25 | Who's watching/Add User/Servers and Accounts & Servers; identity-verified backup/recovery | QuickUserSwitch, SessionSwitch, ServerAddress, BackupRecovery, AuthenticationRecovery, ConnectionFailure tests |
| #23 | Appearance & Home → Accessibility; activity text scale, Compose contrast and classic/Compose focus outline | Accessibility tests; docs/accessibility.md; physical TV limitations retained |
| #24, #35 | Four root groups and settings search; parameterized advanced destinations and explicit keyboard activation | SettingsSearch tests; all 59 referenced search routes have mappings; fresh emulator navigation |
| #26 | Help & Maintenance → diagnostic export; typed allowlist, no raw logs/credentials | DiagnosticExport tests and report source review |
| #27 | Help & Maintenance → Sync settings across devices; independent layout/opt-in per account | SettingsSyncModel/SettingsSync tests, including four new audit regressions |
| #28 | Optional first-run guide and Help revisit | SetupGuide, AccountPreferences tests |
| #40, #46 | Home rebuild after playback report, row replacement and return; cancel obsolete page work | DataRefreshService/paging tests; source review of Home lifecycle and replayed completion flow |
| #47 | Details version chooser, source labels and remembered-version matching | VideoVersionLabels, PlaybackChoices tests; multi-edition live fixture still needed |
| #48 | Details/player readable track labels | TrackLabels tests; Android 6 compatibility fix above |
| #49 | Get more subtitles inside existing subtitle pickers; permission/version gates and verified metadata refresh | SubtitleLookup tests; original successful provider/download/refresh evidence in issue comments |
| #50 | Playback → Subtitles brightness/reset and standard subtitle rendering | SettingsSubtitlesScreen/VideoManager source review; original user/emulator checks; HDR comfort requires physical TV |
| #53 | Help & Maintenance → Test streaming speed; bounded read without playback/progress reports | StreamingSpeed tests |
| #54 | Search → grouped results including people; actor detail navigation | SearchState, SearchViewModel tests; actor visual redesign remains #67 |
| #55 | Library alphabet jump, filtered available letters and unavailable-sort handling | AlphabetInput, AvailableAlphabet tests |
| #56 | Per-library Compact list display choice, shared filters/pagination/context menus | CompactListPreference, CompactLibrary tests |
| #57 | Details Extras/local trailers with omission when none | Extras tests |
| #58 | Existing title menus and Home Watch Later row; per-server/user local records | WatchLaterPreferences tests |
| #59 | Both built-in players' session queue menus; protected current/history and stale-edit guards | VideoQueueEditor tests plus shared playback queue tests |
| #61 | Library/series/season menus → explicit selection of loaded individual movies/episodes | BulkWatch tests; prior same-day emulator watched/unwatched/cancel/remote-toggle checks |
| #62 | Help & Maintenance → Maintenance; clear Coil image caches only | Source review confirms no preference/auth/history reset; original emulator checks retained |
| #63 | Standard player chapters, seek ticks and trickplay provider cancellation/fallback | ChapterNavigation, CustomSeekProvider tests |
| #65 | Series/season focus restoration after playback | SeriesBrowseFocus tests |
| #66 | Viewport-based page sizes, append path, late-response rejection, retry and disposal | BrowsePageSize, BrowsePagingProfile, LatestPageRequest tests; large real library still needs hardware/server coverage |

## Final checks

- Debug assembly succeeded. **367 app tests + 6 playback-core tests + 2 preference tests passed**, no failures/errors/skips. Other requested shared playback modules had no unit-test sources.
- Android lint completed with **428 errors, 429 warnings and 437 hints**, down from 430 errors after removing the two Android 6 API findings. Lint is configured not to fail the build; successful Gradle execution must not be described as a clean lint result. Most errors concern missing translations/plural forms; remaining categories include inherited Leanback restricted-API shims, Media3 opt-in annotations, notification-permission checks, configuration-observation lint, one localized Live TV format mismatch and a local SDK-path property warning. These are not evidence that every affected locale/device works. See the generated lint report for individual locations.
- Fresh emulator checks on the **test** account: account switch and correct authorized Home content, four settings groups, search focus without automatic keyboard, explicit editing/Done, cache query/destination/Back retention, Help tool placement, sync off/default scope, and grouped search returning Pioneer One. No main-account preferences/history were changed.
- Final APK installed over existing data. Cold launch retained the test account; Demo-Movies and Demo-TV were present. Opened Charge from Demo-Movies, started and paused standard playback with its controls visible, stopped and returned to the same selected Charge library card. Normal test-account playback reporting may update its history; no watch-state reset or subtitle download was performed. The emulator crash buffer contained no entries at the check.

## Boundaries

Do not close #52 or #68 based on this audit. Physical HDR/audio/remote behavior, long-duration stability, cross-device sync on two real TVs, Android 6 runtime, unavailable video editions/audio-language fixtures and large real-library latency remain unverified here. Do not re-download subtitles or reset user history merely to repeat original completed tests. #60 remains deferred; #67 remains a design review with the user. The board offers Pending/In Progress/Done only: #1/#60 remain Pending with explicit deferral in their descriptions; #67 is Pending, #52/#68 are In Progress, and all 63 completed issues are Done.
