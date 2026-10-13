# RecipeGrabberV2

Native Kotlin Android app for turning cooking videos into structured recipes, storing them locally, and adjusting portions. This is an early-stage public project, not a production-ready service. Download and user numbers have not been verified.

## Current Capabilities

| Area | Implementation and limits |
| --- | --- |
| Local recipes | Room storage, search, favorites, sharing and portion scaling. Saved recipes are readable offline; extraction needs network access. |
| Gemini video input | Public YouTube URLs remain fileData. Direct HTTPS MP4/WebM/MOV files are downloaded with a 100 MiB cap; files up to 8 MiB use bounded inline video data, larger files use Files API transfer. YouTube and one real 7.5MB WebM import are live-verified with existing OAuth, including persistence/restart. Larger Files uploads still need separately approved permissions and are not live-verified. See the [media audit](.planning/audits/2026-10-13-files-api-verification.md). |
| TikTok / Instagram | Optional Apify scrapers attempt to obtain direct media URLs. Platform pages are not video files. Scraper reliability and returned CDN URL compatibility are not verified end to end. |
| Other AI adapters | OpenAI, Claude and Kimi adapters exist, but send URL text instead of actual video content. Their output is not verified video extraction. |
| Google OAuth | Gemini has an AuthorizationClient consent flow. Google Cloud registration for the exact package/signing certificate is required. An account picker is not proof of API authorization. |
| Google Drive | Authentication and file-operation helpers exist. Automatic recipe backup, a synchronization worker and restore workflows are not implemented. |
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

YouTube and small inline-video requests use only the Generative Language scope. Google's [live Files API discovery metadata](https://generativelanguage.googleapis.com/$discovery/rest?version=v1beta) lists `devstorage.read_only` for upload/get/delete methods. That broader Google Cloud Storage permission is not currently requested by the app; OAuth Files uploads return `ACCESS_TOKEN_SCOPE_INSUFFICIENT`. The optional consent decision therefore concerns larger transfers, not the working YouTube/small-file path. This is not Google Drive backup. API-key upload request wiring is unit-tested, not live-verified here.

UNREGISTERED_ON_API_CONSOLE indicates a registration problem; model changes or retries do not fix it. Gemini authorization and Drive access are separate permissions.

See Google's [Android authorization guide](https://developer.android.com/identity/authorization), [Gemini OAuth setup](https://ai.google.dev/gemini-api/docs/oauth) and [video input documentation](https://ai.google.dev/gemini-api/docs/video-understanding). Never commit API keys, tokens, keystores, personal account details or full device logs. App preferences contain sensitive credentials.

## Code Map

```text
app/src/main/java/com/recipegrabber/
  data/local/          Room entities, DAOs and database
  data/repository/     Persistence and DataStore preferences
  data/remote/         Google authorization/Drive and Apify
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

See [CONTRIBUTING.md](CONTRIBUTING.md). Remaining gaps include real media ingestion for other providers, reliable social-video handling, Drive backup/restore, migration coverage and broader device acceptance tests.

## License

[MIT](LICENSE).
