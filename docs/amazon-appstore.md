# Amazon Fire TV / Appstore preparation

## October 30: 1.2.1 submitted

1.2.0 was prepared in upcoming version `amzn1.devportal.apprelease.a201e05683874b14bc0defdc71b29c59` but held for the Fire Stick low-memory fix. The same version now carries the 1.2.1 Fire TV APK (1020199) and notes covering 1.2.0 and 1.2.1. Targeting (91 Fire TV, 0 tablets), DRM No, reviewer instructions and export compliance carried over. Submitted October 30; status SUBMITTED, estimate November 4, 6:00 PM PST. Details: [1.2.1 build](releases/1.2.1-build.md).

## October 29: 1.1.2 live

Amazon approved the 1.1.2 update the morning after submission; the owner reported it
live on October 29. Amazon Console checked October 30: LumaFin is **Live**, the current
version is release `amzn1.devportal.apprelease.a898550f20ed4c19994bda56fe1dddc7`
(1.1.2, 1010299), and there is no upcoming version. Physical Appstore install/update
checks are still pending.

## October 28: 1.0.0 live; 1.1.2 update

The owner reported approval, and Amazon Console was independently verified as
**Live on Appstore** for 1.0.0. Physical Appstore install/update checks remain pending.
The owner explicitly approved bringing Amazon to stable 1.1.2 on October 28.

