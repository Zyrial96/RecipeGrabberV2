# RecipeGrabberV2

Native Kotlin Android app for turning cooking videos into structured recipes, storing them locally, and adjusting portions. This is an early-stage public project, not a production-ready service. Download and user numbers have not been verified.

## Current Capabilities

| Area | Implementation and limits |
| --- | --- |
| Local recipes | Room storage, search, favorites, sharing and portion scaling. Saved recipes are readable offline; extraction needs network access. |
| Gemini video input | Public YouTube URLs remain fileData. Direct HTTPS MP4/WebM/MOV files are downloaded with a 100 MiB cap; files up to 8 MiB use bounded inline video data, larger files use Files API transfer. Real YouTube, 7.5MB inline WebM and 21.2MB Files API WebM imports are verified with OAuth on the Mi 11, including persistence/restart. Larger files require the separately optional Google upload permission. These specific checks do not establish every format, origin or model. See the [media audit](.planning/audits/2026-10-13-files-api-verification.md). |
| TikTok / Instagram | Optional Apify scrapers attempt to obtain direct media URLs. Platform pages are not video files. Scraper reliability and returned CDN URL compatibility are not verified end to end. |
| Other AI adapters | OpenAI, Claude and Kimi adapters exist, but send URL text instead of actual video content. Their output is not verified video extraction. |
| Google OAuth | Gemini has an AuthorizationClient consent flow. Google Cloud registration for the exact package/signing certificate is required. An account picker is not proof of API authorization. |
| Google Drive | Optional manual versioned snapshots and confirmed restore in the app-specific Drive folder. Existing recipe UUIDs/local edits are kept. No automatic sync, scheduling, deletion propagation or cloud overwrite. See the [Drive audit](.planning/audits/2026-10-13-drive-integration.md) for exact acceptance status. |
| Clipboard | Foreground monitoring is subject to Android clipboard/background restrictions; reliable background detection on all devices is not established. Manual URL entry and Android sharing are available. |

Always compare an extracted recipe with its source. Structural validation cannot prove that a model extracted the actual ingredients, quantities or preparation correctly.

## Build And Test

Use JDK 17, Android SDK platform 35 and build-tools 34.0.0. Supported devices run Android 12 / API 31 or newer. Use the checked-in Gradle 8.7 wrapper, not system Gradle or a newer JDK.

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
sdkmanager "platforms;android-35" "build-tools;34.0.0"
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Open the project root in Android Studio, or install app/build/outputs/apk/debug/app-debug.apk. The release task builds an unsigned, minified APK; it does not configure signing or publish a release.

The [Android Checks workflow](.github/workflows/android.yml) runs unit tests, lint and both builds on pushes and pull requests, followed by Room and Compose instrumentation tests on API 31 and 35 emulators. Its artifacts contain test/lint reports, not live authorization results. Run the instrumentation suite locally with `./gradlew connectedDebugAndroidTest` on a device or emulator.

## Authentication

Credentials are optional for browsing recipes and completing onboarding. For Gemini, configure an API key or Google OAuth in Settings. API use may incur provider charges.

