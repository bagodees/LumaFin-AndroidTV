# LumaFin roadmap

The plan for each upcoming version, in order. Every feature has a GitHub issue in its
version's milestone, and the [By version](https://github.com/users/bagodees/projects/1/views/2)
project view shows them together. Design details for the redesign are in
[docs/redesign-plan.md](docs/redesign-plan.md).

## How to use this roadmap

- **Work on the current version only.** Finish or explicitly defer every item in the
  current version before starting items from a later one.
- **Don't start later items early** without the owner's approval, even when they look
  easy or related. If an approved exception happens, note it in the issue.
- **Scope is settled before a version starts.** New ideas go to a later milestone
  (or "1.x later"), not into the version being built.
- **Order within a version** follows the list below unless the owner says otherwise.
- Bug fixes for the released app go in **1.0.x** at any time.
- When a version ships, mark it Complete here and move **Current version** to the next one.

Statuses: **Planned**, **In progress**, **Complete**, **Deferred**.

**Current version: 1.2.** Next: 1.3. The remaining 1.0.x items wait only on outside steps (Amazon's review and the Google Play testing period); fixes for the released app still go in 1.0.x.

---

## 1.0.x: launch and fixes

Status: In progress (waiting on Amazon review and Play production access). Milestone: [1.0.x](https://github.com/bagodees/LumaFin-AndroidTV/milestone/1).

The released app on Google Play, Amazon Appstore and GitHub. No redesign work.

### Google Play ([#1](https://github.com/bagodees/LumaFin-AndroidTV/issues/1))

Closed testing is active with 1.0.0 (code 1000101). Google requires 12 testers opted in
for 14 days before production access. Details: [Google Play setup](docs/google-play-setup.md).

1.0.1 (code 1000199) packages the TrueHD surround fallback fix
([#106](https://github.com/bagodees/LumaFin-AndroidTV/issues/106)). Signed builds and
377 app unit tests passed. GitHub v1.0.1 is published; the website release feed is
updated. Play Alpha 1.0.1 was submitted for review on October 24; quick checks
are running and tester availability is not yet confirmed. Sony/Beam surround confirmation
remains pending in #106; Amazon's in-review submission is unchanged.

1.0.2 (code 1000299) shipped under 1.0.x with the Dolby Vision direct-play and
remote search-navigation fixes ([#108](https://github.com/bagodees/LumaFin-AndroidTV/issues/108),
[#109](https://github.com/bagodees/LumaFin-AndroidTV/issues/109)). GitHub is published
and the website download/update links point to 1.0.2. Play Alpha was submitted for
review on October 24; tester availability awaits approval. Amazon's in-review
submission remains unchanged. No new Play/Amazon checklist items are completed.

- [x] Play developer account, Play App Signing with the existing key, and a release bundle
- [x] Store text, screenshots, TV artwork, privacy policy and Data safety
- [x] Hide the GitHub update notice for Play installs
- [ ] Reach 12 testers for 14 days
- [ ] Apply for production access and publish

### Amazon Appstore ([#79](https://github.com/bagodees/LumaFin-AndroidTV/issues/79))

Submitted October 24, 2026; waiting for Amazon's review. Details: [Amazon preparation](docs/amazon-appstore.md).

- [x] Developer profile, Fire TV build, artwork and privacy declarations
- [x] Submit for review
- [ ] Confirm the approved listing installs and updates on a physical Fire TV
- [ ] Point the website's Fire TV card to the Appstore listing

### Physical TV checks ([#78](https://github.com/bagodees/LumaFin-AndroidTV/issues/78))

- [x] Confirm the October fixes on a real TV ([checklist](docs/v1-remaining-checks.md)); completed by the owner on the Sony X90J, October 2026

---

## 1.1: the LumaFin layout

Status: Complete. Release 1.1.0 prepared October 26, 2026; publication and store uploads await separate owner approval. Milestone: [1.1](https://github.com/bagodees/LumaFin-AndroidTV/milestone/2).

Release notes: [1.1.0](docs/releases/1.1.0.md).

1. [x] Design two Home concepts and choose one ([#80](https://github.com/bagodees/LumaFin-AndroidTV/issues/80)): concept A (side rail with a preview area) chosen; concept B (top bar with backdrop) becomes a Home style option in 1.2
2. [x] New Home screen, concept A ([#81](https://github.com/bagodees/LumaFin-AndroidTV/issues/81))
3. [x] LumaFin layout by default, with "Switch to classic Jellyfin style" ([#82](https://github.com/bagodees/LumaFin-AndroidTV/issues/82))
4. [x] Side menu on Search, Libraries, library grids and the Live TV guide; grids scroll top to bottom with an A–Z strip ([#111](https://github.com/bagodees/LumaFin-AndroidTV/issues/111))
5. [x] New details pages for movies, series, seasons and episodes, with the side menu ([#85](https://github.com/bagodees/LumaFin-AndroidTV/issues/85))
6. [x] Side menu entry for each library, Live TV straight to the guide, Libraries only as overflow ([#112](https://github.com/bagodees/LumaFin-AndroidTV/issues/112)); added October 26 with the owner's approval
7. [x] Tabbed library pages for Movies and TV: Recommended, Library, Collections/Studios and Genres ([#92](https://github.com/bagodees/LumaFin-AndroidTV/issues/92)); moved from 1.x later on October 26 with the owner's approval

---

## 1.2: settings, menus and Home options

Status: Planned. Milestone: [1.2](https://github.com/bagodees/LumaFin-AndroidTV/milestone/3).

1. [ ] Home style option: top bar with backdrop, concept B ([#110](https://github.com/bagodees/LumaFin-AndroidTV/issues/110))
2. [ ] Reorganize settings into a full-screen layout ([#83](https://github.com/bagodees/LumaFin-AndroidTV/issues/83))
3. [ ] One LumaFin action menu for every card ([#84](https://github.com/bagodees/LumaFin-AndroidTV/issues/84))
4. [ ] Pin genres, studios, playlists and Favorites to Home ([#96](https://github.com/bagodees/LumaFin-AndroidTV/issues/96))
5. [ ] Remove a show from Next Up ([#97](https://github.com/bagodees/LumaFin-AndroidTV/issues/97))
6. [ ] Video/photo filter for mixed libraries ([#86](https://github.com/bagodees/LumaFin-AndroidTV/issues/86))
7. [ ] Use the Home Screen Sections plugin when the server has it, with LumaFin's own sections otherwise ([#107](https://github.com/bagodees/LumaFin-AndroidTV/issues/107))
8. [ ] Choose which libraries appear in the side menu ([#113](https://github.com/bagodees/LumaFin-AndroidTV/issues/113))

---

## 1.3: setup and personal touches

Status: Planned. Milestone: [1.3](https://github.com/bagodees/LumaFin-AndroidTV/milestone/4).

1. [ ] Guided first-run setup ([#88](https://github.com/bagodees/LumaFin-AndroidTV/issues/88))
2. [ ] "Fits tonight": pick how much time you have ([#87](https://github.com/bagodees/LumaFin-AndroidTV/issues/87))
3. [ ] Accent color themes ([#90](https://github.com/bagodees/LumaFin-AndroidTV/issues/90))
4. [ ] PIN lock for switching users ([#98](https://github.com/bagodees/LumaFin-AndroidTV/issues/98))
5. [ ] Optional theme music on details pages ([#99](https://github.com/bagodees/LumaFin-AndroidTV/issues/99))

---

## 1.x later: not yet assigned to a version

Status: Planned. Milestone: [1.x later](https://github.com/bagodees/LumaFin-AndroidTV/milestone/5).

These move into a numbered version when it's planned. Unordered.

- [ ] Redesign series and season browsing ([#91](https://github.com/bagodees/LumaFin-AndroidTV/issues/91))
- [ ] Light theme, once every main screen supports themes ([#93](https://github.com/bagodees/LumaFin-AndroidTV/issues/93))
- [ ] Request movies and shows through Seerr ([#100](https://github.com/bagodees/LumaFin-AndroidTV/issues/100))
- [ ] Install updates from inside the app on Fire TV ([#101](https://github.com/bagodees/LumaFin-AndroidTV/issues/101))
- [ ] Live TV: quick channel switching and stall recovery ([#102](https://github.com/bagodees/LumaFin-AndroidTV/issues/102))
- [ ] Evaluate wider playback format support ([#103](https://github.com/bagodees/LumaFin-AndroidTV/issues/103))
- [ ] SyncPlay group watching ([#104](https://github.com/bagodees/LumaFin-AndroidTV/issues/104))
- [ ] Kids mode, after the PIN lock ([#105](https://github.com/bagodees/LumaFin-AndroidTV/issues/105))

---

## 2.0: the all-new LumaFin

Status: Planned. Milestone: [2.0](https://github.com/bagodees/LumaFin-AndroidTV/milestone/6).

1. [ ] Remove the old Home and details screens ([#94](https://github.com/bagodees/LumaFin-AndroidTV/issues/94))
2. [ ] Launch 2.0: store listings, website, README and release notes ([#95](https://github.com/bagodees/LumaFin-AndroidTV/issues/95))

---

## Not planned

- An independent rebuild in a separate codebase (withdrawn October 23, 2026).
- A phone or tablet app; planned separately after the TV redesign.
- Playlist management; the music-focused review ([#60](https://github.com/bagodees/LumaFin-AndroidTV/issues/60)) is closed.

Contributor changes require maintainer review; automatic merging is disabled.
