# Server streaming speed (#53)

Status: implemented and accepted by the user.

Help & Maintenance → Test streaming speed uses a fresh API client bound to the
active server and user. Settings search also finds the screen. Start is explicit;
no background tests or external speed-test services are used.

The test inspects up to 20 accessible movie/episode/video items and samples the
largest suitable local media source from those results. Live/remote sources and
files smaller than 1 MiB are excluded. It requests a static server video stream,
without starting playback, transcoding or reporting progress. Headers request
uncached, uncompressed bytes with a range capped at 64 MiB; the reader enforces
the cap even if the server ignores Range. Transfer sampling lasts at most ten
seconds; connection/read/whole-operation timeouts prevent hanging tests.

Cancellation cancels the HTTP call and closes its response. Leaving the screen or
changing account/server cancels the operation and clears results. Redirects are
rejected, preventing credentials from following a redirect to another host. Raw
errors, authenticated URLs and tokens are neither displayed nor logged.

Results show average Mbit/s, bytes, elapsed time and sampled title. Completed
one-second intervals provide variation when at least two are available. Samples
under two seconds are labelled short. Where available, the source bitrate and
configured quality limit provide context; the headroom comparison uses 1.5 times
the reported bitrate/limit as a simple explanatory margin, not a playback promise.
No preferences are changed. Empty/no suitable media and transfer failures have
readable messages and can be retried explicitly.

Validation: debug build and six focused tests pass. Tests cover byte/time limits,
speed calculation/intervals, empty/failing streams, cancellation, HTTP errors,
HTML rejection, redirect rejection and prompt cancellation of an in-flight call.

Emulator acceptance: Jarvis returned a 64 MiB sample in 5.5 seconds, averaging
97.9 Mbit/s with one-second intervals ranging from 36.7 to 134.7 Mbit/s. Source
bitrate was 76.7 Mbit/s and the screen correctly explained limited headroom.
Back returned to Help & Maintenance. The final build adds focusable result
sections for remote scrolling, and an attempt generation guard so cancelled or
superseded work cannot replace a newer result or leave the UI in a running state.
User acceptance confirmed Cancel works and remote navigation reaches the final
explanation. User screenshots show a 64 MiB transfer in 5.0 seconds averaging
107.7 Mbit/s, with interval variation and bitrate context displayed correctly.

Limitations: this is a short sample of the current server route and one file,
not general internet speed or a guarantee for all titles. Server/proxy caching,
bitrate peaks, network changes, server conversion and device decoding can affect
playback separately. Libraries with no suitable source in the first 20 returned
items receive an explicit unavailable message rather than scanning everything.
