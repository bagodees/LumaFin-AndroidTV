# UI recovery acceptance — 2026-10-20

Debug build installed on emulator-5554. The user confirmed delete cancellation, biography scrolling/OK/Back, compact empty queue/final-item Close focus, detail header visibility, and restored Home backdrops during this UI pass.

Authorized emulator recovery checks used only the test account, with app-only networking denial. Home exhausted its bounded retries and displayed the modern recovery panel; Cancel dismissed it, and restoring networking then Retry loaded Continue Watching/cards without leaving Home. The movie library displayed its modern page-failure panel; both Cancel and Back dismissed it. Restoring networking then Retry loaded Agent 327 and the movie grid. The library-selection screen also recovered through Retry.

Original networking restored: FIREWALL_CHAIN_OEM_DENY_3 disabled; io.github.bagodees.lumafin.debug allowed. Returned to the original account after testing. No playback, deletion or watch-history mutation was performed. No automated tests were run in this pass; debug assembly succeeded. Physical-TV acceptance remains open in #68, including the original intermittent startup report #69.

## Physical-TV startup follow-up

The user reported Home showing only the toolbar and server background for at least
one minute. Leaving and returning loaded the rows. This confirms #69 remains open.
Home now observes session readiness: initial loading waits until session restoration
finishes, and a ready account starts a replacement load after invalidating stale work.
Previously a session change could silently cancel startup without scheduling a new load.
This closes a code-level gap; the photograph alone does not establish the TV's cause.
Manual acceptance: after installing this build, fully close and reopen LumaFin several
times, then check return from TV sleep and switching accounts. Rows should appear without
leaving Home. Physical-TV confirmation is still pending; no new UI or automated tests
were run for this follow-up.

## Physical-TV UI checks reported before the pause

The user confirmed Home/details row navigation, details header hide/return, Audio
panel sizing and Back/focus restoration, long subtitle-list scrolling, biography
readability/scrolling/return focus, delete cancellation, playback-speed menu
navigation/dismissal, controls/gradient dismissal, resume, restart at 0:00, seeking
and sync, subtitle on/off, Home backdrop restoration, Hide watched appearance and
filtering, Search results/detail return focus, no-results recovery, Settings
readability/navigation, account-picker navigation, and Large-text account-picker
layout. Multiple audio-track switching was unavailable. Strong Focus (check 23)
has not been answered. These reports cover the build on the TV at that time, not
subsequent fixes or broader hardware/codec acceptance.

## Gradient corrections — 2026-10-21 (#71, #72)

- Search's root Column now fills the viewport even with no result rows, keeping
  its background gradient full-screen during typing, loading and empty states.
- Playback uses Leanback's full-screen animated background for the gradient,
  rather than painting only the transport row. Its normal hide/show fade now owns
  the whole gradient, including the bottom edge. Removed the decorative 160dp
  top padding from the controls row.
- Playback grid/row bottom padding is reduced from 48dp combined to 28dp, moving
  controls and their focus labels 20dp lower while retaining bottom clearance.
- Debug assembly and 12 existing SearchState/SearchViewModel/DirectionalSeek
  regression tests passed. The build was installed on emulator-5554. Its graphics
  hang required a cold start with software graphics; after restarting, saved-server
  connections timed out and test-user login could not complete. Actual Search and
  playback visual checks are blocked by that connection, not recorded as passed.
  Physical-TV confirmation of both gradient fixes and the lower labels is pending.
