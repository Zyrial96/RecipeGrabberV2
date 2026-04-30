# External Integrations

**Analysis Date:** 2026-04-30

## APIs & External Services

**LLM Providers:**
- OpenAI Chat Completions - Extracts recipe JSON from a video URL prompt.
  - Implementation: `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`
  - Endpoint: `https://api.openai.com/v1/chat/completions`
  - SDK/Client: Retrofit 2.11.0, OkHttp 4.12.0, Gson converter from `app/build.gradle.kts`
  - Auth: Bearer token from DataStore key `openai_api_key` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
  - Models: `gpt-4o` and `gpt-4o-mini` from `app/src/main/java/com/recipegrabber/domain/llm/LlmModels.kt`
- Google Gemini Generative Language API - Extracts recipe JSON from a video URL prompt.
  - Implementation: `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`
  - Endpoint: `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
  - SDK/Client: Direct OkHttp request; no generated Google AI SDK wrapper
  - Auth: API key query parameter from DataStore key `gemini_api_key` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
  - Models: `gemini-2.5-flash` entries from `app/src/main/java/com/recipegrabber/domain/llm/LlmModels.kt`
- Anthropic Claude Messages API - Extracts recipe JSON from a video URL prompt.
  - Implementation: `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`
  - Endpoint: `https://api.anthropic.com/v1/messages`
  - SDK/Client: Retrofit 2.11.0, OkHttp 4.12.0, Gson converter from `app/build.gradle.kts`
  - Auth: `x-api-key` header from DataStore key `claude_api_key` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
  - Version header: `anthropic-version: 2023-06-01` in `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`
  - Models: `claude-3-5-sonnet-20241022` and `claude-3-haiku-20240307` from `app/src/main/java/com/recipegrabber/domain/llm/LlmModels.kt`
- Moonshot/Kimi Chat Completions - Extracts recipe JSON from a video URL prompt.
  - Implementation: `app/src/main/java/com/recipegrabber/domain/llm/KimiProvider.kt`
  - Endpoint: `https://api.moonshot.ai/v1/chat/completions`
  - SDK/Client: Retrofit 2.11.0, OkHttp 4.12.0, Gson converter from `app/build.gradle.kts`
  - Auth: Bearer token from DataStore key `kimi_api_key` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
  - Models: `moonshot-v1-128k` from `app/src/main/java/com/recipegrabber/domain/llm/LlmModels.kt`

**Video Scraping:**
- Apify - Scrapes TikTok and Instagram video metadata before LLM extraction.
  - Implementation: `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`
  - Endpoint: `https://api.apify.com/v2/`
  - SDK/Client: Retrofit 2.11.0, OkHttp 4.12.0, Gson converter from `app/build.gradle.kts`
  - Auth: Bearer token from DataStore key `apify_api_key` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
  - Actors: `apify/instagram-scraper` and `clockworks/tiktok-scraper` in `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`
  - Flow: start actor run with `POST acts/{actorId}/runs`, poll `GET actor-runs/{runId}`, then fetch `GET datasets/{datasetId}/items` in `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`
  - Used by: `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`

**Google Services:**
- Google Sign-In + Google Drive API - Authenticates the user and stores recipe backup JSON in the Drive appDataFolder.
  - Implementation: `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`
  - SDK/Client: `com.google.android.gms:play-services-auth:21.2.0`, `com.google.api-client:google-api-client-android:2.4.1`, and `com.google.apis:google-api-services-drive:v3-rev20240521-2.0.0` from `app/build.gradle.kts`
  - Auth: Google Play Services account OAuth with scope `DriveScopes.DRIVE_APPDATA` in `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`
  - Storage target: Drive `appDataFolder`; upload/list/delete logic in `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`
  - Persisted state: `drive_sync_enabled` and `google_account_email` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`

**Android Platform Services:**
- Android share sheet incoming text - Accepts shared plain-text URLs.
  - Implementation: SEND intent filter in `app/src/main/AndroidManifest.xml` and handling in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`
  - Auth: Not applicable
- Android clipboard monitor foreground service - Watches clipboard for recipe/video URLs.
  - Implementation: `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`
  - Manifest entry: `app/src/main/AndroidManifest.xml`
  - Permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, and `POST_NOTIFICATIONS` in `app/src/main/AndroidManifest.xml`
  - Auth: Not applicable

## Data Storage

