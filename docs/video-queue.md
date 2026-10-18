# Video playback queue (#59)

Both built-in video players offer a session-only Video queue: the standard player has a labelled queue control, and the experimental player includes it in More. The dialog separates Now playing from upcoming videos. Select an upcoming item to move it earlier/later or remove it. Saved Jellyfin playlists and Watch Later are never edited.

## Behavior

- Protect current and previously played entries. Validate the reviewed queue/current item immediately before every edit; stale menus cannot change another entry after playback advances.
- Preserve the current stream, playback position, autoplay configuration and remembered track languages. Standard-player changes are retained for the existing Next Up handoff.
- Load upcoming entries in batches of 50. Additional batches use the existing supplier without restarting playback. Loading can be retried and stops when the dialog closes.
- Follow the moved item with remote focus; after removal select an adjacent remaining row. Empty queues show a readable message. Next availability updates after edits.
- Experimental-player shuffle/random queues remain viewable in queue order, with an explanation that editing requires normal playback order.
- Reset removed-entry accounting when clearing a core queue, so a subsequent session has the correct size.

## Validation

Debug build passed. Nine focused tests passed (six core queue tests and three app adapter tests): protected/history/invalid indices, duplicates, stale snapshots, playback advancement, empty queues, shuffle/random rejection, supplier pagination after edits, queue reset, reviewed legacy adapter arguments and remembered track languages.

Emulator test account: standard player verified current/upcoming labels, earlier/later, removal, focus following edits, empty upcoming state and Next selecting reordered S1 E4 while removed S1 E3 stayed skipped. Experimental player verified its More entry, reorder/removal/focus and Next selecting reordered Triangular Diplomacy. The final build is installed. Original built-in player selection is restored after testing.

Physical-TV remote testing, a saved server playlist example and a live queue longer than 50 items were not exercised. Large queue/pagination and stale-state cases use controlled tests. Playback checks updated only the test account's normal playback progress; no saved playlist, Watch Later or explicit watched-state actions were used.