New Gemini selections default to [Gemini 3.1 Flash-Lite](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite), which supports video and structured output. Gemini 3.8 Flash remains selectable. Saved model choices are not silently migrated. Existing Gemini 2.5 Flash selections remain available, but Google limits 2.5 access to previous users; see the [model availability notice](https://ai.google.dev/gemini-api/docs/deprecations). Model selection does not fix missing OAuth registration.

On 13 October 2026, Google OAuth and a public YouTube cooking-video extraction using Flash-Lite were verified on a Xiaomi Mi 11 (Android 12). The recipe, four ingredients and five steps survived an app restart, and portion scaling was checked on the imported recipe. This is evidence for that specific flow, not a guarantee of extraction accuracy or availability for all videos/models. Gemini 3.8 requests on the same device had earlier returned HTTP 503 or timed out; no automatic model substitution or retries were added. See the [device audit](.planning/audits/2026-10-13-google-oauth-registration.md).

OAuth requires an enabled Generative Language API, configured consent screen and Android OAuth client matching package com.recipegrabber and the actual installed signing certificate's SHA-1. Use `./gradlew signingReport`; another developer's debug certificate is not interchangeable. Add authorized test users while the consent screen is in testing. A separate audit package needs its own registration.

YouTube and small inline-video requests use only the Generative Language scope. Google's [live Files API discovery metadata](https://generativelanguage.googleapis.com/$discovery/rest?version=v1beta) lists `devstorage.read_only` for upload/get/delete methods. In Settings, **Authorize video uploads** offers this broader Google Cloud Storage permission separately, with an explicit disclosure before Google's consent screen. Large-file requests retrieve a token for both scopes only after the downloaded file exceeds 8 MiB; missing consent aborts before upload. Granting this permission does not configure Google Drive backup or billing. After personal confirmation of Google's test-app warning, a real 21.2MB WebM reached ACTIVE, produced a recipe with four ingredients/eight steps, and survived restart on the Mi 11. API-key upload request wiring is unit-tested, not live-verified here.

UNREGISTERED_ON_API_CONSOLE indicates a registration problem; model changes or retries do not fix it. Gemini authorization and Drive access are separate permissions.

See Google's [Android authorization guide](https://developer.android.com/identity/authorization), [Gemini OAuth setup](https://ai.google.dev/gemini-api/docs/oauth) and [video input documentation](https://ai.google.dev/gemini-api/docs/video-understanding). Never commit API keys, tokens, keystores, personal account details or full device logs. App preferences contain sensitive credentials.

## Credential Storage

API keys are stored as Android Keystore-backed AES-256-GCM encrypted DataStore values. Existing plaintext fields are migrated atomically after successful encryption; key loss/tampering fails closed and is reported in Settings. Encryption failure preserves legacy data for recovery but providers never read that plaintext fallback. Reinstall/device transfer may require entering API keys again; OAuth bearer tokens are not persisted. See the [credential-security audit](.planning/audits/2026-10-13-credential-security.md).

Diagnostics omit recipe/account/response contents, sanitize common secrets, limit retention/export and discard old unknown diagnostic contents at privacy upgrade. Sharing exposes only explicit diagnostic exports. Android backup rules allow only recipe database/WAL data, not keys/settings/logs; cloud backup requires encryption support. Actual system-backup recovery is not live-verified. See the [privacy/backup-boundary audit](.planning/audits/2026-10-13-diagnostic-and-backup-boundaries.md). These checks are not a production-readiness certification.

## Google Drive Backups

In Settings, connect Google Drive separately from Gemini, choose **Backup now**, and use **Refresh backups** to list snapshots. **Restore** requires confirmation and adds only missing recipes; matching UUIDs and local edits always win. Repeating the same restore does not duplicate recipes. **Disconnect Drive** keeps cloud snapshots and does not sign Gemini out.

Only [Drive's appDataFolder permission](https://developers.google.com/workspace/drive/api/guides/appdata) is requested. Snapshots contain recipe data only, never keys, tokens, settings or logs; access tokens are not persisted. Archives are capped at 500 recipes / 2 MiB, and completely validated before a transactional Room merge. The application does not provide end-to-end encryption for recipe snapshots. Backups are hidden from the normal Drive file UI and remain subject to Google account security and the user's ability to remove app data in Drive. This is manual backup/recovery, not automatic synchronization. No backup is created merely by connecting an account.

Room version 4 assigns stable recipe UUIDs without rewriting existing content. Native tests cover schema-3 migration, local-conflict preservation and rollback; the Mi 11 main database migration retained all three recipes, twelve ingredients and twenty steps with unchanged previous fields. See the [backup-core audit](.planning/audits/2026-10-13-backup-core.md) and [Drive acceptance audit](.planning/audits/2026-10-13-drive-integration.md).

On the Mi 11, a real Drive snapshot was uploaded, listed, downloaded and restored twice with zero new recipes and all existing content/IDs preserved. This proves that particular authorized same-device round trip; a clean-install/second-device or minified-release restore has not been live-verified. Google did not return account email metadata for this scope-only request, so the UI shows a generic connected state rather than claiming an account identity.

## Code Map

```text
app/src/main/java/com/recipegrabber/
  data/local/          Room entities, DAOs and database
  data/repository/     Persistence and DataStore preferences
  data/remote/         Google authorization/Drive and Apify
  data/security/       Keystore encryption and credential migration
  domain/backup/       Validated snapshot format and Drive coordination
  domain/llm/          Adapters, request builders and parsers
  domain/usecase/      Validated extraction and saving
  presentation/       Compose screens and ViewModels
  service/            Clipboard foreground service
app/src/test/          JUnit 5 / MockK tests
app/src/androidTest/   Room and Compose tests with AndroidJUnitRunner
.planning/codebase/    GSD map refreshed 2026-10-13; verify source checkpoint
.planning/audits/      Audit evidence and verification limits
```

ExtractRecipeUseCase owns validation and saving. Both list entry and shared-video entry use it; providers must return ingredients and steps, not turn parser errors into placeholder successes.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Remaining gaps include real media ingestion for other providers, reliable social-video handling, historical Room 1/2 migration coverage, minified-release device acceptance and broader lifecycle/device testing. Drive synchronization and scheduling are not implemented.

## License

[MIT](LICENSE).
