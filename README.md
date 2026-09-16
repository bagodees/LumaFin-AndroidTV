<h1 align="center">LumaFin</h1>
<h3 align="center">A personalized Android TV client for <a href="https://jellyfin.org">Jellyfin</a></h3>

---

<p align="center">
<a href="https://github.com/bagodees/LumaFin-AndroidTV/blob/lumafin-main/LICENSE">
<img alt="GPL 2.0 License" src="https://img.shields.io/github/license/bagodees/LumaFin-AndroidTV.svg"/>
</a>
<a href="https://github.com/bagodees/LumaFin-AndroidTV/releases">
<img alt="Current Release" src="https://img.shields.io/github/v/release/bagodees/LumaFin-AndroidTV.svg"/>
</a>
</p>

LumaFin is a personal fork of [jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv), the official Jellyfin
client for Android TV, Nvidia Shield, and Amazon Fire TV devices. It tracks upstream Jellyfin for bug fixes and new
features while layering on its own branding and home screen customizations, styled after a [JellyFrame](https://jellyfrom.tv/)
webui setup using the "Better Jellyfin UI" theme by tromoSM, the "Rating Badges" mod by grimmdev, and the
[jellyfin-plugin-home-sections](https://github.com/IAmParadox27/jellyfin-plugin-home-sections) plugin by IAmParadox27.

This is not an officially supported Jellyfin project - it's built for personal use and shared as-is. See
[LICENSE](LICENSE) for license details, and the upstream project at
[jellyfin/jellyfin-androidtv](https://github.com/jellyfin/jellyfin-androidtv) for the official Android TV client.

## What's different from upstream

- **Landscape, thumbnail-first artwork** - content cards throughout the app use a Thumbnail → Backdrop → Primary
  image priority and render as uniform 16:9 tiles, matching the JellyFrame webui look instead of per-type portrait
  posters. Continue Watching and Recently Added correctly show series artwork for TV episodes.
- **Rating badges** - movie and series cards show critic (Rotten Tomatoes-style) and community rating badges,
  matching the webui's grimmdev Rating Badges mod.
- **More home section types** - in addition to the built-in sections, you can add Latest Movies, Latest Shows,
  Because You Watched, Collections, Watch Again, and Genre rows to the home screen.
- **Live home section reordering** - changes to home section layout in Settings apply immediately, no app restart
  required, and are stored locally on-device so they can't be reverted by a session refresh or another Jellyfin
  client.
- **LumaFin branding** - its own icon, Android TV banner, splash screen, and accent color theme, with its own
  application ID so it can be installed alongside the official Jellyfin app.

## Building

The app uses Gradle and requires the Android SDK. We recommend using Android Studio, which includes all required
dependencies, for development and building. For manual building without Android Studio make sure a compatible JDK
and Android SDK are installed and in your PATH, then use the Gradle wrapper (`./gradlew`) to build the project with
the `assembleDebug` Gradle task to generate an apk file:

```shell
./gradlew assembleDebug
```

The task will create an APK file in the `/app/build/outputs/apk/debug` directory.

## Branching

The `lumafin-main` branch is the active development branch and default branch for this fork, carrying all LumaFin
customizations. The `master` branch tracks upstream `jellyfin/jellyfin-androidtv` unmodified, to make pulling in
upstream changes straightforward.
