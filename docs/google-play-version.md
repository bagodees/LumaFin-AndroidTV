# Google Play release version

The first planned Google Play release is **1.0.0**, with version code **1000099** from the existing semantic-version calculation.

Build the release app bundle with the configured release signing credentials:

```powershell
.\gradlew.bat :app:bundleRelease '-Pjellyfin.version=1.0.0'
```

Release APK and app-bundle packaging require a real version matching `lumafin.releaseVersion` in `gradle.properties`. A missing development version or a different supplied version is rejected. The existing `JELLYFIN_VERSION` environment override also has to match.

Ordinary debug builds retain their development version and display the upcoming `lumafin.nextVersion` (now 1.0.0). Before packaging a later public release, update the release target and provide that version explicitly. Do not reuse a version code already uploaded to Play.

This configuration does not create a release tag, upload an app bundle, or publish to Google Play.
