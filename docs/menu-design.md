# LumaFin menu pattern

`LumaFinMenuDialog` provides a reusable rounded navy panel with a clear heading,
short supporting text, scrollable rows, and a fixed action footer. Rows separate
the setting name from its current value. A cyan focus outline is distinct from
the check mark for a saved/draft selection. Stronger Focus uses white outlines;
High Contrast brightens secondary text. Text follows the app's font scaling.

The panel fits the available screen size, supports native D-pad scrolling and
Back dismissal, and closes when its view lifecycle stops. No entrance animation
is required, including with Reduced Motion. Native callbacks and existing filter
query/runtime logic remain in use.

The first conversion covers library Filters, its choice pickers, option loading
and errors, and runtime-check progress/errors. Changes remain drafts until Apply;
Back/Cancel discards them, and Reset also waits for Apply. Further menus can adopt
the same component after visual review; sorting and alphabet jump remain separate.

The library Hide watched shortcut uses an icon with a persistent active color
and outline. On/Off remains in its accessible description, without visible state
text. Existing reset/apply/runtime-restoration paths update it consistently.

Details pages also use the shared panel for audio/subtitle selection, More,
Version, Extras and remembered track scope. Selection uses a check mark;
chevrons indicate further choices, while immediate actions have none. Compact
details panels fit their content, capped to the available screen height. Back
returns focus to the invoking control. Shared detail buttons and track chips use
rounded navy surfaces with cyan focus outlines; stronger focus stays white.

Subtitle lookup uses the same panel for languages, results, confirmation,
progress and errors, including when opened from the player. Provider metadata
appears beneath result names. Existing server permission, cancellation,
source/account guards, download verification and retry behavior are retained.

These visual changes were built with :app:assembleDebug. Runtime and visual
acceptance are handled by the user; no tests or emulator navigation were run.