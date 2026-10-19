# Remote shortcuts

Settings → Appearance & Home → Remote shortcuts controls holding OK on movie,
episode and series cards. The setting is saved separately for each server/user
and is included in settings backups.

- Actions menu (default): opens the existing contextual menu.
- Details: opens the title's details.
- Play or resume: uses the existing Play action; series show playback choices.
- No action: consumes the hold and release without opening the title.

Normal OK presses and the remote's Menu button retain their existing behavior.
Libraries, music, queue controls and unavailable pinned titles retain their
specialized menus. Cards from an earlier account revision cannot activate a
shortcut after switching accounts.

Validation: debug build and 300 unit tests passed, including account/server
isolation, persistence, backup compatibility and card-type fallback coverage.
Emulator checks covered the actions menu, details, playback, disabled holds,
normal OK presses, the Menu key and persistence across an app restart.
Physical remote timing remains a later physical-TV check.
