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
