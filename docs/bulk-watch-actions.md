# Multiple-title watched actions (#61)

Hold OK on a library item and choose **Mark selected titles…**. Series/season details also expose this action when at least two individual episodes are loaded. Existing recursive series/season actions remain separate.

The chooser contains only currently loaded movies and episodes, with no initial selection. Review is disabled until a title is checked. Groups are limited to 100 items and include the focused item; the scope text describes this limit. The confirmation names the selected titles, account and server and explains watch-history, resume and Home effects.

Requests use the captured server/session and explicit user ID. Each target is freshly read and checked before writing. Completed requests stay saved; stopping prevents further requests after the current request. Read failures are reported and three consecutive read failures stop the batch. An uncertain write stops the batch without retrying. Results distinguish changed, already in state, failed checks, unconfirmed writes and unattempted titles. Potential writes invalidate Home data and refresh the originating view when it is still current.

Marking unwatched clears Jellyfin play count, last-played date and resume position. Marking watched updates history and clears resume position. Already-matching clean states are skipped to avoid needless history updates. Server behavior reference: [Jellyfin BaseItem](https://github.com/jellyfin/jellyfin/blob/v10.10.7/MediaBrowser.Controller/Entities/BaseItem.cs).

## Validation (2026-10-19)

- Debug assembly and 363 unit tests passed, including ten new tests covering deduplication, state checks, read failures, uncertain writes, stopping, account changes, invalid targets, bounds and cancellation.
- Emulator, test account on Jarvis: empty initial selection, disabled Review, exact-title/account/server confirmation, cancellation, two-title watched and unwatched writes, result counts and refreshed watched badges passed. The two test titles were returned to unwatched.
- Series details: loaded episode candidates exclude the season; remote OK toggles the focused checkbox and updates Review. Cancelled without applying episode changes.
- Physical remote/layout checks, larger collections and real connection-loss behavior remain follow-up coverage in #68. Interrupted-write and account-switch guards were tested with controlled unit tests rather than disrupting the live server.
