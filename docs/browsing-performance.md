# Browsing performance (#66)

## Profile before changing behavior

The Home audit found bounded common item rows and existing lightweight browsing fields. The grid audit found two shared costs worth fixing before introducing any cache:

- `BrowseGridFragment.buildAdapter` calculated a viewport-dependent page size and then unconditionally replaced it with 100.
- `ItemRowAdapter.setItems` read every previously loaded card, built a new complete list and sent the complete list through DiffUtil for every later page. A refreshed first page could also retain the old query's unrefreshed tail and advance its cursor past that stale tail.

A JVM profile ran the actual adapter `setItems` extension with a recording adapter, 1,000 ordered items and 25-item pages. It counts prior-card reads and cards submitted to full-list diffing; it does not measure Android frame time or network latency.

| Work across 40 pages | Before | After |
| --- | ---: | ---: |
| Reads of previously loaded cards | 19,500 | 0 |
| Cards submitted to full-list diffing | 20,500 | 25 |
| Cards appended with insertion notifications | 0 | 975 |

On the test emulator's existing landscape grid, the measured viewport was 15 cards with a stride of 3. Its page limit is now 25, compared with the former fixed 100. This reduces the requested page cap by 75%; the available demo library has only 10 movies, so it is not evidence of a 75% reduction in transferred bytes or loading time.

## Changes

Page sizing fills the viewport plus one row/column, with 25–150 item bounds. Nonpaged queries remain nonpaged. Later contiguous pages append their new card objects with one insertion range and retain all existing objects. Initial/replacement responses still use a diff and remove the old query tail, so the next server offset reflects only newly loaded results.

Paged Items, Artists, Album Artists and Live TV channel requests share a latest-request coordinator. Replacing a request cancels its previous job; a generation check also rejects late results from work that ignores cancellation. Server and account identity must still match before publishing. Grid destruction/rebuild and Home row replacement/destruction cancel their pending paged requests. Background page completion does not request grid focus away from the toolbar; first-page arrival can still focus the grid.

A failed request retains the previous cards and cursor. The grid offers Retry/Cancel; Retry repeats the failed page's captured request and offset rather than skipping it or starting again from the beginning. Nonpaged query failures retry their existing retrieval. Cancelling or replacing the query discards its retry action. The client-side runtime filter keeps its separate checked-ID pagination and cancellation behavior.

No metadata scanner, persistent cache, new library mode, server plugin, or server data mutation is introduced. Full browsing metadata and existing server queries are retained. Home's existing post-playback refresh behavior is preserved, including bringing previously empty rows back.

## Checks and limits

Regression tests exercise the real page-application path with 10,000 ordered cards (including a short final page), identity preservation, refreshed/empty results, viewport sizing, delayed and superseded responses, account changes, cancellation, discarded retries, failure/retry without losing earlier cards, and existing runtime-filter pagination. The complete app unit suite passes.

Emulator checks use only the test account and its demo libraries: adaptive page sizing, grid navigation, details metadata, return to the selected card, filter replacement, and a timeout/Retry recovery under a temporary 1 kbit/s throttle. Restoring unlimited speed and retrying returned all ten demo movies. The original network configuration and unfiltered Name/Ascending library setting were restored.

Large-library coverage is synthetic, not a live 10,000-title server or physical-TV frame-rate measurement. Physical TV navigation, dense poster layouts and real large-library/server timings remain useful follow-up measurements; the work-count profile above is the verified improvement.
