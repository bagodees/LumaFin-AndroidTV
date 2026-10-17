# Verified server addresses (#22)

Status: complete. Verified backup editor and explicit recovery are implemented.
Saving a backup alone does not change the active address. Once the user
has selected backup recovery, startup tries the primary first and reuses the
verified backup only if the primary is still unreachable.

## Implemented foundation

- Saved-server records have an optional backup address, defaulting to null so old
  installations load without migration or losing users/tokens. Positional version
  arguments retain their previous meaning.
- Accounts & Servers → saved server → Backup server address provides explicit
  Verify and save, Cancel, and Remove backup address controls. A failed check
  leaves the entered address available to edit. Dismissal cancels verification.
- Require an explicit HTTP(S) address. Reject embedded credentials, query strings,
  fragments, missing hosts, and invalid ports. Preserve reverse-proxy paths and
  encoded path components; normalize scheme/host casing and trailing slashes.
- Probe public system information with a fresh unauthenticated SDK client. Never
  use the shared authenticated API or send a saved login token to an unverified
  backup. The reported server ID must match the saved Jellyfin server ID.
- A missing/different identity or unreachable server cannot replace the saved
  address. Re-read the store after probing to preserve concurrent account changes
  and avoid recreating a server removed during verification.
- Keep the same server UUID and users, preserving the account-preference scope
  from #9. Do not create a second independent server/library for a second address.
- A separate verification operation probes the saved backup again rather than
  trusting an old successful check. Recovery integration must call this before
  sending credentials to that address.

## Checks

Debug build and 28 focused tests pass, covering address validation, backup
recovery, session switching, account switching and primary return.

The user supplied an alternate public URL for Jarvis. Unauthenticated public-info
requests to the saved address and supplied alternate returned the same server ID.
Initial identity probes used no credentials. The recovery test subsequently
validated the saved user at the verified alternate. The configured primary stays
unchanged.

The TV keyboard Done action now hides the keyboard and focuses Verify and save,
preserving the entered address. Verification failures keep their message visible.
Manually checked Done followed by Verify and save against the supplied alternate;
the saved address remained after reinstall/relaunch. Server equality includes the
backup address so settings state publishes changes immediately; a focused state
flow regression test covers adding and clearing it (nine address tests pass).

## Recovery behavior and acceptance

- Manual recovery now offers Try backup address on reachability failures in the
  startup profile screen and home account switcher. Public identity is checked
  before saved-user authentication. Only successful user validation commits the
  runtime address. Configured primary and backup roles stay fixed; a separate
  using_backup flag remembers the user's recovery choice. Cancellation/failure leaves
  the current account, address, queues and preferences intact. Concurrent address
  edits or credential removal prevent committing stale results. Slow metadata
  refreshes preserve the latest addresses and users.
- 28 focused tests pass: nine backup recovery, nine address validation, two
  session switching and eight account switcher checks. Recovery tests include an
  already active account, both unreachable, wrong server/user, cancellation during
  identity and authentication, authentication timeout and concurrent address edits.
- Emulator check: temporarily simulated an outage until startup showed Can't reach
  the server with Try backup address. Restored the emulator network, selected that
  action, and reached Home using the supplied alternate. The actual primary server
  was not disrupted; this tests recovery after a failed primary attempt rather
  than a sustained outage isolated to one hostname.
  Force-stop/relaunch also returned to Home successfully after recovery.
- Primary return: on the next launch, validate the primary first. If it succeeds,
  clear using_backup; if it remains unreachable and backup recovery was previously
  selected, reverify and reuse the backup. No background address switch during
  playback, authentication/permission/certificate bypass, or cycling retries.
  Emulator test restored the original primary/backup roles with using_backup set,
  launched successfully, and confirmed using_backup cleared after primary login.
- Failure cases and state preservation are covered by focused tests; emulator
  acceptance covers keyboard save, recovery, restart and primary return. No
  working server certificate or credentials were altered for these tests.
