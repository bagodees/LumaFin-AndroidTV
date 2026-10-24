# Interface redesign plan

Status: Planned October 23, 2026. Replaces the withdrawn independent v2 rebuild.
No screen has been redesigned yet; versions below are targets, not commitments.

## Goal

Make LumaFin recognizable at a glance as its own app, not a restyled copy of the
official Jellyfin Android TV client, while keeping the playback, sign-in, sync and
device support that already work.

People judge a TV app by its screens. LumaFin's colors, cards, menus and player are
already its own, but the Home and details screens still follow the official app's
structure: a top toolbar over stacked rows, and the same details layout. That
structure is what makes the app read as "the default app with a new theme".

## Approach: replace screens, keep the core

Rebuild each visible screen with LumaFin's own layout, one screen at a time, inside
this app. Write new screens in Jetpack Compose, which the app already uses alongside
the older Leanback screens. Keep, unchanged:

- Server, sign-in and account handling, including per-user settings and sync.
- The playback engine, device profiles, track selection and progress reporting.
- Features users already rely on (listed below). A new screen must keep them.

An independent rebuild is not planned. It would spend months recreating code users
never see, and remove existing features until they were rebuilt.

## Features a new screen must keep

These already exist and are part of what makes LumaFin different. They are not
redesign goals:

- Audio and subtitle choices beside Play, remembered track choices and preference
  scope ([playback preferences](playback-preferences.md)).
- Spoiler protection for unwatched episode descriptions and artwork.
- Home section editor with reordering and the Home card style
  ([Home cards](home-card-appearance.md)).
- Library filters, including movie runtime ([runtime filter](runtime-filter-design.md)).
- Video queue editing, "Ends at" time and the sleep timer ([video queue](video-queue.md)).
- Subtitle downloads, bulk watched actions, quick user switching, text size,
  settings backup and redacted diagnostics.

## Release path

Each new screen ships behind a setting first, so testers can compare and switch
back. Once it holds up on real TVs it becomes the default, and the old screen is
removed in a later release.

| Version | Change |
| --- | --- |
| 1.0.0 | Google Play launch. GitHub and Fire TV releases move to the same version. No redesign. |
| 1.1 | New Home screen available as "Try the new Home" in settings. |
| 1.2 | New Home becomes the default. New settings and action menus. New details screen available as an option. Video/photo filter. |
| 1.3 | "Fits tonight" (below). New first-run setup. New details screen becomes the default. Accent color themes. |
| 1.x | Remaining screens as needed: library browsing, search, series and seasons. Light theme once every main screen supports it. |
| 2.0 | All main screens are new and the old Home and details screens are removed. |

2.0 is the "all-new LumaFin" release for the store listing and announcements.
Ordering after 1.1 can change based on tester feedback.

## Screens in priority order

1. **Home.** Highest impact: the first screen people see and the one in screenshots.
   Choose one navigation structure (for example a labeled side rail or a new top
   bar) and add a large preview area for the focused title: artwork, title, watch
   state and remaining time, without moving focus targets.
2. **Details.** An original layout with clear order: title and watch state, then
   Play/Resume with the audio and subtitle choices, then episodes, cast and related.
