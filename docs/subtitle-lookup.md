# Server subtitle lookup (#49)

Get more subtitles lives in the standard built-in player's subtitle picker,
including titles with no existing subtitles. The remote Captions key opens the
same picker. Existing details subtitle chips also offer lookup when that details
layout exposes track chips, so downloads can be prepared before playback.
No permanent new player button or client-side provider account is added.

## Eligibility and server contract

Offer lookup only for movies/episodes, a known Jellyfin version >= 10.9 and a
current user whose policy explicitly enables subtitle management. The server
still authorizes each request. Administrator status alone is not assumed to
override the specific permission. Providers and their credentials stay managed
on the server; an empty response does not prove that no provider is installed.

The implementation uses SDK 1.8.12 SubtitleApi searchRemoteSubtitles and
downloadRemoteSubtitles, and UserLibraryApi.getItem to refresh tracks. The GET
and POST routes and subtitle-management policy were checked in Jellyfin 10.9.11
and 10.10.7 source. Unsupported/status failures are explained rather than
guessing successful capability. No server plugins or permissions are changed by
the app.

Download has no mediaSourceId parameter. For a local selected version, its
server-returned UUID source ID addresses that version's item; unknown/non-UUID
sources show unavailable instead of silently downloading for a grouped title.
Before POST, retrieve the target using the current user and verify that its
sources include the reviewed source. Every request uses an independent API client
bound to the captured account/server, with checks before and after suspension.
Changing title/source/account or leaving the invoking view ends the flow.

## User flow and limits

Choose a language (initially the configured preference, otherwise TV language),
then a provider result. Display at most the first 100 usable unique results,
with file-match, format and hearing-impaired/forced labels when supplied. A
confirmation explains that download changes the server library and other users
may see the added subtitle. There is no automatic download or provider signup.

Search has a 30-second deadline and can be cancelled/retried. Download and
verification have a 45-second deadline. Submit exactly one POST, then poll
metadata at most twelve times for a new external subtitle in the selected
source/language. A 204 alone is not reported as success: older server code can
swallow a download exception and queue asynchronous metadata refresh. A new
track is evidence of availability, not proof of subtitle quality or exact sync.

No automatic retry follows a submitted write. Timeout, cancellation or delayed
metadata may leave a server download running; the UI tells users to recheck the
title before trying again. Raw errors, provider IDs, tokens and authenticated
URLs are not shown or logged by this flow.

On details, verified tracks refresh the existing chip choices. During standard
playback, an explicit Refresh playback action reloads tracks at the current
position; it does not automatically select a subtitle or restart video without
that action. Existing subtitle/audio selection behavior remains authoritative.
The experimental player currently has no subtitle-selection UI; this change
does not invent one or claim external-player hot reload.

## References

- [Jellyfin 10.9.11 SubtitleController](https://github.com/jellyfin/jellyfin/blob/v10.9.11/Jellyfin.Api/Controllers/SubtitleController.cs)
- [Jellyfin 10.10.7 SubtitleController](https://github.com/jellyfin/jellyfin/blob/v10.10.7/Jellyfin.Api/Controllers/SubtitleController.cs)
- [Subtitle permission handler](https://github.com/jellyfin/jellyfin/blob/master/Jellyfin.Api/Auth/UserPermissionPolicy/UserPermissionHandler.cs)
- [SDK SubtitleApi](https://kotlin-sdk.jellyfin.org/dokka/jellyfin-api/org.jellyfin.sdk.api.operations/-subtitle-api/index.html)

## Validation

Automated coverage exercises title/permission/version gating, selected-version
UUID mapping, missing versions, delayed metadata, unconfirmed 204 responses,
source/language/embedded-track isolation, cancellation and account/title changes,
single-POST behavior, invalid languages and usable/unique result filtering.
Live provider results and remaining hardware acceptance are recorded in the issue.
