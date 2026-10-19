# Dialogue clarity investigation (#52)

## Decision

Keep the existing off-by-default night mode; do not add a separate dialogue
boost or change its DSP tuning without measured output and listening evidence.
The audit and lifecycle repair do not establish improved speech intelligibility.
Physical output evaluation remains open in #52 and the TV checklist (#68).

## Existing processing

The standard player's VideoManager snapshots audioNightMode when created and
attaches an Android audio effect when its audio session arrives. Android 9+
uses DynamicsProcessing: a linked all-channel limiter, 30 ms attack, 300 ms
release, 10:1 ratio, -24 dB threshold and +3 dB post gain. The pre-EQ gains are
0, 0.02, 0.03, 0.02 and 0 dB. Older Android uses only Equalizer with corresponding
0, 2, 3, 2 and 0 millibel gains. These are very small EQ changes, not a selective
voice extractor. The existing values are preserved by this change.

For illustration, an ideal settled compressor using those limiter parameters
would map -30 dB to -27 dB, -24 dB to -21 dB, -12 dB to -19.8 dB and 0 dB to
-18.6 dB. This is an algebraic static curve, not a measurement of Android's
implementation, attack transients, clipping or perceived dialogue clarity.

The experimental player's ExoPlayerAudioPipeline uses LoudnessEnhancer for the
queue entry's normalization gain. It does not consume audioNightMode. Increasing
this gain would not isolate dialogue. External players own their processing.
Encoded passthrough/offload routes may bypass session effects; the application
must not claim successful processing solely because an effect was constructed.

## Repair

Night-mode effect ownership moves from a process-global variable to each
VideoManager instance. Replaced sessions and destroyed players release it;
invalid IDs never attach to the global mix. Unsupported effect creation or
configuration falls back to normal playback, and partial configuration releases
its allocated effect. No new preference, gain, forced decoding, downmix or server
conversion is introduced. The setting now states player/version/output limits
and that a new playback session is required.

## Evidence needed before a new control

Use identical speech-and-effects clips at fixed TV/receiver settings, comparing
off/on at matched dialogue loudness, not just a louder master volume. Record
player, source channels/codec, decoded output route and effect availability.
For decoded stereo and multichannel separately, capture output where the test
equipment allows it: compare speech/background loudness, peak/clipped samples,
pumping, noise and channel balance. Include near-full-scale transients, quiet
speech under music, track changes, pause/resume and a second playback session.
Compare passthrough and external output as bypass cases rather than promising
processing. Retain defaults if there is distortion, instability or no meaningful
benefit. A center-channel boost would need verified channel mapping and a
headroom/limiting design; stereo speech isolation is a separate DSP problem.

## Sources

- [Android DynamicsProcessing](https://developer.android.com/reference/android/media/audiofx/DynamicsProcessing)
- [Limiter parameter units](https://developer.android.com/reference/android/media/audiofx/DynamicsProcessing.Limiter)
- [LoudnessEnhancer](https://developer.android.com/reference/android/media/audiofx/LoudnessEnhancer)

## Validation

The debug build and unit suite pass, including lifecycle coverage for session
replacement, duplicate callbacks, unsupported effects, invalid IDs, independent
owners and failing release. Unit coverage does not measure DSP output. Emulator
playback checks and physical output results are recorded in the issue.

Emulator test account: Pioneer One playback created enabled Dynamics Processing on session 105, with stereo 48 kHz PCM float input/output. After stopping, that session's effect chain was absent. A subsequent Sprite Fright session created a separate effect on session 113. No AndroidRuntime failure appeared. Night mode was restored to Off; the final APK is installed. These observations establish lifecycle behavior, not audible benefit or freedom from clipping. All 304 app unit tests passed.
