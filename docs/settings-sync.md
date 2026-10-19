# Optional settings sync (#27)

Status: implemented (`SettingsSync`, `SettingsSyncModel`, Settings → Help & Maintenance → Sync settings across devices).

## Decisions

- **Opt-in** per TV and per signed-in account. Off by default; turning it off
  keeps local values.
- **Personal preferences sync; layout stays per TV.**
- **Conflicts are asked about once**, listing only the conflicting settings.
- **Settings only.** Watch Later, Home shortcuts and remembered track choices
  (#3) are not synced.

## Storage

Jellyfin per-user display preferences (`/DisplayPreferences/{id}`), which the app
already uses for library display settings:

- id `lumafin-settings-sync` (the server maps non-GUID IDs to an MD5 item ID),
  client `lumafin-androidtv` (server limit: 32 characters).
- One custom-preference key, `lumafinSettingsSync`, holding a JSON envelope. The
  server rewrites some keys (`homesection*`, `landing-*`, `tvhome`,
  `skipForwardLength`, `skipBackLength`, `chromecastVersion`,
  `enableNextVideoInfoOverlay`, `dashboardTheme`), so settings never use those
  keys directly; skip lengths travel inside the envelope.
- Visible only to the same Jellyfin user (server administrators can read the
  database). No plugin or server setting is needed.
- A save replaces the record's whole key set and the server has no versioning:
  last write wins. All conflict handling is client-side (below).

Envelope:

```json
{ "format": "lumafin-settings-sync", "version": 1, "revision": 7,
  "writer": "<random per-install id>",
  "settings": { "<key>": <value>, ... } }
```

Values reuse the settings-backup (#7) field definitions and validation: same
keys, types, ranges and size/nesting limits (the backup's limit check is shared). Unknown keys are ignored; a
malformed known value rejects the whole remote copy (local settings untouched).
No credentials, usernames, server addresses or device IDs beyond the random
writer id.

## Synced vs per-TV

Synced (17): spoiler controls (episode artwork, descriptions), hide-watched
filters (Latest, Recently added), watched indicator, rating badges, series
thumbnails, remote long-press action, autoplay next episode, cinema prerolls,
skip back/forward lengths, subtitle brightness, weight and text/background/stroke
colors.

Per TV (12): Home row order/visibility and the four Home item limits, card
size, theme, backdrop, clock, Reduced Motion, subtitle size and position.

Never synced: everything the backup excludes (credentials, servers, device and
decoder capability overrides, player choice, diagnostics, history).

## Sync and conflicts

Each TV keeps, in its account preference file, the last synced revision and the
values it last agreed with the server (the *base*).

1. **When:** whenever the main screen resumes (including after sign-in or an
   account switch), from Sync now, and 3 seconds after a synced setting changes. Offline or failed requests retry later and
   never block the UI.
2. **Merge** each synced field three ways (base, local, remote):
   - unchanged on both → nothing;
   - changed on one side → take that side;
   - changed on both to the same value → take it;
   - changed on both to different values → **conflict**.
3. **Conflicts:** everything else is merged and uploaded first. A short review
   then lists only the conflicting settings with "This TV" and "Synced" values;
   choose Keep this TV's or Use synced for the set. It is shown once on the main
   screen (never during playback) and stays reachable from the sync screen if
   dismissed. Until answered, local values stay and the server keeps its value.
4. **Write:** re-read the remote revision immediately before saving; if it moved,
   merge again instead of overwriting. Save with `revision + 1`. The remaining
   race window (two TVs saving within the same request) resolves to last write
   wins and is caught as an ordinary change on the next sync.

**Turning sync on** when a synced copy already exists shows the differences and
asks: Use synced settings / Replace synced with this TV's / Cancel. With no
remote copy, this TV's values are uploaded.

**Turning sync off** leaves local values and the server copy alone; a separate
"Remove synced copy from server" action deletes the key for the account.

Accounts are isolated as in #9: enablement, base and revision live in the
account preference file, requests use the current account's API client, and a
pending sync or review is discarded if the account changes.

## UI (existing menus only)

Settings → Help & Maintenance → *Sync settings across devices*: on/off,
last-synced time, Sync now, Remove synced copy from server, and a description of
what does and does not sync. Searchable in settings search.

## Validation

Unit (14 tests): the 17/12 split, every merge case, both resolutions, envelope
format/version/type/range/nesting checks and unknown-key tolerance; enable with
no copy, with a differing copy (no change until answered; Use synced and
Replace), two TVs merging different settings without asking, a real conflict
reviewed while other changes still sync, re-merge when the copy changes between
read and upload, account change mid-sync, malformed copy (no local or server
change), disable keeping both copies, and Remove. Settings backup tests still
pass with the shared helpers.

Emulator (Jellyfin 12.2.0, one TV): enabling uploaded the copy; after turning
sync off and changing rating badges, re-enabling found the server copy and
offered "Show rating badges — This TV: Off · Synced: On"; Use synced restored
On. With sync on, each change uploaded once after the delay. Remove deleted the
copy (re-enabling uploaded without a review) and left sync off; the account's
copy was removed again at the end of the check.

Not checked live: two physical TVs on the same account, which is required to
see a real cross-device merge and conflict prompt. Added to #68.
