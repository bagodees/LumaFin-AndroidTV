# Quick user switching (#21)

Status: complete. User accepted account switching, restart behavior and the
full-screen picker. Server-selection Back regression is fixed and verified.

The home toolbar profile button opens a full-screen "Who's watching?" picker for the
current server. The active user is first and labelled Current user. Selecting
that user, Cancel, or Back closes the picker without ending the session.
The same profile screen is used after selecting a server, including startup,
account selection and connection recovery. Add User opens sign-in for the chosen
server directly. Long-press user actions retain saved-user sign-out/removal.
Cards show Jellyfin pictures or stable colored initials, with names beneath and a
white border and slight enlargement on focus. The row scrolls for larger lists.
Saved users can switch directly when a usable token is available and Always
authenticate is off. Add User opens the existing login flow; Servers opens
server selection directly. Both run above Home without ending the active session;
Back cancels selection and returns to the profile picker. Selecting the already
active user returns without authenticating again. A different authenticated
account clears the old activity task before opening its new Home. Multiple-server management
remains separate work in #22.

## Handoff and account isolation

- Validate a candidate token and its user ID with a separate SDK API client before
  changing the shared API, selected preferences, last-active identity, or queues.
- Serialize session switches with restoration. A failed preflight leaves the
  original account untouched and returns the session state to Ready.
- Once validated, complete the handoff without coroutine cancellation: clear
  playback queues and display caches, bind the account preferences from #9,
  publish the new identity, then refresh display preferences.
- A display-preference refresh failure does not undo a validated login.
- Prevent overlapping picker attempts and repeated startup launches. Show a busy
  message during switching; restore picker focus on failure.
- Finish the old MainActivity immediately and relaunch through StartupActivity.
  Startup resets navigation and refreshes the home, workers and WebSocket session.
  The old activity and its view models cannot remain in the account's back stack.
- Reuse #9's account/server-scoped preferences and display-store generation and
  identity checks, which reject stale results and commits from the old account.

## Checks completed

- Debug build succeeds.
- 6 picker-controller tests: current user is a no-op; missing token and Always
  authenticate require sign-in; success; failure and retry; unexpected failure;
  overlapping clicks cannot start another switch.
- 2 session tests: timeout preserves user, session, API binding, preferences and
  queues, followed by successful retry even if display refresh fails; a token
  resolving to a different identity is rejected.
- 5 existing account-preference isolation tests pass.
- Emulator: profile button opens the picker, active user is focused first, both
  saved users and sign-in/cancel entries fit, Back preserves the home and restores
  focus to the profile button.
- Server-selection regression: opening Servers and pressing Back returns to the
  full-screen profile picker with Servers focused. Another Back returns to Home
  with the same active account. Updated build and all 8 switch/session tests pass.
- Shared-screen follow-up: choosing Jarvis from Servers opens the same square
  profile cards, title, current-user marker and Add User tile. Initial profile
  focus and Back to server selection were visually verified on the emulator.
  Add User opens login directly; its Cancel handles root and nested navigation.
  User-list loads cancel older requests and reject stale generations on switching
  servers. Build and 18 switch/authentication/recovery tests pass.

## Accepted manual checks

1. Open the profile button on Home and switch to the other saved user. Confirm
   their libraries and home rows appear, with no rows belonging only to the first
   user. Open the picker again and confirm Current user moved to that user.
2. Switch back and confirm the first user's libraries and saved values return.
   Existing test values were 20 / 60% for one user and 30 / 80% for the other;
   use whatever values are currently saved. No need to change them again.
3. Press Back on Home after a switch. Confirm it does not reveal the previous
   user's page. Close and reopen the app and confirm the intended automatic
   sign-in preference is still respected.

The no-token/Always authenticate branches have unit coverage. These users receive
a sign-in message and can use Other users without ending the existing account
merely by opening or canceling the selection screen.

The user confirmed switching and exiting/restarting work. The full-screen layout
has been inspected on the emulator with both saved users, initials, names and the
current-user marker, Other users, Servers and Cancel visible. Servers → Back
returns to the picker, another Back returns Home, and force-stop/relaunch restores
the active account. User authorized completion. No repeated video-playback testing
was needed.