**Databases:**
- Local SQLite via Room
  - Connection: In-app local database named `recipe_grabber_db` in `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`
  - Client: Room database provided by `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`
  - Schema: `recipes`, `ingredients`, and `steps` entities in `app/src/main/java/com/recipegrabber/data/local/entity/*.kt`
  - Versioning: database version 3 with migrations in `app/src/main/java/com/recipegrabber/data/local/Migrations.kt`
  - Schema export: `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json`
- Android DataStore Preferences
  - Connection: `preferencesDataStore(name = "settings")` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
  - Client: `PreferencesRepository` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
  - Stores: onboarding flag, LLM provider/model selection, API keys, Drive sync state, clipboard monitor state, dark mode, and auto-extract setting.

**File Storage:**
- Google Drive appDataFolder for cloud recipe backup via `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`.
- Local app files for logs in `context.filesDir/logs` via `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.
- Local cache file `recipegrabber-logs.txt` for log sharing through FileProvider in `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.
- Android FileProvider configured in `app/src/main/AndroidManifest.xml` with paths in `app/src/main/res/xml/file_paths.xml`.

**Caching:**
- No external cache service detected.
- Gradle build caching is enabled with `org.gradle.caching=true` in `gradle.properties`.
- Runtime persistence relies on Room/DataStore and local log files, not a cache layer.

## Authentication & Identity

**Auth Provider:**
- Google Sign-In for Drive sync
  - Implementation: `GoogleSignInOptions.DEFAULT_SIGN_IN`, `.requestEmail()`, and `.requestScopes(Scope(DriveScopes.DRIVE_APPDATA))` in `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`
  - OAuth credential: `GoogleAccountCredential.usingOAuth2(...)` in `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`
  - UI entry points: onboarding in `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/GoogleDriveStep.kt` and settings in `app/src/main/java/com/recipegrabber/presentation/ui/screens/SettingsScreen.kt`
- API key identity for LLM and Apify services
  - Implementation: User-entered keys persisted by `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
  - UI entry points: `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/LlmSelectionStep.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/ApifyStep.kt`, and `app/src/main/java/com/recipegrabber/presentation/ui/screens/SettingsScreen.kt`

## Monitoring & Observability

**Error Tracking:**
- External error tracking service: None detected.
- Local error capture: `AppLogger.e(...)` writes stack traces to local log files in `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.

**Logs:**
- Local file logging in `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.
- Log directory: `context.filesDir/logs` in `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.
- Active log file: `app.log` with rotation at 5 MB in `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.
- Android Logcat output: `android.util.Log.d("RG-$tag", message)` in `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.
- Log viewer/export UI: `app/src/main/java/com/recipegrabber/presentation/ui/screens/LogViewerScreen.kt` and `app/src/main/java/com/recipegrabber/presentation/viewmodel/LogViewerViewModel.kt`.

## CI/CD & Deployment

**Hosting:**
- Android app only; no server hosting platform detected.
- Release artifact is produced by Gradle Android build from `app/build.gradle.kts`.

**CI Pipeline:**
- None detected. No `.github/workflows/`, Bitbucket, GitLab, Fastlane, Vercel, Netlify, or Docker deployment config is present.

## Environment Configuration

**Required env vars:**
- None detected. The app does not read process environment variables.
- Required runtime secrets are user-entered and persisted in DataStore by `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`:
  - `openai_api_key` for OpenAI
  - `gemini_api_key` for Gemini
  - `claude_api_key` for Claude
  - `kimi_api_key` for Kimi/Moonshot
  - `apify_api_key` for Apify
- Google Drive auth uses Google Sign-In account OAuth from `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, not a checked-in secret.

**Secrets location:**
- API keys are stored in Android DataStore Preferences named `settings`, declared in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- Google account email and Drive sync flag are stored in the same DataStore via `google_account_email` and `drive_sync_enabled` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- `local.properties` is present and ignored by `.gitignore`; its contents were not used for this audit.
- `.env` files are not detected.
- `google-services.json` is ignored by `.gitignore` but not detected.

## Webhooks & Callbacks

**Incoming:**
- No HTTP webhook endpoints are present; this is a client-only Android application.
- Android launcher and share intents are declared in `app/src/main/AndroidManifest.xml`.
- Google sign-in result is handled as an Android activity result in `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/GoogleDriveStep.kt` and in view models such as `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/OnboardingDriveViewModel.kt`.

**Outgoing:**
- No registered webhook callbacks detected.
- Outgoing HTTPS calls are made directly to OpenAI, Gemini, Anthropic, Moonshot/Kimi, Apify, and Google Drive from `app/src/main/java/com/recipegrabber/domain/llm/*.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`, and `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`.

---

*Integration audit: 2026-04-30*
