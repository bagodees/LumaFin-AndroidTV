# Appearance review — October 27, 2026

Design discussion only. No app behavior changed or public issue/release created.
Current roadmap version is 1.2. Full-screen settings (#83) landed in d40f229e7.

## Owner decisions

- Inspect only emulator-5554 over ADB. Do not take over desktop mouse/keyboard;
  do not connect to the physical TV. Preserve emulator/device settings.
- Full-screen settings: categories on the left, options on the right.
- Three independent Landscape / Portrait choices: Continue Watching, other Home
  rows, and library grids. Retain current defaults and support both Home layouts.
- Cards keep their chosen shape when focused, with a subtle zoom and focus border;
  no portrait-to-landscape expansion or shifting neighboring cards.

## Layout patterns reviewed

These patterns informed the design discussion; they are not implementation commitments.

### Compact preview and side navigation

- Home has a narrow icon rail which expands to labeled navigation.
- Compact preview contains title logo, episode, metadata and a short description.
- Roughly seven portrait cards fit across the collapsed-rail Home view; cards
  carry small episode badges and progress bars, with a purple focus outline.
- Settings use large labels, secondary current-value text and a filled focus row.
- Application Theme opens a simple list of color choices.

### Backdrop presentation and top navigation

- Floating top navigation expands the focused item's label. Libraries can expose
  additional library tabs.
- Home starts with a large backdrop presentation. Below it are My Media and a
  combined Continue Watching & Next Up row in the installed configuration.
- TV library shows eight portrait columns at 1920x1080, with titles/secondary
  metadata below and count badges. This is an observed arrangement, not a proposed
  hard-coded LumaFin column count.
- Settings are a right-side panel with labeled categories, descriptions, icons,
  a settings search field, and a strong white/cyan focus state.
- Personalization groups General Style, Details Screen, Navigation, Screensaver,
  Home Screen and Libraries.
- General Style exposes Interface Style, Interface Layout, App Theme, Theme Store,
  Saved themes and Focus Border Color.
- Home Screen exposes Row Type, expanding My Media cards, card display size,
  row padding, expanded rows and merge Continue Watching/Next Up.
- Details page uses rounded action buttons and tabs for Cast, Crew, Studios,
  Chapters, Details and Similar.

## Proposed LumaFin direction

- Under Appearance, show each card-style setting with its current value, a short
  scope description, and visual Landscape / Portrait previews.
- Keep shape separate from sizing. Avoid changing landscape dimensions when
  implementing portrait choices; preserve the library grid's intended density.
- Use portrait cover art for portrait cards; use appropriate landscape artwork
  for wide cards, with a predictable fallback when artwork is missing.
- Keep consistent gaps, rounded corners, focus treatment and badge placement.
- Reserve room for focused-card scaling so outlines do not clip or overlap.
- Keep readable title/episode/progress information in either shape.
- The preview area sizes to its text and the rows follow it smoothly (owner's
  choice, October 27); the title always starts at the same spot under the menu.
- Theme colors and other later-version features are observations only; do not
  pull them into 1.2 without owner approval.

## Focus behavior

Owner selected stable card shapes with a subtle zoom on October 27. Reserve focus
space so the zoom and border do not clip or shift the row layout.

Screenshots from this inspection are local artifacts in the chat's visualization
directory. They are not store artwork and must not be published as LumaFin images.
