# LumaFin roadmap

This file tracks agreed project milestones. Detailed work can be tracked in GitHub Issues,
with links added here as issues are created. Suggested features are added only after they
have been accepted for the roadmap.

Milestone statuses: **Deferred**, **Planned**, **In progress**, and **Complete**.

## Future milestone: Google Play distribution

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
