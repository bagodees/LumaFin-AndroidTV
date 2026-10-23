# LumaFin roadmap

This file tracks agreed project milestones. Detailed work can be tracked in GitHub Issues,
with links added here as issues are created. Suggested features are added only after they
have been accepted for the roadmap.

Milestone statuses: **Deferred**, **Planned**, **In progress**, and **Complete**.

## LumaFin v1 roadmap

Public project: [LumaFin v1 Roadmap](https://github.com/users/bagodees/projects/1).
No release date is set. The project tracks
approved features on the path toward v1. Features may ship in earlier releases; inclusion
does not commit every item to v1.0. Contributor changes require maintainer review;
automatic merging is disabled.

### Google Play distribution

Status: Deferred. Planned for a future release; implementation has not started.

Tracking issue: [#1 — Google Play distribution](https://github.com/bagodees/LumaFin-AndroidTV/issues/1).

Publish LumaFin on Google Play for Android TV and Google TV while continuing to provide GitHub APK releases.

- [ ] Set up the Play developer account and confirm applicable verification and testing requirements.
- [ ] Configure Play App Signing with the existing LumaFin app signing key and a separate upload key, preserving update compatibility with existing installations.
- [ ] Build and verify a release Android App Bundle (`.aab`), retaining the existing application ID and increasing version codes across distribution channels.
- [ ] Audit current Android TV quality, target SDK, 64-bit, and 16 KB page-size requirements before submission.
- [ ] Prepare store text, screenshots, TV artwork, privacy policy, Data safety disclosures, and reviewer access to a demo Jellyfin server.
- [ ] Direct Play-installed users to Google Play from the update notice while retaining sideloading instructions for GitHub installations.
- [ ] Adapt the inherited publishing workflow for LumaFin if release automation is introduced.
- [ ] Complete required testing and submit for Android TV review.

Recheck Google Play requirements when this milestone begins, as policies may change.

### Amazon Fire TV distribution

Status: In progress. Preparation started October 22, 2026.

Publish on the Amazon Appstore for supported Fire OS TVs, with a physical Fire TV
test before submission. Vega OS is a separate platform and is outside the initial
Android build. Setup, listing draft and device checks: [Amazon preparation](docs/amazon-appstore.md).

- [ ] Complete Amazon developer profile and confirm the test device model/OS.
- [x] Build and verify the Amazon APK while preserving standard Play TV targeting.
- [ ] Complete physical Fire TV testing and store artwork/privacy declarations.
- [ ] Run Live App Testing, then submit the Fire TV listing for review.

## LumaFin v2 roadmap

Public project: [LumaFin v2 Roadmap](https://github.com/users/bagodees/projects/2).
The independent redesign was approved October 23, 2026. V1 maintenance continues
separately. No release date is set; individual feature candidates still need prioritization.

### Original redesign and independent implementation

Status: Planned and approved October 23, 2026. Tracking issue:
[#77](https://github.com/bagodees/LumaFin-AndroidTV/issues/77).

Build an original v2 client in the separate private LumaFin-v2 repository, without
reusing implementation from the Jellyfin Android TV fork or other clients. Standard
libraries and an appropriately licensed Jellyfin SDK are allowed. Public planning
remains in this repository and the linked v2 project. Foundation scope: original
design, sign-in, browsing, details, reliable playback/resume and progress reporting,
followed by physical Android/Google TV and Fire OS validation.

Research and candidate flows: [V2 design exploration](docs/v2-design-exploration.md).

- Compare useful design patterns observed in Wholphin and Moonfin, with inspiration
  from Plex and Emby, while developing LumaFin's own visual and interaction identity.
- Explore choosing something that fits the available viewing time, a clear before-Play
  setup panel, and session planning with finish estimates and stop rules.
- Consider Home layout previews, series navigation, spoiler-safe browsing and readable
  playback recovery alongside dependable focus and return navigation.
- Review two original Home concepts and one details/player flow before implementing
  the first viewing flow. Prioritize candidate features separately; approval of the
  redesign does not promise every idea. Keep current v1 work separate.

### Playlists

Status: Planned for v2; scope review required before implementation.

Tracking issue: [#60 — Playlist support: music-focused scope review](https://github.com/bagodees/LumaFin-AndroidTV/issues/60).

Revisit playlists alongside music support, with music-focused playlists as the current
direction to review. General movie/TV playlist management remains deferred. Confirm
media types, Jellyfin capabilities and permissions, shared/server effects, and TV remote
flows with the user before implementation. Existing video Watch Later and the session
playback queue remain available.
