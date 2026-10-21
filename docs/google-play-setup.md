# Google Play setup

Status checked October 21, 2026. No release has been uploaded or sent for review.

## Saved declarations

- Package: `io.github.bagodees.lumafin`; planned release: 1.0.0.
- Privacy policy: https://getlumafin.com/privacy/
- Reviewer instructions saved in Play Console, with access limited to Demo-Movies and Demo-TV. Credentials belong only in Play Console, never this repository.
- No ads; intended audience 18 and over (separate from content rating).
- IARC: All Other App Types, no bundled or developer-promoted catalog. Returned ESRB Everyone and PEGI 3.
- Not a government app; no financial or health features.
- Category: Video Players & Editors.
- Public contact: support@getlumafin.com; website: https://getlumafin.com.

## Data safety

Saved in Play Console, pending submission/review. Google defines collection as transmission off the device, including to a user's chosen server. This differs from the privacy policy's statement that the LumaFin developer does not receive personal data.

| Data type | Optional | Purpose |
| --- | --- | --- |
| User IDs | No | App functionality, account management |
| Device or other IDs | No | App functionality |
| App interactions (playback activity/progress) | No | App functionality, personalization |
| In-app search history (queries sent to the server) | Yes | App functionality |
| Other actions (favorites and explicit watched-state changes) | Yes | App functionality, personalization |
| Crash logs | Yes | Analytics (Google's classification for crash diagnosis) |
| Diagnostics | Yes | Analytics (technical diagnosis) |
| Voice or sound recordings | Yes | App functionality |

No ephemeral-processing exemption claimed: server and speech-provider retention cannot be guaranteed by this app. No third-party sharing declared under Google's user-initiated transfer exception: users choose their Jellyfin server, explicitly invoke voice input or an external player, and optional crash reporting is described in settings and the privacy policy. Reassess if destinations or consent flows change.

No claim that all data is encrypted in transit: the app supports HTTP Jellyfin servers (`usesCleartextTraffic=true`). HTTPS is supported and recommended, and update checks use HTTPS, but the Play preview consequently displays "Data isn't encrypted."

The app does not create accounts; it signs into accounts created by the administrator of the user's independently operated Jellyfin server. No developer-run data-deletion request mechanism claimed. Users can clear local data and ask their server administrator to remove server-side data as described in the policy.

Evidence reviewed: `TelemetryService.kt`, `TelemetryPreferences.kt`, `SessionRepository.kt`, `AppUpdateChecker.kt`, `speechRecognizer.kt`, `ReportingHelper.kt`, `SearchRepository.kt`, the manifest, and the live privacy policy.

Official definitions: https://support.google.com/googleplay/android-developer/answer/10787469

## Remaining

- Store listing saved and ready to send for review: LumaFin text, icon, 1024x500 feature graphic, 1280x720 TV banner, and three current screenshots (Home, library, playback). The general gallery also uses these TV screenshots to satisfy Console validation; no phone support is claimed.
- Feature graphic and TV banner generated from the existing wordmark; labeled as AI-created/edited in Console. Their source is `docs/branding/lumafin-play-feature-source.png`. Screenshots have media credits in the description.
- Android TV opt-in confirmed with the user's explicit approval. No review submission made.
- Manifest requires `android.software.leanback` to restrict installation to TV devices. Verify the uploaded bundle's actual device catalog before release; Android XR was enabled by Console by default, not explicitly added.
- Existing fastlane images still contain upstream branding. Do not automate uploads from that directory until those images are replaced with the approved LumaFin assets.
- Signed 1.0.0 bundle built successfully, including release lint: `app/build/outputs/bundle/release/LumaFin-AndroidTV-v1.0.0-release.aab` (version code 1000099). Merged release manifest confirms required Leanback support.
- Bundle signing certificate SHA-256 matches the existing 0.2.1 release APK: `A5:24:4C:B8:85:42:EC:BE:8E:94:25:57:92:B2:E9:15:12:FA:C4:F6:C7:AD:BF:3A:8F:69:93:B4:75:CF:68:55`.
- All inspected arm64-v8a and x86_64 native ELF LOAD segments have 16384-byte alignment. Actual Play device compatibility and bundle delivery validation remain pending upload.
- User approved securely exporting the existing release key to Google Play and completed the encrypted ZIP upload and Save. Verified Console's app-signing fingerprint matches the certificate above. No private key or password is stored in this repository.
- Closed-testing Alpha release draft saved, named `1.0.0 - first closed test`, with English release notes and bundle 1000099 uploaded. Bundle SHA-256: `1F7EC9989116AE00123D71A4E57A1DA947B7F3987DC4D54157280D2F0CD1BA0E`.
- Google reports support for 16 KB pages and four ABIs (arm64-v8a, armeabi-v7a, x86, x86_64). Device Catalog terms accepted with explicit user approval. All 3,075 supported models remain when filtered to TV, confirming TV-only targeting without manual exclusions.
- United States-only closed-test availability saved with explicit user approval. Release preview now has no errors and two warnings (no testers; optional native debug symbols missing). Release saved to Publishing overview, ready for review submission. Nothing submitted or rolled out.
- Tester list remains unconfigured; the user does not yet have 12 testers. Gather their Google account emails or a suitable Google Group before starting the closed test.
- Finish pending physical-TV checks, including playback and Search gradient fixes.
- Configure closed testing. The dashboard requires at least 12 testers opted in continuously for 14 days before applying for production access.
- Obtain approval before sending the release for review or publishing.
