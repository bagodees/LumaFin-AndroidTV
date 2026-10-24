# Google Play release version

The next Google Play closed-test release is **1.0.1**, code **1000199**,
including the Dolby TrueHD surround fallback fix (#106). Version 1.0.0 used codes
**1000099**, **1000100** (TV fixes), and **1000101** (library footer). `lumafin.releaseVersionCode` in
`gradle.properties` supplies the code when building `lumafin.releaseVersion`, so
test updates can retain the planned public version. Other versions keep the
semantic-version calculation. The configured release code cannot be lower than
that calculation; increase it for every Play upload and check the Console first.

Build the release app bundle with the configured release signing credentials:

```powershell
.\gradlew.bat :app:bundleRelease :app:assembleRelease '-Pjellyfin.version=1.0.1'
```

Release APK and app-bundle packaging require a real version matching `lumafin.releaseVersion` in `gradle.properties`. A missing development version or a different supplied version is rejected. The existing `JELLYFIN_VERSION` environment override also has to match.

Ordinary debug builds retain their development version and display the upcoming `lumafin.nextVersion` (now 1.0.1). Before packaging a later public release, update the release target and provide that version explicitly. Do not reuse a version code already uploaded to Play.

This configuration does not create a release tag, upload an app bundle, or publish to Google Play.
