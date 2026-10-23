# Google Play release version

The Google Play closed test uses public version **1.0.0**. The first bundle used code
**1000099**; TV fixes used **1000100**, and the library-footer update uses **1000101**. `lumafin.releaseVersionCode` in
`gradle.properties` supplies the code when building `lumafin.releaseVersion`, so
test updates can retain the planned 1.0.0 public version. Other versions keep the
semantic-version calculation. The configured release code cannot be lower than
that calculation; increase it for every Play upload and check the Console first.

Build the release app bundle with the configured release signing credentials:

```powershell
.\gradlew.bat :app:bundleRelease '-Pjellyfin.version=1.0.0'
```

Release APK and app-bundle packaging require a real version matching `lumafin.releaseVersion` in `gradle.properties`. A missing development version or a different supplied version is rejected. The existing `JELLYFIN_VERSION` environment override also has to match.

Ordinary debug builds retain their development version and display the upcoming `lumafin.nextVersion` (now 1.0.0). Before packaging a later public release, update the release target and provide that version explicitly. Do not reuse a version code already uploaded to Play.

This configuration does not create a release tag, upload an app bundle, or publish to Google Play.
