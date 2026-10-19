# Optional compact library list (#56)

Open a movie/TV library's display settings (the gear in its browser, or Settings
→ Appearance & Home → Libraries → choose a library) and enable Compact list.
Settings search also finds the option and opens the library chooser.

The default remains the grid. Compact list uses a vertical, single-column
Leanback grid with recycled rows, small artwork, readable title, available year
and duration, and watched/progress information. Missing metadata is omitted;
invalid percentages cannot overflow the progress bar. Long titles use two lines
and ellipsis. Each row has one focus target and an explicit focus outline, without
zooming beyond the screen. The usual details, Play and long-press actions remain.

The choice uses the existing Jellyfin display-preferences store, scoped by the
authenticated user, server and library. Cache clearing on account change removes
the preceding user's local copy. It is separate from grid direction, image type
and image size; disabling it restores those choices. Smart library screens remain
separate and unchanged. Music/other library types do not offer the option.
It is not included in the local settings backup, which excludes server-backed
library preferences.

The list reuses the existing adapter/query, sorting, combined filters, letter
jump, bounded page sizing and request cancellation/retry. It does not fetch the
whole library, add a persistent index, or introduce another loading path. Its
viewport uses one-item stride and the existing 25–150 page-size bounds.

Validation: debug build and all 310 app unit tests passed. Six new tests cover
duration/progress edge cases, supported library types, independent library
choices, preservation of grid choices and account-switch cache clearing.
Existing pagination coverage includes 10,000 ordered items and a final short page.

Visible-emulator checks used only the test account: movie and series lists,
available year/runtime, watched/partial-progress rows, navigation to the final
movie (10 of 10), details and selected-row return, long-press menu, empty Favorites
filter and recovery, restart persistence and independent movie/TV choices. Both
libraries were restored to their original grids and Favorites was turned off.
No playback or explicit server watch-state changes were needed.

The demo account contains ten movies and one series; large-library behavior uses
the existing synthetic paging tests, not a measured hardware benchmark. Actual
TV-distance readability, long translated titles, large-library scrolling and
real cross-account/server acceptance remain in the physical-TV checklist (#68).
