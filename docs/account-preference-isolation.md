# Account preference isolation (#9)

## Storage and migration

Local Home layout/order, row limits and watched filters, appearance, spoiler controls,
playback quality/autoplay/resume/skip defaults, subtitle styling, photo settings,
screensaver choices, audio behavior/night mode and segment actions use a file keyed
by Jellyfin server UUID plus user UUID. Signed-out reads/writes use a separate guest
file. No credentials, server URLs or content lists are copied into these files.

After upgrading the legacy preference schema, the first authenticated server/user
claims the existing local preferences once. That identity is recorded before copying,
and an account migration marker is committed with the copied values. Other users
start from built-in defaults. The legacy values remain as a backup, not a fallback
for new accounts. Existing Watch Later, shortcuts and hidden recommendations retain
their own server/user keys.

Device capability and compatibility options remain global: HDR, codec levels,
bitstream support, software/FFmpeg decoders, PGS/ASS compatibility, refresh matching,
display-start delay, buffering and developer playback/debug switches. Authentication,
automatic sign-in and telemetry stores remain device-level. The new user storage
does not wrap those stores.

## Session and cached state

- Compare both server and user IDs when deciding whether a session is unchanged.
- Clear current published identities during transition; select the correct local
  storage before publishing the incoming authenticated user.
- Clear video queue/language carry-over, audio queue and cached library/Live TV
  display preferences on session changes/sign-out.
- Invalidate pending display preference results with a generation plus captured
  server/token identity, and reject commits for invalidated stores.
- Local SharedPreferences editors capture their original file. A delayed apply
  cannot write into the account selected afterward.
- Rebind preference observers and reset remembered settings/Home editor state when
  the active account changes. Segment choices are reloaded rather than retained
  in the singleton repository across accounts.
- Content grids/runtime plans remain view-owned. The normal startup/Main navigation
  reset recreates them after user selection; verify that behavior on the real accounts.
- Shared artwork disk caching is retained; it is a download cache, not a substitute
  for the server-authorized library queries. Jellyfin stays authoritative for item
  access, watched history and progress.

## Automated validation

Account tests cover first-account-only migration, independent Home/playback values,
same user UUID on different servers, process/store recreation, guest separation,
late editor apply, shared hardware settings, credential exclusion and listener
rebinding. Segment tests cover refreshing actions after preference identity changes.
Existing Watch Later, shortcut and settings-search regression suites are also run.

## Manual acceptance still required

Use the two existing users with their different libraries. Shared library access
is unnecessary for the first check.

1. First user after update: confirm the existing Home layout and playback choices
   survive migration. Record the original settings before changing anything.
2. Give that user a recognizable Home row limit, subtitle brightness and quality
   preset. Switch via the existing user picker; no need to sign out/delete users.
3. Other user should show their authorized libraries and fresh local defaults on
   first use, with none of the first user's Home/queue/shortcut content.
4. Give the second user different values. Switch back and forth and reopen the app;
   each account should retain its own choices and permitted libraries.
5. Check Watch Later, shortcuts, hidden recommendations and segment-skip choices;
   change only reversible settings and restore any temporary test changes afterward.

User validation completed: the first account retained a 20-item limit and 60%
subtitle brightness; the second independently retained 30 items and 80%. The user
confirmed the checks were complete and authorized completion of #9. Startup after
migration also passed in the emulator. The build and all 21 account/shortcut/Watch
Later/segment/search tests passed. No live second server was available for manual
testing; server isolation is covered by automated tests.
