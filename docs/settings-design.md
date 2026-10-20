# Settings design

All Compose settings pages use shared settings-only row components. Other app menus are unaffected.

- Navigation rows explicitly set `navigates = true`, with a trailing chevron. Existing artwork or color swatches remain beside it. Immediate actions and single-choice options have no chevron.
- Boolean rows use `toggleState` and a passive `SettingsSwitch`. The whole row owns remote focus, click handling and accessibility switch semantics. Cyan indicates enabled; thumb position also conveys state.
- Single-choice rows retain their radio selection indicators, distinct from focus.
- Rows, including sliders, use a blue focus surface with a cyan outline. Stronger focus retains its thicker white outline. Text colors follow the high-contrast theme.
- Page headings and section labels have distinct sizes. The slightly wider panel and larger, lighter supporting text improve readability without truncating explanations.
- Library settings group grid choices under Layout and switches under Browsing. Existing preference keys, account/library scope, router destinations and focus keys are preserved.

Build verification: `:app:assembleDebug`. The user handles runtime and visual acceptance; no automated or emulator tests were run for this redesign.

Suggested user acceptance: main categories and search; library switches and persistence; playback choices and Back focus; subtitle preview/sliders; the remaining Accounts, Help and Appearance screens. On a physical TV, include large text, high contrast and stronger focus.
