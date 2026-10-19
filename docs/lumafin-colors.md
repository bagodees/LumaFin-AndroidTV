# LumaFin color foundation (#67)

The user authorized starting the UI redesign with colors. This pass takes its direction from the existing LumaFin wordmark: cyan and blue at the Fin end, with purple/lavender around the Luma mark. Existing layouts and navigation remain the starting point; the actor-page layout redesign is separate pending review.

| Role | Color |
| --- | --- |
| Main background | `#080D1C` deep navy |
| Settings, menus and panels | `#101A2D` |
| Resting controls | `#1B2A45` |
| Focused settings rows | `#233E64` |
| Focus / checked indicators | `#39CEF3` cyan |
| Progress / sliders | `#679BFF` blue |
| Selected navigation / saved state | `#B9A4FF` lavender on `#302448` |
| Primary text | `#F2F5FF` |
| Descriptions | `#B6C3DC` |
| Secondary labels / input outlines | `#9BAFD0` |

Compose and classic Android resources use matching palette values. Settings, toolbar buttons, selection controls, card focus, compact library rows, the account picker, detail buttons, popup backgrounds and playback accents share the direction. Artwork remains the visual content; recordings/errors and rating badges retain their distinct semantic colors. Alternate theme choices remain available. High contrast still overrides its defined colors, and the optional stronger white focus outline remains available.

## Validation

Debug assembly and all 367 existing app tests passed, including accessibility coverage. Numerical contrast ratios are 15.96:1 for main text on panels, 6.08:1 for captions on focused rows, 4.85:1 for small labels on focused rows, 10.41:1 for focused-button text, and 6.67:1 for selected tabs. These checks concern the defined solid colors, not arbitrary artwork or physical-TV output.

Emulator checks used the test account: Home/card focus, settings/search, library navigation, account picker, popup/review dialogs and detail buttons, including the already-watched icon while focused. High contrast and the stronger white focus outline were enabled, inspected, and restored to their original Off values. No watch-state action was activated. The final APK is installed.

The first color direction still needs the user's visual review; #67 remains In Progress. Physical-TV viewing-distance/color checks were added to #68. Layout/navigation behavior was not redesigned in this pass.
