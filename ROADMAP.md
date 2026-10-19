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

## LumaFin v2 roadmap

Public project: [LumaFin v2 Roadmap](https://github.com/users/bagodees/projects/2).
Future planning after the current v1 work; no release date is set. Inclusion does not
start implementation or settle details that still need user review.

### Playlists

Status: Planned for v2; scope review required before implementation.

Tracking issue: [#60 — Playlist support: music-focused scope review](https://github.com/bagodees/LumaFin-AndroidTV/issues/60).

Revisit playlists alongside music support, with music-focused playlists as the current
direction to review. General movie/TV playlist management remains deferred. Confirm
media types, Jellyfin capabilities and permissions, shared/server effects, and TV remote
flows with the user before implementation. Existing video Watch Later and the session
playback queue remain available.
