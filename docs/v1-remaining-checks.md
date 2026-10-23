# Remaining v1 verification

Audit: October 23, 2026. This is a short follow-up to
[the detailed acceptance record](ui-recovery-acceptance.md), not a replacement
for the broader hardware checklist in issue #68.

## Local verification today

- Debug assembly succeeded on the current checkout.
- Fresh execution of all 374 app unit tests: zero failures, errors or skipped tests.
- Emulator-5554 is available. Reopening initially showed an empty restored details
  screen; a force-stop/cold launch displayed the persistent “Can't reach the server”
  recovery dialog with backup address, switch server and retry actions. No new
  application bug is established by the initial screen. Server-dependent UI checks
  remain blocked pending connectivity; no settings, media or watch history were changed.
- No additional code fix was needed or made during this audit. Build success and unit
  tests do not confirm physical-TV lifecycle, audio or display behavior.

## Priority physical-TV retests

Record the installed version/build and device/OS before starting. Use the latest test
release containing the October 22 fixes. Restore temporary settings afterward.

| Check | Steps and expected result | Tracking |
| --- | --- | --- |
| Home after sleep/wake | Start on populated Home, sleep and wake the TV, reopen if needed. Repeat three times and after longer idle when practical. Rows load and remote input works without backing out. | #69: fix implemented; physical confirmation pending |
| Text-size reload | Change Default to 130%, navigate immediately, then restore Default. Repeat after opening details and returning. No frozen reload, lost navigation or empty Home. | #73: fix implemented; physical confirmation pending |
| Subtitle first row | Open subtitle appearance, move down then back to Brightness. The complete first-row focus outline stays visible below the preview; remaining settings stay reachable. | #74: fix implemented; physical confirmation pending |
| Remote Home during playback | Start disposable test media, press the remote Home key, reopen LumaFin and resume. Repeat three times. No crash; app responds and resume is near the prior position. If it recurs, record exact time/media/build and retrieve the server crash report with secrets redacted. | #75: lifecycle hardening implemented; original crash stack still uncollected |
| Details Up navigation | On an episode, press Up separately from Resume, Play, Restart and other action buttons. Home receives focus. Audio also goes Up to Home; Subtitles goes Up to Audio. Check Down/Back remains usable. | October 22 navigation fix; emulator passed, TV pending |
| Play-installed update notice | Open the copy installed through Google Play. No GitHub/website update-available banner should appear. This tests installer detection, not whether Play automatic updates are enabled. | Store-specific acceptance pending |

Stop and investigate a repeated crash or unresponsive screen rather than continuing
through the checklist. Keep these issues open until the affected TV confirms the fix.

## Hardware-only follow-ups, after the fixes

- Resume the paused surround-audio check: dialogue/channel mapping, dropouts and sync;
  note receiver output mode if available. Multiple audio tracks remain unavailable
  unless suitable media is supplied; do not count this as a pass.
- Sleep timer expiry and end-of-episode stop, not just enabling/canceling the timer.
- Representative HDR/output and passthrough formats supported by the actual hardware.
- Fire TV device acceptance before Amazon submission; emulator success is insufficient.

## Already accepted; avoid repeating without a regression

The user confirmed both playback and Search full-screen gradients on physical TV
on 0.2.1, including keyboard/no-results states and readable lowered player labels.
Issues #71/#72 remain open in GitHub at audit time, but the acceptance record contains
physical passes. These are tracking cleanup, not currently reported unfixed gradients.

Other accepted interactions are recorded in the detailed log. Broader issue #68 still
includes untested features; this document does not declare the whole app release-ready.

## Next emulator session when the server is reachable

Retry connection, confirm populated Home, then repeat startup/background return,
130% recreation and subtitle row positioning on the current debug build. Inspect
details Up navigation without changing watch state. Use a test account for any playback
or preference changes and restore its original choices. Record fresh outcomes separately
from the October 22 passes; do not relabel old results as today's verification.

## Emulator follow-up — October 23

Connectivity recovered after an Android reboot and reconnecting emulator Wi-Fi.
Android subsequently reported Ethernet as the validated default connection; the
temporary Ethernet warning was not a persistent application failure. User confirmed
Home loaded. This does not establish the original connection failure's root cause.

Fresh checks on the installed debug build:

- Home remained populated after sleep/wake; D-pad input and opening the account
  picker worked. Switched from cocarte to the saved test account for further checks.
- Default to Larger (130%) reloaded successfully. Playback settings and subtitle
  appearance remained navigable. Restoring Default also reloaded successfully.
- At 130%, Subtitle Brightness's whole focus outline was visible below the pinned
  preview after Down/Up. Screenshot visually inspected. Existing brightness of 80%
  was unchanged.
- Up from Resume, Play, Restart, Watched, Favorite, Previous episode and More each
  focused the header Home button. No watched/favorite action was activated.
- Pioneer One test-account playback was confirmed by screenshot. Pressing remote
  Home and reopening returned populated Home with the same app process ID, with no
  observed crash. This is one visually confirmed playback/Home cycle, not proof
  that the intermittent physical-TV crash is resolved. A prior cycle was not
  visually confirmed and is not counted as playback acceptance.
- Test-account watch progress advanced during playback. Original account restored
  afterward; its watch history was not intentionally changed.

These are emulator passes only. The physical-TV retests above remain outstanding;
no new application source changes were required in this pass.
