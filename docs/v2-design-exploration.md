# V2 design exploration

Discussion notes, October 22, 2026. These are candidates to evaluate, not approved
features, an implementation plan, or a commitment to replace the current app. A full
redesign and independent rebuild remain undecided. Improvements may instead be made
within the existing app. This document keeps the discussion in the current repository;
it does not depend on access to the separate private prototype repository.

## Intended direction

Explore a recognizable TV experience that helps people decide what to watch,
understand what will play, and return to where they left off. A coherent experience
is more useful than accumulating features or claiming competitors lack shared ones.

## Design inspiration from installed clients

Initial hands-on inspection: Wholphin 1.0.9 and Moonfin 2.7.0 on the same Android TV
emulator and Jellyfin server. Existing layouts were used; settings were not exhausted.
This was not a playback, performance or reliability benchmark.

| Observed pattern | Candidate LumaFin interpretation |
| --- | --- |
| Wholphin's readable left navigation | Compare an expanding labeled rail with a labeled top bar; choose one consistent navigation structure. |
| Wholphin's focused-title artwork and metadata | A stable preview area with concise information and remaining runtime, without shifting focus targets or mandatory video previews. |
| Wholphin's season tabs and episode strip | Keep selected season, episode progress and nearby episodes visible; restore the same position on return. |
| Wholphin's consolidated More menu | Keep secondary actions organized while making essential playback choices directly discoverable. |
| Moonfin's prominent artwork and organized detail sections | An original, readable information layout with restrained backdrops and purposeful Episodes, People and Related sections. |
| Moonfin's audio/subtitle controls beside Play | A labeled playback setup panel showing selected values beside the start/resume action. |
| Moonfin's extensive player tools | A simple primary transport row and a clearly labeled secondary tools panel, with readable focus labels and safe screen margins. |

Both clients support audio/subtitle selection before playback: Wholphin through More,
Moonfin beside Play. LumaFin's labeled selectors differ in presentation, not exclusivity.
Use interaction principles as inspiration; do not reproduce competitor screens, assets
or source. If an independent rebuild is chosen later, preserve that implementation boundary.

## Potential signature flows

1. **Find something that fits tonight.** Choose available time, movie/episode and
   unwatched/rewatch preferences; receive a small shortlist with clear reasons.
   Begin with library metadata and deterministic filters. Unknown runtimes must be
   labeled rather than assumed to fit. Measure time and remote presses to a choice.
2. **Know what will play.** Show version, audio, subtitles, remembered preference scope
   and resume/start choices together. Explain unavailable preferences and fallback
   selections. Do not promise Direct Play, HDR or passthrough from metadata alone;
   distinguish provisional information from actual negotiated playback.
3. **Plan the session.** Queue episodes, show an estimated finish time and choose a
   stop rule before starting. Keep the plan accessible during playback. Handle resume
   positions, missing runtimes and removed items; start locally without promising
   cross-device session synchronization.

These are hypotheses about a useful combined experience, not claims of unique invention.

## Supporting candidates

- Home editor with live preview, useful presets and Undo rather than a long settings list.
- Spoiler-safe browsing across Home, details and search, with explicit reveal controls.
- Named version/edition comparison where Jellyfin metadata supports it.
- Plain playback error explanations, meaningful recovery actions and private, redacted diagnostics.
- Readable text/density presets, clear focus labels and reduced-motion support.
- Explainable discovery rows that can be adjusted and saved as useful views.

Plex collections and discovery suggest ways to organize and choose content. Emby's
configurable Home sections suggest useful source/filter/presentation combinations.
Optional cinema intros are lower priority than prompt, dependable playback. These
Plex/Emby observations come from documentation, not hands-on testing of their apps.

## Visual and interaction principles to evaluate

- Clear hierarchy: title and watch state first, meaningful choices next, technical details on demand.
- Consistent typography, spacing, card shapes, focus treatment and full-screen gradients.
- Readable screens with bright or missing artwork, loading, empty results and the keyboard open.
- Restrained transitions that explain movement, with no moving focus targets.
- Exact return to the logical card, row and scroll position after details or playback.
- Visible loading, offline and retry states; a backdrop alone must not imply a working screen.
- Essential actions accessible without undisclosed long presses.

Reliability, pre-play track selection, favorites and series browsing are baseline
expectations. More themes, badges, animations or integrations alone do not establish
a distinct product. Avoid speed, compatibility or leadership claims without evidence.

## Decision gate before implementation

Keep v1 maintenance and release work separate. If we choose to take this discussion
further, first review two original Home concepts and one details/player flow using
neutral sample content. Compare remote tasks: resume, find a title under 30 minutes,
choose subtitles before Play, plan two episodes, return to the same card and recover
from an unavailable server. Include large text and cold launch on physical TVs.

Only then decide which ideas are worth implementing, whether the existing app can
support them, and whether a complete independent redesign is justified. Create
implementation issues and revise milestones after that scope decision.

## References

- [Wholphin](https://github.com/damontecres/Wholphin)
- [Moonfin Core](https://github.com/Moonfin-Client/Moonfin-Core)
- [Plex collections](https://support.plex.tv/articles/201273953-collections/)
- [Plex discovery](https://www.plex.tv/discover/)
- [Emby Home configuration](https://emby.media/support/articles/apps/common/User-Prefs-HomeScreen.html)
- [Emby Cinema Intros](https://emby.media/support/articles/Cinema-Intros.html)

Recheck current versions and behavior before making public comparisons.
