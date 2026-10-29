# Movie runtime filtering

Tracking: [#20](https://github.com/bagodees/LumaFin-AndroidTV/issues/20).

Status: implemented and validated. Runtime filtering is integrated into the movie
filter dialog, with complete metadata scans, staged Apply, bounded result pages,
saved selections and Retry/Cancel handling. Validation and test limits are
recorded below; the early prototype measurement is retained as historical evidence.

## User experience

Offer Any, Under 30 minutes, Under 60 minutes, Under 90 minutes and Under 2 hours
in the existing movie filter menu. A selected duration means the full movie
runtime is strictly below that duration; it does not mean remaining playback time.
Exclude unknown, zero and invalid runtimes from a limited-duration result.

Combine runtime with genre, year, minimum rating, resolution, Favorites,
Unwatched and letter selection. Retain the chosen library sort order. Do not
apply movie runtime filtering to a TV series or sum its episode durations.

Applying a runtime limit shows "Checking movie lengths: 500 of 2,400" with Cancel.
The count is movies checked, not matches found. Keep the previous grid visible
behind this progress view. Do not replace it with incomplete results or a
temporary zero-match message. On success show the complete matching count and
the active duration in the existing filter summary. On failure retain the
previous applied filters/results and offer Retry or Cancel.

Stage edits in the filter dialog and use an Apply Filters action so selecting
several dimensions starts one check. Cancel discards staged edits. Existing
toolbar shortcuts can remain immediate, but changing one while runtime is active
must follow the same cancellable check. Save preferences only after a successful
apply; a cancelled check must not become the remembered filter.

Any and Reset Filters immediately restore ordinary server browsing. Reopening a
library remembers the selected duration but rechecks its results with visible
progress; no stale matching list is persisted.

## Why this needs a client-side check

The installed Kotlin SDK and the reviewed Jellyfin Items endpoint expose runtime
sorting but no minimum/maximum runtime query parameters. Width constraints support
the implemented resolution groups. Sending invented runtime parameters would not
establish that the server filtered the results.

Reference: [Jellyfin ItemsController](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Controllers/ItemsController.cs).

Filtering only loaded cards would miss matches later in the library and produce
incorrect totals. Scanning media files, downloading videos, requiring a plugin or
changing the server is outside this design.

## Query and result pipeline

1. Capture the effective GetItemsRequest and the selected server/user identity.
   Preserve every existing scope constraint, sort, letter and server-side filter.
   Remove only pagination and optional display payload for the metadata pass.
2. Read the matching movie metadata in sequential pages of 500. Disable images
   and user-data payloads and avoid extra fields. Use the returned ID and full
   RunTimeTicks; do not retrieve media sources or file contents. Confirm that
   supported servers include runtime in this reduced response during the prototype.
3. Compare ticks with durationMinutes * 60 * 10,000,000 using Long arithmetic.
   Keep matching IDs in server result order, deduplicating IDs. Read through the
   complete candidate result, including the final page. Never stop merely because
   an intermediate page contains no runtime matches.
4. Publish the matching-ID index atomically only after the check succeeds. Its
   size is the filtered total. Ordinary card pages request at most the normal
   adapter batch of IDs and restore index order after the response. Do not put
   the whole matching-ID list into a URL or load every artwork/card at once.
5. Maintain a separate index cursor from rendered-card count. Missing/deleted IDs
   must not cause duplicate pages or permanently stop pagination. Invalidate and
   recheck a changed result rather than claiming the old index is still exact.

The current ItemRowAdapter couples its server offset to itemsLoaded. Runtime
pagination therefore needs a dedicated result source/cursor, not a predicate
added to retrieveItems after its existing count calculation. Keep the ordinary
server path unchanged when runtime is Any.

## Lifetimes, invalidation and failures

- Own the check and ID index in the library view lifecycle, rather than the
  process-wide retrieval scope. Cancel on Back, view destruction, replacement
  query, library change or account/server switch. Discard late responses using
  a request generation and captured identity.
- Retain at most the current successful index in memory. Its key includes
  server/user, library scope, every query filter, sort and selected duration.
- Reuse that index only for paging the same applied query. Recheck after explicit
  refresh, a relevant library change, or watched/favorite changes that affect
  the query. Check current authorization when fetching card pages.
- Count changes or duplicate IDs encountered during the metadata pass indicate
  a changing library. Retry the check at most once, then explain that the library
  changed and offer manual Retry. Jellyfin does not provide a transactional
  snapshot here; do not claim stronger consistency than ordinary server browsing.
- Authentication, permissions and network failures are errors, not empty results.
  Cancellation is a distinct outcome and must not display an error or save edits.
- Begin the prototype with a 100,000-candidate and 180-second work budget. If
  exceeded, stop with a request to narrow other filters; never silently truncate
  or present a capped index as complete. Tune these limits only from measurement.

## Prototype and acceptance gates

Before shipping, measure metadata bytes, server calls, elapsed time and peak
memory for small, 10,000-item and large libraries, including a slow connection.
The full check may take noticeably longer than ordinary server filtering; the
progress view must stay responsive and cancellation must stop further requests.
Do not advertise a short or instant check before measurements support it.

Automated cases must include:

- Strict duration boundaries, invalid/missing runtimes and overflow-safe units.
- Matches only on later pages, zero-match pages, exact final-page boundaries,
  complete totals and no duplicate cards.
- Combined filters, preserved sort/letter/scope and bounded card-page ID requests.
- Cancellation, identity switches, replaced requests and late response rejection.
- Mid-check count changes, missing IDs, network errors and work-budget failures.
- Any/reset restoring ordinary browsing, successful persistence and cancelled
  staged edits leaving the previous query intact.

User validation must cover a short demo, known longer movies, several combined
filters, reopening the library, cancelling a check and recovery after a network
failure. #20 stays open through implementation and those checks.

## Prototype measurement (2026-10-15)

The user supplied a screenshot from Check Movie Lengths / Under 90 minutes:
5,033 movies checked in 3.8 seconds using 11 sequential requests; 889 matched and
2 had no valid runtime. This confirms the reduced response includes usable runtime
metadata on this server and provides a timing baseline for this library. It is
not independent ground-truth verification of all 889 matches, a slow-network
measurement, a memory profile, or a guarantee of performance on larger libraries.

Seven scanner regression tests pass, including a 100,000-item synthetic dataset,
strict runtime boundaries, later-page matches, cancellation, changing/duplicate
results and work-budget failures.

## Integrated validation (2026-10-15)

The prototype has been replaced by staged runtime filters with Apply, complete
metadata scanning, bounded ordered result pages, retry/cancel handling and saved
filter preferences. The user verified 889 under-90-minute results, paging beyond
the first page, Crime combined with runtime (76 results), A + Crime + runtime
(6 results), Cancel preserving results and returning to filters, Reset restoring
all 5,033 movies, and persistence after reopening. A lifecycle timing bug on
reopening was fixed and manually retested. The 21 scanner, result-page, combined
filter and alphabet tests pass. Network-failure recovery of the integrated dialog
was subsequently tested as described below.

## Network timeout and Retry validation (2026-10-15)

Recorded Crime + Under 90 minutes at 76 results (687 metadata candidates).
Used emulator network shaping at 1 bit/s to stall the connection, then reopened
the Movies library with those saved filters. The runtime check displayed an
explicit failure with Cancel and Retry, rather than publishing zero matches.
Restored unrestricted network shaping and selected Retry. The scan completed
and the grid returned with the same Crime + Under 90 minutes filters, ascending
name order and 76 results. No code fix was needed for this path.

The initial mobile-data toggle did not interrupt this Ethernet-based TV emulator
and is not counted as a failure test. Root access was rejected by automatic
approval review; no privilege change was made. The successful test exercises a
stalled-connection timeout and recovery on saved-filter retrieval, not a physical
Ethernet disconnection midway through applying different staged filters.
Emulator network speed/delay were restored and verified as unrestricted/zero.
