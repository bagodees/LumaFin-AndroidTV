# LumaFin roadmap

This file tracks agreed project milestones. Detailed work can be tracked in GitHub Issues,
with links added here as issues are created. Suggested features are added only after they
have been accepted for the roadmap.

Milestone statuses: **Deferred**, **Planned**, **In progress**, and **Complete**.

Each planned version has a GitHub milestone with one issue per feature:
[1.0.x](https://github.com/bagodees/LumaFin-AndroidTV/milestone/1),
[1.1](https://github.com/bagodees/LumaFin-AndroidTV/milestone/2),
[1.2](https://github.com/bagodees/LumaFin-AndroidTV/milestone/3),
[1.3](https://github.com/bagodees/LumaFin-AndroidTV/milestone/4),
[1.x later](https://github.com/bagodees/LumaFin-AndroidTV/milestone/5) and
[2.0](https://github.com/bagodees/LumaFin-AndroidTV/milestone/6).
The [By version](https://github.com/users/bagodees/projects/1/views/2) project view shows them together.
A version's scope is settled before work on it starts; new ideas go to a later milestone.

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

## Interface redesign (1.x to 2.0)

Status: Planned October 23, 2026. No release date is set.

Plan: [Interface redesign](docs/redesign-plan.md).

Replace the visible screens with LumaFin's own layouts one at a time, inside this app,
keeping the existing playback, sign-in, sync and features. Each new screen ships first
as an option testers can switch back from, then becomes the default.

- [ ] Review two Home concepts as mockups and choose one.
- [ ] 1.1: new Home screen available as an option.
- [ ] 1.2: new Home by default; reorganized full-screen settings; one LumaFin action menu for all cards; new details screen available as an option; video/photo filter for mixed libraries.
- [ ] 1.3: "Fits tonight" viewing-time picker; guided first-run setup; new details screen by default; accent color themes.
- [ ] Remaining screens as needed (series and seasons, library, search).
- [ ] Light theme, once every main screen supports themes.
- [ ] 2.0: all main screens are new and the old Home and details screens are removed.
