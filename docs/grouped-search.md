# Grouped search and people discovery (#54)

Implemented and checked locally; not committed or pushed.

## Audit and changes

Search already had separate movie, series, episode, collection, people and other media rows. The existing item launcher opens person details, whose existing rows show their movies, series and episodes. These routes are reused.

People now use the installed Jellyfin SDK's dedicated Persons API with the current user's ID. Item searches also explicitly include that user. Requests retain the existing 25-result limit per category and use the existing lightweight browse fields; opening a result fetches its details normally. Collections and People now appear immediately after Movies, Series and Episodes when those rows contain results. Empty rows are omitted.

Search state distinguishes guidance, loading, successful empty results, complete failure and partial failure. Successful categories remain visible when others fail; a message identifies failed categories and offers Retry. Submitting an unchanged query bypasses its pending debounce, and Retry reruns it. Changed or blank queries cancel earlier work and clear stale results. Keyboard submission hides the keyboard and moves focus out of the field. Query and selected row/item are retained when returning from details; a restored saved query can run again if the view model was recreated.

Status messages use the theme's foreground color on a dark background so artwork does not make them unreadable.

## Checks

- Debug build and eight focused tests passed: category order, partial failure, empty versus failed searches, separate people/collection queries, cancellation, retrying an unchanged failed query, immediate submission during debounce, newer query superseding old work, and blank-query reset.
- Emulator profile picker confirmed `test` as current before checks.
- `Pioneer` returned the Series row. Opening and returning from the series preserved the query.
- Broad query `a` showed Movies, Episodes and People as separate populated rows. Remote Up/Down moved between rows and back to the query field.
- Opened the sixth People result, Cas Jansen, and saw his details and Elephants Dream under Movies. Back restored the same query, People row and Cas Jansen selection.
- `Valerie` returned Valerie LaPointe in People.
- `zzznomatch987` produced the empty-results guidance with the keyboard dismissed. Clearing the field removed prior results and returned to initial guidance.
- Final installed build was checked for readable guidance/empty-state contrast and keyboard submission.

Failures, retry and cancellation were exercised with controlled repository tests rather than interrupting the live server. The test library did not provide a populated collection example. Physical-TV remote and voice-recognition testing were not performed. No media was played, no watch state was changed, and no server-side content was edited.
