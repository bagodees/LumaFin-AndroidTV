# Connection recovery (#25)

Status: recovery foundation implemented and installed for manual acceptance.
Issue remains open. Optional verified home/remote address fallback depends on
#22 and has not been implemented as part of this change.

## Behavior

- A shared classifier distinguishes unreachable/timeout/DNS, expired or rejected
  sign-in (401), denied access (403), incorrect endpoints (404/410), HTTPS trust
  failures, temporary server/proxy faults (5xx), rate limiting (429), and unknown
  responses. Messages are fixed resources; raw exception text, URLs, headers,
  credentials and tokens are never used in the recovery UI.
- Startup restoration, saved-user login, credential login and Quick Connect use
  persistent recovery dialogs instead of treating every failure as unavailable.
  Retry is offered only for retryable categories; rejected sign-in offers Review
  sign-in. Switch server and Cancel remain available. Duplicate dialogs are
  suppressed and view destruction dismisses them.
- The full-screen quick-user picker explains failures and retains the existing
  account. Retry repeats only the selected attempt and runs only on a button
  press. Permission/certificate/login problems do not offer repeated retries.
- Session preflight preserves the exception category and the exact attempted
  server/user identity without changing the active account. Startup recovery
  uses that identity, including when automatic sign-in targets a specific user.
- A successful session uses its already-validated user; it does not make another
  authentication request that could fail after the account has changed.
- Credential submits are guarded against overlap. Leaving the credentials screen
  cancels its pending request. Leaving Quick Connect stops polling and invalidates
  its previous generation. Changing server or leaving login clears pending work.
- No automatic recovery retry loop, account deletion, settings reset, insecure
  certificate bypass, address guessing, or automatic diagnostic upload is added.

## Validation

- Debug assembly succeeds.
- 23 focused tests pass: 6 failure-classifier tests; 2 authentication recovery
  tests; 8 picker/retry tests; 2 session isolation/recovery tests; 5 existing
  account-preference isolation tests.
- Classifier tests cover wrapped DNS/TLS exceptions, HTTP categories, generic
  messages independent of sensitive exception text, and cyclic cause chains.
- Retry tests prove one failed attempt does not automatically make another
  request, explicit Retry can succeed, and 401/403 cannot repeatedly submit the
  same stored login via Retry.
- Emulator startup was tested at 1 bit/s with 40-second injected latency. The
  persistent "Can't reach the server" dialog appeared, with Retry initially
  focused and Switch server/Cancel visible. Saved users remained present.
- The emulator was restored to unlimited speed and zero latency. Installing the
  final build and restarting returned to the original user's Home successfully.

## Manual acceptance and remaining scope

Pending: exercise the visible Retry and Switch server actions, check the
quick-user failure presentation, and confirm leaving credentials/Quick Connect
does not continue an unwanted request. Real permission and certificate rejection
UI paths have classifier coverage but have not been induced against the user's
working server; do not invalidate credentials or certificates to test them.

This foundation covers startup and account selection/sign-in. Existing playback
and individual library-row failure handling is unchanged. Verified same-server
address fallback remains #22; speed tests and diagnostic export remain #53/#26.
No repeat of the already accepted video-playback or preference-value tests is
required for this change.
