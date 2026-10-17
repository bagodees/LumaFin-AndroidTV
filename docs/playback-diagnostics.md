# Optional playback diagnostics (#64)

Status: implemented and checked locally; not pushed.

Both existing internal-player information surfaces are extended. No new permanent
player button, server polling, automatic reports, or buffer strategy is added.

The legacy dialog reads Media3 totalBufferedDuration directly, avoiding the
legacy progress-bar fallback that can treat an unknown buffer as full duration.
It shows current video decoder dropped/rendered counters, buffering state, and
the min/max/start/rebuffer policy captured when this player was configured.
An app-main-thread Handler refreshes once per second while the dialog is visible;
dismissal and player-host teardown remove its callbacks. Text is replaced only
when values actually change. The existing scroll area preserves remote access.

The newer player backend exposes an optional diagnostics snapshot through the
core backend interface. Unsupported backends default to nullable/unavailable
fields. The Media3 implementation supplies the same observations and its actual
immutable creation options, using Media3 defaults where an option is absent.
The existing overlay polls once per second only while composed; leaving closes
that coroutine. It has bounded height, remote scrolling and Back dismissal.
The stream snapshot is initialized from the current queue entry when opened.

No measured value is invented for an unavailable metric. Decoder counts may
reset during decoder changes and do not measure TV display refresh or all visual
stutters. Buffered time is current media ahead, not a network-speed measurement.
The panel explains the difference between buffer targets and guaranteed preload,
and directs users to the existing server streaming-speed test. Playback →
Advanced → Troubleshooting also links to that test.

## Buffer audit

Legacy and newer Media3 players already use the existing Auto/Large/Extra Large
options. Legacy Large is 50/120 seconds with 2.5/5-second start/rebuffer
thresholds; Extra Large is 80/240 seconds with 5/10-second thresholds. Auto uses
the bundled Media3 defaults; the panel reads those constants, rather than claiming
a default from a different library version. The newer backend uses its supplied
options or the same defaults. External apps own their buffering and expose no
equivalent counters here.

Buffer policy is unchanged. Increasing preload can increase memory and startup
waiting, so no larger default or new read-ahead strategy is proposed without a
separate device/bitrate-specific memory and startup study. The diagnostic UI does
not continuously collect metrics while hidden.

## Validation

Debug build and two focused tests pass. Tests distinguish unavailable/invalid
buffer values from measured zero, preserve decoder zero counts, and check safe
formatting independent of stream URLs.

All playback acceptance uses the user's **test** account. The saved account was
selected in Who's watching before opening any media. Legacy Pioneer One S1:E2
played directly, with H.264/AAC, 52.2 seconds buffered ahead, zero dropped frames,
1473 rendered frames while paused and 50/50-second active buffer targets. Remote
scrolling reached the full explanation and speed-test guidance. Back closed the
dialog and returned to playback; stopping returned to details/Home.

Newer-player Sprite Fright playback verified the existing panel opens immediately
from the current queue stream. Live observations included 50.7–56.2 seconds
buffered ahead, zero dropped frames, and rendered counts increasing from 422 to
1696. Android D-pad scrolling reached the full buffer thresholds, decoder caveat,
and speed-test guidance. Back dismissed the overlay while playback continued;
the next Back stopped playback. Screenshots are retained in the ignored build
directory. The original internal player choice is restored after testing; the
account remains test. These emulator observations do not benchmark physical-TV
decoder performance or measure dropped display frames.
