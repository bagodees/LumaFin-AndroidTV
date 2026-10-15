# Home card appearance

All supported Home content sections use `CardPresenter.homeRows()`, regardless
of whether the section is enabled. This supplies the landscape card size and
card-size preference, two-line 15sp semibold titles below artwork, 11sp secondary
text immediately below the title, and a fixed caption-area height with spare
space at the bottom. Library cards omit duplicate captions but reserve the same
space. The Home row presenter supplies common horizontal spacing and focus zoom.

Covered section families:

- Continue Watching (video/audio), Up Next and the combined row.
- Latest Media, Latest Movies/Shows and Recently Added Movies/Shows.
- Recommendations, Watch Again, Collections, Favorites and Watch Later.
- Library tiles/buttons and pinned Home shortcuts.
- Latest/recent albums, artists, music videos, books and audiobooks.
- Active recordings and Live TV programs; Live TV navigation buttons also bind
  through the shared presenter while retaining their original click objects.
- The Home audio Now Playing queue uses the Home presenter; the separate audio
  player queue keeps its existing appearance.
- Browse by Genre uses its random landscape artwork with matching card dimensions,
  typography and caption height.

Notifications are informational banners, not configurable content sections.
Resume Book remains unsupported and None creates no row; this appearance work
does not add data support for either.

The enabled movie, TV, recommendation, collection, library and genre rows were
reviewed in the emulator during development. Disabled music/book/Live TV rows
share the styling paths above; data-dependent visual checks require those media
or server features and have not been performed for every section.
