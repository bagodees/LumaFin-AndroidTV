# Quick setup guide (#28)

A skippable guide appears once per server/user after sign-in, including existing accounts after upgrading. It links to existing Home layout, subtitle appearance and autoplay settings, with subtitle language guidance. No defaults are changed by opening or skipping it.

Revisit from Settings > Help & Maintenance > Quick setup guide, or settings search. Presentation is recorded per account immediately so Back, skipping or an interrupted visit does not force it again.

## Validation

- Debug build succeeded and was installed on emulator-5554.
- 14 focused tests passed: SetupGuideTests (2), AccountPreferencesTests (7), SettingsSearchTests (5).
- On the test account: first presentation appeared; Skip closed it; force-stop/restart did not repeat it; ordinary Settings opened at its root.
- Reopened through Help & Maintenance; Home layout, subtitle guidance/appearance and autoplay opened correctly; Back returned to the guide.
- Remote navigation reached Done and returned to Help & Maintenance. Existing subtitle brightness remained at 80%; no playback or server changes were made.
- Git whitespace check passed.

Physical TV and a separate live server/account first presentation were not tested; account/server isolation is covered by unit tests.