An isolated checkout of `962a7d542` (the published 1.1.2 release source) was used,
so no in-progress 1.2 features enter this package. Build command:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease '-Pjellyfin.version=1.1.2' '-Plumafin.distribution=amazon'
```

- Build succeeded; 383 app unit tests passed, zero failures/errors/skips.
- Package `io.github.bagodees.lumafin`, version 1.1.2 / 1010299, min SDK 23,
  target SDK 36, four ABIs; Leanback optional for Fire OS packaging.
- APK signature verified; release certificate SHA-256:
  `A5244CB88542ECBE8E94255792B2E91512FAC4F6C7ADBF3A8F6993B475CF6855`.
- File: `LumaFin-AndroidTV-v1.1.2-firetv-release.apk`.
- APK SHA-256: `19CB678D04148A38626B82155A5E30B91AED71C3FCCF78D30D56127AAEBC34B8`.
- Update draft: `amzn1.devportal.apprelease.a898550f20ed4c19994bda56fe1dddc7`.
- Existing targeting: 91 Fire TV models, zero tablets/automotive, all available
  countries; DRM No. Existing reviewer instructions include the restricted server
  and review account; credentials are retained privately and not recorded here.
- Release notes cover the new 1.1 layout and 1.0.1–1.1.2 fixes since Amazon's
  initial 1.0.0. Replacement APK processed as code 1010299; submission completed
  October 28 and status verified **SUBMITTED**. Amazon estimates November 2,
  2026 at 05:00 AM PST. The update is not yet approved/live; 1.0.0 remains live.

Historical preparation notes below are preserved as a record of earlier stages.

## October 24: submitted

The owner submitted LumaFin 1.0.0 (code 1000101) to the Amazon Appstore on
October 24, 2026. It is waiting for Amazon's review. The draft notes below record
how the submission was prepared.

## October 23 draft status

Amazon confirmed identity/account review is complete. LumaFin app entry created;
**not submitted**. The owner explicitly requires personal review before submission.

- App ID: `amzn1.devportal.mobileapp.abef31512d6f4b1f85d1349a847a88f2`
- Draft release: `amzn1.devportal.apprelease.da93c7ff9356446e93085df227af6b3b`
- Current Fire TV APK uploaded and processed: version 1.0.0, code 1000101.
  SHA-256: `FF6786710A9DEDCF0D390B82E4BE67FA209DB54908687919BB6DFC3083DB77B2`.
  Release signature matches the existing LumaFin key; min SDK 23, target 36,
  four ABIs, Leanback optional. Build succeeded; no runtime changes in this pass.
- Fire TV targeting: 91 compatible models selected; zero tablets and zero
  automotive devices. Free, all available countries, intended audience 18+.
- Amazon-added DRM: No. English listing text saved. Privacy policy URL saved.
- Fire TV icon/background created from existing branding and a simple navy
  background in `docs/amazon-assets/`; three existing sample-media app screenshots
  uploaded. These are Android TV app screenshots, not new Fire TV hardware captures.
  Required common 512px/114px icons and screenshot fields are also populated;
  the review page reports English multimedia Complete. This does not enable tablets.
- Privacy questionnaire is **unsaved and under review**. The owner correctly
  notes that the developer receives no users' personal data. Name changed to No
  in the draft. Other preliminary collection answers must be reconciled with
  Amazon's app-level definitions before saving; do not treat them as accepted.
  Automatic approval review rejected saving the earlier proposed Analytics /
  Personalization answers. No final privacy declaration has been submitted.
- Account/data deletion answers and private reviewer-access instructions remain
  to be completed/reviewed. Do not copy credentials to Amazon without specific
  authorization for that destination. No credentials belong in this document.
- General reviewer instructions are entered, with an explicit note that credentials
  still need to be added privately. Export-compliance certification is unchecked.
  Submit App was not clicked. Review page and unsaved privacy draft left open.

The earlier preparation notes below describe the October 22 starting state and
candidate; the status and artifact above supersede those historical details.

Started October 22, 2026. No Amazon app has been submitted. The existing developer
account is signed in and its contact address is saved. Amazon requires account
identity verification before app submission; the verification page is open for
the user to complete privately. A friend reports a "Fire TV Stick 4K Manx",
interpreted as 4K Max; exact model/generation, OS and physical testing are pending.
The friend reports that the current GitHub APK is already sideloaded and works
well. Treat this as an initial compatibility report, not a completed Appstore
installation or acceptance checklist. Prioritize testing the new candidate and
Amazon Live App Testing delivery rather than assuming a runtime port is needed.

## Initial scope

Target Fire TV devices running Android-based Fire OS, not Fire tablets. Vega OS
requires a separate VPKG app and is outside this initial Android release. Confirm
the friend's model/OS before sharing a test APK. With minimum SDK 23, Fire OS 5
(API 22) is unsupported; do not lower the SDK merely to claim older-device support.

Keep package `io.github.bagodees.lumafin`, public version 1.0.0, and the existing
release key. Amazon re-signs ingested apps: do not promise seamless upgrades from
GitHub/Play installations without comparing the Amazon certificate first.

## Build

```powershell
.\gradlew.bat :app:assembleRelease '-Pjellyfin.version=1.0.0' '-Plumafin.distribution=amazon'
```

Output: `app/build/outputs/apk/release/LumaFin-AndroidTV-v1.0.0-firetv-release.apk`.
Candidate built successfully October 22: version code 1000100, min SDK 23, target
SDK 36, four ABIs (ARM 32/64 and x86 32/64), release signature verified. SHA-256:
`FAA328D21D538EC6E04A716C38D48829ABA2A7B099885016C4430F85543A23DB`.
Packaged APK declares Leanback optional; a separate standard manifest-processing
run confirmed Leanback remains required by default. This is packaging validation,
not certification on Fire TV hardware.
This option makes `android.software.leanback` optional, following Amazon's TV
manifest example. Standard builds still require Leanback. The existing MAIN,
LAUNCHER and LEANBACK_LAUNCHER intent entries remain. Select Fire TV devices only
in Amazon Console; an optional Leanback feature alone does not enforce TV-only
distribution. No new SDK, billing, analytics, account or backend is added.

Current source already suppresses website-update notices for installer
`com.amazon.venezia` as well as Google Play. The production-signed Amazon-install
path still needs testing. No direct Google Play links were found in app sources.
Speech uses Android APIs and checks availability; do not advertise Alexa/global
search integration. Android launcher channels are guarded by TV-provider support;
Amazon launcher recommendations are not implemented by that integration.

## Console preparation

- Complete Amazon identity verification privately (contact profile is saved).
- Create LumaFin as a free Fire OS app and upload the signed candidate.
- Review actual compatible models and restrict delivery to Fire TV; exclude Vega
  and untested/incompatible models. Use Live App Testing before public release.
- Support: support@getlumafin.com; website: https://getlumafin.com;
  privacy: https://getlumafin.com/privacy/.
- Complete Amazon's own privacy labels and content declarations from current app
  behavior. Do not automatically copy Google form answers into different questions.
- Provide restricted reviewer server access privately if required; no public demo
  login. Credentials must never be committed here.
- Confirm signing implications and DRM choice before final submission. The app is
  free and has no billing integration; automatic DRM is not needed for this scope.

## Listing draft

Title: LumaFin

Short description: Browse and play your Jellyfin library on Fire TV with a remote-friendly interface, customizable Home rows, subtitles and playback controls. Requires access to a Jellyfin server and account.

Long description:

LumaFin connects your Fire TV to a Jellyfin server you own or have permission to
use. Browse movies and TV shows, continue watching, explore cast details, and
customize your Home rows using your TV remote.

Playback controls include resume, seeking, subtitle options and playback
information. Available audio, subtitles and media formats depend on your server,
media files and Fire TV device.

LumaFin does not provide movies, TV shows, a media library or a streaming
subscription. You need a Jellyfin server URL and user account to get started.

Features: Remote-friendly browsing; customizable Home rows; resume playback;
audio/subtitle controls; movie, episode and cast details.

## Artwork

Amazon currently requires a 1280x720 opaque PNG Fire TV app icon, 3–10 1920x1080
landscape screenshots, and a 1920x1080 opaque background image. Reuse LumaFin's
approved logo and appropriate sample-media screenshots after Fire TV validation;
the background must not contain embedded logos or titles. Existing Google
1024x500 artwork is not the Fire TV icon. Verify requirements in the live form.

## Physical Fire TV checks (not yet passed)

1. Install/launch from the Fire TV app row; sign in with HTTPS server credentials.
2. D-pad, Select and Back across Home, Libraries, Search and details; Up from actions
   and Audio reaches Home. Keyboard search works if voice is unavailable.
3. Resume/play/restart, seek, captions, available audio tracks, volume/audio focus,
   soundbar surround and supported HDR formats; record exact model and formats.
4. Remote Home during playback stops cleanly; reopen/resume without a crash.
5. Sleep/wake and text-size changes preserve a responsive populated Home.
6. Subtitle settings show complete first-row focus at default and 130% text.
7. Store-installed update notice, update-in-place, login retention and uninstall.
8. Capture crash logs and genuine Fire TV screenshots; do not mark Android emulator
   checks as Fire TV hardware verification.

## Official references

- https://developer.amazon.com/docs/fire-tv/differences-from-android-tv-development.html
- https://developer.amazon.com/docs/fire-tv/add-tv-support-to-an-existing-react-native-project.html
- https://developer.amazon.com/docs/app-submission/upload-app-file.html
- https://developer.amazon.com/docs/app-submission/device-filtering-and-compatibility.html
- https://developer.amazon.com/docs/app-submission/appstore-details.html
- https://developer.amazon.com/docs/app-submission/presubmission-checklist.html