3. **Settings.** See [Settings redesign](#settings-redesign).
4. **Action menus.** See [Menus and actions](#menus-and-actions).
5. **First-run setup.** See [First-run setup](#first-run-setup).
6. **Series and seasons.** Keep the selected season, episode progress and nearby
   episodes visible, and return to the same episode after playback.
7. **Library and search.** Lower priority; already partly redesigned.
8. **Player.** Already the most redesigned area; revisit last.

## Settings redesign

Settings feel cluttered: the panel is narrow, and pages mix unrelated controls.
Appearance & Home alone has 16 entries covering Home, libraries, screensaver,
theme, clock, backgrounds, badges, motion, spoilers, accessibility and remote
shortcuts. Everyday choices sit beside rarely used technical ones.

Direction:

- A full-screen layout: categories on the left, the selected category's settings on
  the right, so moving between categories doesn't mean repeated Back presses.
- Smaller, clearer categories, for example Home, Playback, Subtitles, Appearance,
  Accounts and Help. Each page shows common settings first and puts technical
  options under one Advanced entry.
- The current value next to every setting, and a live preview where it helps
  (subtitle style, theme, Home cards).
- Keep settings search, and let it find every setting in its new location.

Settings already use Compose, so no Leanback screens need replacing. Keep every
existing preference key, route and saved value, so updating changes where settings
appear but never what they are set to. Settings backup and sync must keep working.
Because behavior doesn't change, the new settings can ship directly without a
"try it" option. Map every current setting to its new place before building, and
review that map before any code.

## Menus and actions

The player, details, filter and queue menus already use LumaFin's menu design. The
action menu from long-pressing a card does not: Home rows, library grids, item
lists, music, and user and server cards still open the standard Android pop-up
inherited from the official app. Long-press is also undisclosed, so many users
never find actions such as Mark watched, Add to favorites or Go to series.

Direction:

- One LumaFin action menu for every card, in a consistent order: play options,
  then watch state and favorites, then navigation (Go to series, season, artist).
- A one-time hint the first time a card with actions is focused.
- Every card action also available from the details screen, so nothing is long-press only.
- Focus returns to the same card after an action, and the card updates in place.

## First-run setup

New users, and Google Play reviewers, see server connection and sign-in before
anything else, and those screens still follow the official app's flow. Make it one
guided path:

1. Find servers on the network, with manual address entry as the fallback.
2. Sign in, with Quick Connect where the server supports it.
3. Choose a user when the server has several.
4. Land on Home, with a short optional tour of the Home editor and settings.

When a server can't be reached, say why in plain words (wrong address, server
offline, not on the same network) and offer the next step. Keep existing saved
servers and users working without signing in again.

## Signature feature: "Fits tonight"

Pick how much time you have and get a short list of movies or next episodes that
fit, with the reason each was chosen. Build on the existing runtime filter and
library metadata. Label unknown runtimes instead of assuming they fit, and count
remaining time for partly watched items.

This gives people something to talk about other than the app's origin. Check the
idea with a mockup before building it.

## Themes

Requested by testers: theme support, or at least a light theme. LumaFin still carries
the official app's theme picker (Dark, Emerald, Muted Purple), but much of the LumaFin
styling uses fixed colors rather than theme colors, so those choices only partly apply.

Direction:

- Build every new screen from named color tokens (background, surface, text, accent,
  focus, watched) instead of fixed colors, so a theme is a set of token values.
- First themes: LumaFin (default navy, cyan and purple) plus a few accent variations,
  replacing the inherited Emerald and Muted Purple choices.
- Light theme: a full set of tokens with readable text over bright artwork, a visible
  focus outline and dimmed backdrops. Ship it only when every main screen uses tokens;
  a half-light app with dark leftovers is worse than none.
- Themes are per user, like other appearance settings, and included in settings
  backup and sync. High contrast and the stronger focus outline keep working on top.
- Check each theme at TV distance, with large text and on bright and missing artwork.

## Video/photo filter

Requested by testers: filter a library by file type. Mixed libraries (home videos,
folders of photos and clips) show videos and photos together. Add a Type choice to the
library filter menu (All, Videos, Photos) where a library contains both, saved per
library like the other filters. Hide the choice in movie, show and music libraries,
where it would do nothing.

## First step: Home concepts

Before writing code, make two Home concepts as clickable mockups using neutral
sample content. Pick one, optionally after showing testers. Check that each concept:

- Works with only a remote: every action reachable without hidden long presses.
- Returns to the same card and row after details or playback.
- Stays readable with bright or missing artwork, large text and empty libraries.
- Leaves room for the Home section editor and existing Home rows.

## Checks for every new screen

- Focus never jumps or lands somewhere unexpected, including after data loads.
- Back and return navigation restore the previous position.
- Loading, offline, empty and error states are visible and offer Retry where useful.
- Acceptable speed on low-end devices (Fire TV Stick, older Android TV boxes).
- Tested on the emulator and at least one physical TV before becoming the default.

## Out of scope

- An independent rebuild or a separate private codebase.
- A phone or tablet app, which is planned separately.
- Copying another client's screens, artwork or code. Learn from interaction patterns
  only, and keep public comparisons to verified facts.

## Describing LumaFin

During the 1.x releases: "LumaFin, a Jellyfin client for TV with a new interface
built on the official app's playback core." From 2.0: "an all-new interface built on
Jellyfin's proven playback core." Keep crediting jellyfin-androidtv, as the license
and the community expect.
