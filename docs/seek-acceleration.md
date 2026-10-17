# Held timeline seeking (#29)

Implemented and validated for #29.

Both internal player timelines retain the user's independent forward/back intervals for a fresh press. Android repeated key-down events use elapsed time since that press: normal below one second, 2x from one second, 4x from three seconds, and 8x from five seconds. A released/new press starts at normal speed. Timing does not depend on the remote's repeat count or rate. Existing endpoint bounds remain in effect.

The original Leanback timeline also applies the policy to its existing rewind/fast-forward keys. Accessibility seek actions keep their normal interval. The Compose seekbar enables acceleration only for PlayerSeekbar; generic controls and settings sliders keep their existing behavior. No new setting or persistent preference is added.

## Validation

- Debug build and seven focused unit tests passed (SeekAccelerationTests and DirectionalSeekTests): threshold boundaries at different repeat rates, fresh presses, independent configured intervals, preview positions and endpoint bounds.
- Emulator account picker explicitly confirmed `test` as current before playback.
- Original player, test-account Charge: single Right preview 0:00 to 0:30; a repeat at 1.2 seconds moved 1:30 to 3:00 (30 + 60 seconds); fresh Left moved 3:00 to 2:50; a repeat at 3.2 seconds moved 2:50 to 2:00 (10 + 40 seconds); a repeat at 5.2 seconds clamped forward to the 4:22 endpoint. Back left the seek preview; subsequent Back stopped playback.
- New player, test-account Charge: a timed held Right moved 0:30 to 2:00 (30 + 60 seconds). A settled fresh Left returned to the normal ten-second step, 2:00 to 1:50. Test-account Sprite Fright was also exercised with continuous repeated Left events without leaving the timeline.
- Temporary shell helper under ignored build output sends actual Android repeated KeyEvents; the standard emulator input long-press command alone does not exercise all timing thresholds. Helper is not packaged with the app.
- Original internal-player preference restored after checks. Physical TV remotes were not tested.
