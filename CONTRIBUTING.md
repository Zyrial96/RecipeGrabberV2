# Contributing

Use JDK 17, the checked-in Gradle wrapper and the SDK versions in the README. Complete onboarding without credentials for local feature work. Unit tests and CI must not require private API keys.

Before submitting changes:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
git diff --check
```

Reports are in `app/build/reports/`. Release builds are unsigned. Run the Room and Compose instrumentation suite with `./gradlew connectedDebugAndroidTest` on an authorized device/emulator. It verifies independent detail IDs, rollback on failed writes, favorites, cascading deletion, localized portion controls and delete confirmation/navigation using isolated in-memory databases, not the user's recipe database. CI runs this suite on API 31 and 35 emulators. Passing it is not proof of external API authorization or actual video extraction.

## Architecture And Scope

- Keep Compose/ViewModels separate from networking and storage.
- Route extraction through `ExtractRecipeUseCase`; never save directly from a screen or duplicate its save operation.
- Preserve coroutine cancellation. Network/parser errors must not become successful empty recipes.
- Test actual media request structure for new providers. A URL inside a text prompt is not video ingestion.
- Validate generated fields and retain the submitted source URL. Room-generated detail IDs must not be replaced by per-response indexes.
- Schema changes require migrations and tests preserving recipes. Do not use destructive migrations as a shortcut.
- Preserve unrelated edits and project instructions. Verify live source instead of trusting the historical GSD map alone.

## Tests And Device Evidence

Add regressions for changed behavior, including malformed responses, missing credentials and cancellation where relevant. Use small synthetic fixtures without personal data. Do not call paid APIs from unit tests or pull-request CI.

For device tests, record model, Android API level, build/commit and observed flow. Distinguish an opened OAuth dialog, granted permissions, a usable token and successful extraction. Do not claim the latter from the former.

Xiaomi may restrict USB installation and input separately. Normal APK download/package installation can support development without root or globally disabling Play Protect. Report actual device restrictions rather than substituting a successful host build.

## Issues And Pull Requests

Include expected/actual behavior, reproduction steps, Android version, app commit and a redacted error. Never attach keys, private video URLs, account emails or unreviewed full logcat dumps. For OAuth errors, check package/signing registration rather than posting secrets.

Keep changes focused and describe verification and untested dependencies. Documentation must describe shipped behavior, not planned features. Contributions are covered by the repository's MIT license.
