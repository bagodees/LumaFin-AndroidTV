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

### Emulator follow-up after server recovery

The user restored the server after nginx was down. Retrying the connection
succeeded, then all playback checks used the saved test account. Search's empty
results with keyboard open and populated results with keyboard closed both showed
continuous full-screen coverage. Playback of Pioneer One S1:E4 showed the gradient
reaching the bottom edge with no uncovered strip, and the focused Skip forward
label remained fully visible below the lower controls. During normal playback the
controls timed out and the entire gradient disappeared, leaving unobstructed video.
Playback was stopped and the original cocarte account was restored; its playback
history was not changed. No additional code change or APK rebuild was necessary.
Both issues remain open for physical-TV confirmation. Screenshots are in ignored
build outputs: search-gradient-empty.png, search-gradient-results.png,
player-gradient-label.png and player-auto-hidden.png.

### Physical-TV confirmation on 0.2.1 — 2026-10-21

The user confirmed playback's gradient reaches the bottom without an uncovered
strip, and the lowered controls and Playback Information focus label remain
comfortably visible. The user also confirmed Search's dark background covers the
whole screen during typing and no-results checks with the keyboard open.
Physical-TV acceptance of the gradient fixes in #71 and #72 passed.
The user also confirmed Stronger focus outline (check 23): selected Home cards and
buttons have a clear, thick white outline visible from the normal seating position.
High contrast also passed: card titles and secondary text were readable with
solid title backgrounds and no reported clipping.

Changing Text size to Larger (130%) stalled the app on the physical TV. The user
had to back out and reopen; 130% was then applied. Switching back to Default
reloaded normally. This is a failed accessibility transition check, not a passed
130% layout check. Leave text at Default until fixed and retested.

The user subsequently confirmed Home loaded its rows normally on both opens
without backing out and returning. This is a two-open startup pass on 0.2.1;
sleep/wake and account-switch startup coverage remain outstanding for #69.

Sleep/wake follow-up on the same physical TV and 0.2.1 failed: after sleeping the
TV from LumaFin Home and waking/returning, the toolbar and server backdrop were
visible but Home rows were absent. User photograph confirms the blank content
area. #69 remains unresolved despite the two normal opens passing. Delay duration,
navigation recovery, and account-switch behavior had not yet been established
when the photo was supplied.

The user clarified that nothing responds in this wake state; switching tabs is
not possible. Backing out and returning restores movement/responsiveness. Record
this as an unresponsive app after sleep/wake, rather than only absent Home rows.
The cause is unconfirmed; no ANR or device logs have been captured.

After recovering responsiveness, switching to another saved Jellyfin user passed:
Home loaded and responded normally. Switching back to the original user also
passed, with that user's rows and watch progress visible. Sleep/wake remains failed.

Held Right/Down navigation stopped on release without opening an item. Home row
reordering passed: the moving row was clearly highlighted, normal focus returned
after dropping it, and the chosen position persisted after closing/reopening.

Subtitle appearance settings failed the preview/first-row spacing check on 0.2.1:
the user reported the first setting cut off at the top. Photo shows Subtitle
Brightness focused beneath the pinned preview, with the upper focus outline
clipped. Its title, description and slider are visible. Whether Up can reveal the
complete row and whether navigation becomes trapped remain unconfirmed.
