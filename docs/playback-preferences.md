# Remembered playback choices (#3)

Audio, subtitle and version choices are remembered automatically. There is no
new setting to turn on and no new player button.

## What is remembered and where

- Choosing a track in the details Audio/Subtitles chips, or in the player's
  audio/subtitle menus, saves it. Choosing a version on the details screen
  saves the version. Server defaults, automatic selection and remote control
  from another Jellyfin client are not recorded.
- Episodes save to their series by default; movies save to the movie.
- Once something is remembered, the same menus show **Remembered for: …**.
  It opens *Remember these choices for* with This series / This season /
  This episode only (episodes) and **Forget**. Choosing a level moves what
  currently applies to that level only, so "This episode only" stops the rest
  of the series using it. Forget removes the title, season and series entries.
- **Settings → Playback → Clear remembered track choices** forgets everything
  for the signed-in account after confirmation.

Choices are stored locally per Jellyfin server and user, beside Watch Later,
and contain only content IDs and track descriptions. They are not sent to the
server, not part of settings backup, and capped at the 1000 most recently
changed titles; older entries fall back to normal defaults.

## Matching

Track numbers differ between episodes, so a choice is stored by type:

- **Audio:** language (ISO 639 two-letter, bibliographic and terminology codes
  are treated as equal), whether it is a commentary, and channel count. A
  commentary never stands in for the main track or vice versa. Same channel
  count, then the default flag, break ties. Unknown-language audio is not
  remembered.
- **Subtitles:** off, or language plus forced and hearing-impaired flags.
  Forced-only and full subtitles are never swapped; hearing-impaired and
  regular subtitles may stand in for each other. Off is always honored.
- **Version:** source name, then video height. Only remembered for titles with
  more than one version; the details screen opens on the remembered version so
  its chips describe what will play.

## Precedence

Per field, most specific first:

1. Forced subtitles from a playback fallback (e.g. subtitle burn-in failure).
2. A track picked on the details screen for this play.
3. Remembered: this episode/movie, then season, then series.
4. Existing behavior: language carried over within the play session, then the
   server's default and Jellyfin user language preferences.

A remembered level whose choice has no match on this title falls through to the
next level, and finally to step 4. The app never picks an unrelated track to
satisfy a remembered choice.

## Resume

Resume behavior is unchanged: resume position still comes from Jellyfin, and a
resumed title uses the same track precedence above. No resume preference was
added.

## Validation

Automated tests cover language normalization, reordered tracks, commentary and
forced/SDH rules, channel and default tie-breaks, version name/height matching,
per-field fall-through, title keys shared across versions, series default
scope, movie isolation, per-user isolation and signed-out writes, moving
between levels, Forget, Clear all, single-version exclusion and the entry cap.

Emulator (Jellyfin 12.2.0): choosing English subtitles on Pioneer One S1:E2
showed *Remembered for: This series*; S1:E4's chip then showed English and its
playback started with subtitles on. Moving to *This episode only* and Forget
updated the player menu as expected. Audio language and version matching were
not exercised live because the test library has single-language,
single-version titles.
