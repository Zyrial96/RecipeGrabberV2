---
last_mapped_commit: 3dfcf75a166b1af026a710d6e54c8de6b1c9c126
analysis_date: 2026-10-13
---

# External Integrations

**Analysis Date:** 2026-10-13
**Last mapped commit:** `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`

## APIs & External Services

**Google Gemini / Generative Language:**
- `GeminiRequestFactory.kt` calls `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent` with OkHttp.
- API-key mode sends `x-goog-api-key`; OAuth mode sends a bearer token from `GoogleDriveService.kt`.
- YouTube URLs remain `fileData.fileUri`. Direct HTTPS MP4/WebM/MOV files are downloaded to bounded cache; up to 8 MiB use `inlineData`, larger files use Files API and only ACTIVE file URIs. Input credentials, non-HTTPS redirects and private media DNS destinations are rejected.
- JSON output is requested with `responseMimeType: application/json` and parsed by `GeminiResponseParser.kt`.
- Models are declared in `app/src/main/java/com/recipegrabber/domain/llm/LlmModels.kt`; new selections default to Gemini 3.1 Flash-Lite.
- Live status: `.planning/audits/2026-10-13-google-oauth-registration.md` verifies one public YouTube video succeeded with Flash-Lite, produced four ingredients/five steps, persisted in Room, and survived restart. This does not verify every model, URL, or generated value.
- Current issue: Gemini 3.8 had prior HTTP 503 and timeout observations. No automatic retry or model substitution is implemented.

**Other LLM providers:**
- OpenAI, Claude, and Kimi adapters exist in `OpenAiProvider.kt`, `ClaudeProvider.kt`, and `KimiProvider.kt`.
- Their extraction requests include the source URL as text rather than attaching actual video media; no live end-to-end video acceptance is claimed.
- Construction and selection are centralized in `LlmProviderFactory.kt`.

**Apify:**
- `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt` calls `https://api.apify.com/v2/` through Retrofit.
- It starts `apify/instagram-scraper` or `clockworks/tiktok-scraper`, polls up to 30 two-second intervals, and reads one dataset item.
- Returned `videoUrl` is passed onward as media when present; a platform page is not itself Gemini media ingestion.
- Social-video compatibility is not live-verified; API keys are runtime settings and are not documented.

## Data Storage

**Databases:**
- Room SQLite database `recipe_grabber_db` is a singleton from `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`.
- `RecipeDatabase` version 3 contains `Recipe`, `Ingredient`, and `Step` entities with DAOs under `app/src/main/java/com/recipegrabber/data/local`.
- Migrations are registered from `Migrations.kt`; schema export is enabled.
- Live status: the audit read back the Flash-Lite recipe after app restart. Migration coverage remains a documented test gap.

**File Storage:**
- `GoogleDriveService.kt` uploads JSON to the Drive `appDataFolder`, lists JSON files, and deletes by ID.
- Room/DataStore persist application data. Direct video transfers use temporary app-cache files (100 MiB limit) removed in finally. Uploaded Google files get a best-effort delete limited to the newly created file; if deletion cannot complete, Google's file expiration applies. No permanent video library is added.

**Caching:**
- No remote cache is detected. Account state/preferences are held in memory/DataStore; Gemini responses are not cached.

## Authentication & Identity

