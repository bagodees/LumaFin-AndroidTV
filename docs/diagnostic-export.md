# Diagnostics export (#26)

Settings > Help & Maintenance > Export diagnostics (also indexed in settings search) prepares a local report on request. Select a problem and its trigger, review all sections with the remote, then save the exact reviewed snapshot.

## Privacy and limitations

The report uses an allowlist of typed settings. It never collects logs, names, account/server/device IDs, addresses, media metadata, file paths, passwords or tokens. Version fields are accepted only when they match a bounded numeric version with a known release suffix; unexpected text is omitted whole. No network requests or automatic uploads are added.

Includes app/Android/server versions, basic connectivity indicators, selected playback settings and advertised AVC/HEVC/AV1 decoder support. Unknown values remain unavailable. These are not server-reachability, certificate, speed or actual playback tests. Existing Playback information and Test streaming speed remain separate opt-in tools.

Problem/trigger choices provide initial reproduction context. Users can add exact steps and expected/actual results to the saved text file before sharing it themselves.

Android 10+ saves to Downloads/LumaFin through MediaStore, publishing only after a successful write and removing an incomplete entry on failure. Android 6–9 uses the system document picker; a missing picker produces an explicit message. No storage permission or sharing service was added. Leaving before Save creates no export.

## Validation

- Debug assembly passed, final build installed on emulator-5554.
- 11 focused tests passed: DiagnosticExportTests (6) and SettingsSearchTests (5). Tests cover secret/URL/newline/version rejection, unavailable values, bounded bitrate, exact UTF-8 output, stream closure, missing output and write failure.
- On the test account: entry from Help, Cancel and revisit, problem/trigger selection, remote scrolling through review, no file before Save, successful Downloads export, readable saved confirmation, and return to Help all worked.
- Retrieved and inspected the first saved file: selected BUFFERING/SEEK context and technical values matched the review; no private values were present.
- Git whitespace check passed.
- Physical TV, Android 6–9 document picker, and actual storage-full/permission failures were not exercised. Writer failures are tested with controlled streams.

Implemented alongside grouped search (#54) and the optional setup guide (#28).
