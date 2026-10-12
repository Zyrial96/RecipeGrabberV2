# RecipeGrabberV2

Native Kotlin Android app for turning cooking videos into structured recipes, storing them locally, and adjusting portions. This is an early-stage public project, not a production-ready service. Download and user numbers have not been verified.

## Current Capabilities

| Area | Implementation and limits |
| --- | --- |
| Local recipes | Room storage, search, favorites, sharing and portion scaling. Saved recipes are readable offline; extraction needs network access. |
| Gemini video input | Public YouTube URLs or HTTPS MP4/WebM/MOV URLs are attached as fileData, not just included in a prompt. Request/parsing tests use fixtures. Live extraction requires valid credentials, model access and an accessible video. |
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

The [Android Checks workflow](.github/workflows/android.yml) runs unit tests, lint and both builds on pushes and pull requests. Its artifacts contain test/lint reports, not live authorization results. Device UI tests are separate.

## Authentication

Credentials are optional for browsing recipes and completing onboarding. For Gemini, configure an API key or Google OAuth in Settings. API use may incur provider charges.

OAuth requires an enabled Generative Language API, configured consent screen and Android OAuth client matching package com.recipegrabber and the actual installed signing certificate's SHA-1. Use `./gradlew signingReport`; another developer's debug certificate is not interchangeable. Add authorized test users while the consent screen is in testing. A separate audit package needs its own registration.

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
.planning/codebase/    GSD map: historical snapshot, verify live source
.planning/audits/      Audit evidence and verification limits
```

ExtractRecipeUseCase owns validation and saving. Both list entry and shared-video entry use it; providers must return ingredients and steps, not turn parser errors into placeholder successes.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Remaining gaps include real media ingestion for other providers, reliable social-video handling, Drive backup/restore, Room integration coverage and broader device acceptance tests.

## License

[MIT](LICENSE).
