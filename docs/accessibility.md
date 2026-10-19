# Accessibility: text size, high contrast and focus outline (#23)

Settings → Appearance & Home → **Accessibility** (all three are also in settings
search). Each is saved per Jellyfin account like other appearance settings, is
off by default, and is not part of settings sync (#27) or backup (#7) because
the right value depends on the TV and viewing distance.

## Text size

Default (100%), Large (115%) and Larger (130%). The factor multiplies the TV's
own system font scale and is applied to the whole activity in
`attachBaseContext`, so classic XML screens and Compose screens both follow it.
Changing it reloads the current screen immediately (settings reopen at the top);
starting the app applies the saved value.

Checked at 130% on the emulator: Home rows, details (title, metadata, summary,
track chips, action-button labels) and settings fit without clipping. Longer
summaries wrap one more line before their existing ellipsis.

## High contrast

Applies a high-contrast variant of the Compose color scheme (`highContrast()`):
white headings, body and captions; brighter overlines; a lighter, more visible
focused row; white focused buttons with black text; a darker panel surface and
heavier scrim. Brand accents (badges, sliders) are unchanged. Card title strips
use a near-solid background, and the dimmed info values on the details page go to
full opacity. It updates visible screens without a reload.

## Stronger focus outline

A 3dp white outline on whatever has focus:

- Compose list rows, buttons and media cards draw it themselves
  (`Modifier.focusOutline`), following each control's own shape.
- Classic views (details action buttons, track chips, other Leanback screens)
  get it from a global focus listener installed in the main and startup
  activities. It is drawn as the view's foreground, so layout never moves, and
  the previous foreground is restored when focus leaves.

Turning it on or off applies to the next focus change.

## Not covered

The video player overlay and subtitles keep their own styling; subtitle size,
weight, colors and brightness already have dedicated settings. Server web
content and system dialogs follow the TV's own accessibility settings.

## Validation

Unit: text-size factors, the high-contrast scheme (stable when applied twice,
readable focused buttons, brand accents unchanged). All 353 tests pass.

Emulator: outline on settings rows, Home cards, details action buttons and track
chips; high-contrast settings panel; 130% text on Home, details and settings;
live switch from Larger to Large and back to Default; restart keeps the choice.
Physical TV checks (couch readability, overscan, other screens) are in #68.