**Google OAuth:**
- Gemini uses Android Identity AuthorizationClient with only `https://www.googleapis.com/auth/generative-language.retriever`, requested by `GoogleAiAuthorizationRequestFactory.kt`.
- The live Files upload test returned `ACCESS_TOKEN_SCOPE_INSUFFICIENT`. Discovery metadata lists `devstorage.read_only` for file operations. A separate optional consent flow for larger files requires approval and remains unactivated. Small inline videos work with the existing scope; no Cloud roles or account scopes were silently broadened.
- `consumeGenerativeLanguageAuthorizationResult` requires the exact scope and a nonblank access token.
- Follow-up at `aa4a8eb`: the user approved the additional Files permission. The statements above describe the historical base flow; Settings now offers a separate optional disclosure/authorization requesting retriever plus `devstorage.read_only`. All requested scopes and a nonblank token are checked. The combined token is fetched only for files above 8 MiB and used for upload/polling/cleanup/generation; YouTube and inline remain base-only. No token is persisted or billing/IAM/client configuration changed. The Mi 11 reached Google's unverified-app warning and awaits personal user confirmation, not another implementation approval.
- Drive uses Google Sign-In with email and `DriveScopes.DRIVE_APPDATA`, then creates a `GoogleAccountCredential`.
- These are separate permission flows. Tokens and account identifiers are intentionally omitted from documentation.
- Live status: the audit records `RESULT_OK`, the Generative Language scope, and successful Flash-Lite video generation on the registered Android client. Drive backup/restore is not live-verified.

**Provider API keys:**
- Gemini, OpenAI, Claude, Kimi, and Apify credentials are read from `PreferencesRepository`; values are user-provided at runtime and must not be committed.

## Monitoring & Observability

**Error Tracking:**
- No hosted error-tracking service is detected.

**Logs:**
- `AppLogger` is used by providers, OAuth, Apify, and repositories. Gemini logs bounded HTTP status failures, but other adapters still log response snippets, source URLs and account identifiers. Do not describe the entire logger as sanitized; review exports before sharing.
- `GeminiProvider.kt` maps HTTP 401/403/429/5xx responses to distinct user-facing failures.

## CI/CD & Deployment

**Hosting:**
- The application is an Android APK; no server-side deployment target is detected.

**CI Pipeline:**
- `.github/workflows/android.yml` runs JVM tests, lint, builds, and Room/Compose instrumentation on API 31 and 35 emulators.
- CI does not perform OAuth authorization, paid generation, or live provider acceptance; external checks are explicitly selected locally.

## Environment Configuration

**Required env vars:**
- None are required for browsing and local storage. Provider keys and Google authorization are configured interactively.

**Secrets location:**
- Runtime secrets reside in Android preferences supplied by the user. No secret values, account details, or private logs belong here.

## Webhooks & Callbacks

**Incoming:**
- No server webhook is detected. Google authorization returns through PendingIntent/activity-result handling in `MainActivity.kt` and `GoogleDriveService.kt`.

**Outgoing:**
- The app sends HTTPS requests to Gemini, Apify, Kimi, and Google Drive. No callback URL or webhook registration is detected.

## Evidence-Backed Issues Requiring Parent Action

- Drive upload/list/delete helpers exist, but automatic backup, synchronization worker, and restore workflow are not implemented (`README.md`, `GoogleDriveService.kt`).
- TikTok/Instagram scraping and returned CDN URLs lack live end-to-end acceptance evidence; keep this capability experimental.
- The positive device path is specific to public YouTube plus Google OAuth plus Flash-Lite. Do not generalize it to 3.8, other providers, social platforms, or arbitrary direct files.
- The prior 3.8 failures remain unresolved service behavior. The audit does not identify quota, billing, transport, or OAuth scope as the cause.
- A live 7.14MB Commons WebM request through the share-intent path returned HTTP 400. The explicit diagnostic reported `INVALID_ARGUMENT` / `URL_UNREACHABLE-UNREACHABLE_5xx`: Google could not fetch that origin, although the Mac received HTTP 200. Direct-file remote acceptance remains open; this is not an OAuth failure.
- The new device downloader reaches the same origin with a policy-compliant application User-Agent, but current OAuth cannot start a Files upload (HTTP 403 / scope insufficient). See `.planning/audits/2026-10-13-files-api-verification.md`; do not infer successful upload from the isolated tests.
- Current positive update: the same 7,482,355-byte WebM now uses inline data, produced recipe ID 2 with four ingredients and seven steps, and survived restart. No Files API object or extra permission was needed. The preceding failure statements are historical for direct-URI/Files transfer, not current small-file acceptance.

---

*Integration audit: 2026-10-13; last_mapped_commit: `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`*
