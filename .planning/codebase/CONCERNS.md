---
last_mapped_commit: 3dfcf75a166b1af026a710d6e54c8de6b1c9c126
analysis_date: 2026-10-13
---

# Codebase Concerns

**Analysis Date:** 2026-10-13

**Source checkpoint:** `/Users/zyrial/Projects/RecipeGrabberV2` at `3dfcf75a166b1af026a710d6e54c8de6b1c9c126` (`HEAD`).

**Last mapped commit:** `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`.

**Verification checkpoint:** 118 JVM tests pass; ten native core tests pass with three probes skipped. Real YouTube and one inline WebM extraction/persistence/restart are verified. Broad lifecycle/security verification remains deferred; larger Files uploads remain permission-blocked.

## Tech Debt

**Provider-specific extraction remains duplicated:**
- Issue: OpenAI, Claude, Kimi, and Gemini each own request construction, prompt handling, parsing, and mapping. Only Gemini has verified native video `fileData` handling.
- Files: `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/KimiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`
- Impact: Schema, timeout, redaction, cancellation, and capability fixes can drift.
- Fix approach: Centralize provider-neutral prompt/schema validation and expose explicit URL/file/video capabilities.

**Drive helpers have no synchronization coordinator:**
- Issue: Upload, download, and delete helpers exist, and Room exposes `isSynced`, but no production caller coordinates backup, restore, conflicts, or retries.
- Files: `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`
- Impact: Enabling Drive settings does not establish backup or recovery semantics.
- Fix approach: Add a worker/coordinator with remote IDs, durable state, conflict policy, deletion handling, and visible status.

**List search loads all detailed data before filtering:**
- Issue: `RecipeListViewModel` collects every recipe with ingredients and steps and filters in memory; the DAO search query is unused by the display path.
- Files: `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeListViewModel.kt:45-69`, `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt:29-34`
- Impact: Room work, memory, and recomposition grow with recipe count.
- Fix approach: Query summary rows in Room, debounce input, and load details only for visible/detail screens.

## Known Bugs

**Larger file acceptance remains open:**
- Symptoms: `VideoLinkParser` accepts safe direct HTTPS MP4/WebM/MOV links and shared text, and native payload coverage passes. A real Commons WebM returned HTTP 400 with `URL_UNREACHABLE-UNREACHABLE_5xx` from Google's fetcher; the same origin responded HTTP 200 from the Mac. Remote-file extraction/persistence acceptance is not established.
- Update: `GeminiFileUploader` now downloads that media with proper application identification and uses Files API transfer. Its live upload start is rejected with HTTP 403 / `ACCESS_TOKEN_SCOPE_INSUFFICIENT`; the current scope remains retriever-only. Additional Cloud Storage permission needs separate user-approved consent, not a global or silent scope expansion.
- Current update: the same WebM succeeded through the new inline path (up to 8 MiB), saved four ingredients/seven steps and reopened after restart. No extra Google permission was required. The 8-100 MiB Files branch remains unverified/permission-blocked; do not narrow the full acceptance gate to the successful small-file branch.
- Files: `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt`, `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`, `app/src/test/java/com/recipegrabber/VideoLinkParserTest.kt`
- Trigger: Extract a real remote MP4/WebM/MOV URL through each supported provider and device entry point.
- Workaround: Use the verified public YouTube plus Gemini Flash-Lite flow.

**Lifecycle fixes are unit-proven but broad device coverage remains deferred:**
- Symptoms: `RecipeExtractionViewModel` owns and cancels the active `Job`; the sheet resets through `DisposableEffect`; three lifecycle regressions are green. Broader lifecycle/device combinations are not covered.
- Files: `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeExtractionBottomSheet.kt`, `app/src/test/java/com/recipegrabber/RecipeExtractionLifecycleTest.kt`
- Trigger: Configuration changes, process recreation, repeated share intents, or dismissal during provider/network completion.
- Workaround: None beyond the tested cancellation behavior.
- Status: Implemented and unit-proven; retain as a verification limit, not an unfixed defect.

## Security Considerations

**API keys are plaintext DataStore values:**
- Risk: OpenAI, Gemini, Claude, Kimi, and Apify keys are written directly into `Preferences`; DataStore is not encryption.
- Files: `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt:18-40`, `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt:74-103`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/SettingsScreen.kt`
- Current mitigation: Password-style fields; keys are not committed in source.
- Recommendations: Use Android Keystore-backed encryption, minimize raw-key UI lifetime, and test clearing/migration.

**Logs can contain exportable sensitive content:**
- Risk: Exceptions, source URLs, remote identifiers, and provider/service errors are persisted and can be shared as a file.
- Files: `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt:105-153`, `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt:39-45`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt:36-53`
- Current mitigation: Logs are app-private until explicit sharing; extraction use case avoids full URL logging.
- Recommendations: Redact URLs, account identifiers, response bodies, run IDs, and stack traces; bound export size.

**Clipboard monitoring has permission and sensitive-data exposure:**
- Risk: A foreground service reads clipboard content and emits matching URLs. `POST_NOTIFICATIONS` is declared, but no runtime request is evident before service start.
- Files: `app/src/main/AndroidManifest.xml:5-8`, `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt:193-219`, `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`
- Current mitigation: Service is `exported=false`; startup is setting-gated.
- Recommendations: Request notification permission, centralize lifecycle, avoid clipboard payload logging, and verify clean Android 13+ installs.

**Backup rules are narrow and do not protect DataStore secrets:**
- Risk: `allowBackup` is true and both rule files include only `sharedpref`; `PreferencesRepository` uses Jetpack DataStore `settings` under the app files datastore location. This is not a claim of backup leakage from `sharedpref`.
- Files: `app/src/main/AndroidManifest.xml:10-14`, `app/src/main/res/xml/backup_rules.xml:2-4`, `app/src/main/res/xml/data_extraction_rules.xml:2-9`, `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt:18`
- Current mitigation: Extraction is explicitly scoped to `sharedpref`.
- Recommendations: Decide and test the intended DataStore backup policy alongside Keystore storage; do not describe current rules as encryption.

## Performance Bottlenecks

**Apify polling has a fixed two-minute upper window:**
- Problem: Instagram and TikTok poll 30 times with a two-second delay before result fetch.
- Files: `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt:55-84`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt:123-153`
- Cause: Duplicate loops lack a shared deadline, backoff, and status surface.
- Improvement path: Shared cancellable polling helper with bounded backoff and progress.

**Log export materializes all logs and blocks the caller:**
- Problem: `getLogContent` concatenates every log and `createShareIntent` calls it through `runBlocking`.
- Files: `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt:105-153`
- Cause: No total retention/export-size limit and synchronous public call.
- Improvement path: Stream to bounded cache output from a lifecycle-aware coroutine.

## Fragile Areas

**Room migration history lacks matching historical schemas:**
- Files: `app/src/main/java/com/recipegrabber/data/local/Migrations.kt:6-85`, `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`, `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json`
- Why fragile: Only schema 3 is committed while 1->2 and 2->3 manually rebuild/add columns.
- Safe modification: Export versions 1 and 2 and use `MigrationTestHelper` for every upgrade path.
- Test coverage: `app/src/androidTest/java/com/recipegrabber/RecipePersistenceTest.kt` does not test migrations.

**R8 release behavior for reflection-heavy clients is weakly verified:**
- Files: `app/proguard-rules.pro`, `app/build.gradle.kts:27-34`, `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`
- Why fragile: Minification is enabled, but release smoke tests for Google clients, Retrofit/Gson, and provider parsing are absent.
- Safe modification: Inspect R8 diagnostics, add only required rules, and install a minified APK for critical flows.
- Test coverage: No release-runtime smoke test detected.

## Scaling Limits

**Local-first storage has no conflict/recovery model:**
- Current capacity: Recipes live in Room; `isSynced` is a boolean and no sync worker is wired.
- Limit: Multi-device edits, remote restore, deletions, and conflicts are not representable reliably.
- Scaling path: Add remote IDs, versions, dirty/deleted states, conflict policy, and durable attempts.

## Dependencies at Risk

**Google API client surface exceeds the active sync feature:**
- Risk: Android Google API/Drive dependencies are present while synchronization is not wired.
- Files: `app/build.gradle.kts:72-74`, `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`
- Impact: Release size and shrinking complexity grow without backup semantics.
- Migration plan: Keep while implementing sync; otherwise remove after product decision.

## Missing Critical Features

**Reliable Drive backup/restore and synchronization:**
- Problem: Sign-in/helpers exist, but automatic backup, restore, scheduling, conflicts, and deletion propagation are absent.
- Blocks: Dependable cross-device recovery.

**Native media ingestion for non-Gemini providers:**
- Problem: OpenAI, Claude, and Kimi accept URL-shaped input but have no live video-file ingestion proof.
- Blocks: Equivalent extraction behavior across provider selection.

## Test Coverage Gaps

**Extraction and URL-entry acceptance matrix is incomplete:**
- What's not tested: Independent full remote-file acceptance, native/device rerun of the new parser suite, and complete social scraper-to-persistence behavior. Parser recognition, shared/clipboard wiring, fallback error display, and lifecycle regressions are covered by the current JVM suite.
- Files: `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt`, `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`, `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`, `app/src/test/java/com/recipegrabber/VideoLinkParserTest.kt`, `app/src/test/java/com/recipegrabber/RecipeExtractionLifecycleTest.kt`
- Risk: Network/CDN, process, and device-specific behavior can still diverge from unit-proven parsing and cancellation.
- Priority: High

**Secret storage, backup policy, and log redaction lack behavioral tests:**
- What's not tested: Keystore migration, actual DataStore backup policy, notification gating, and export redaction/limits.
- Files: `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`, `app/src/main/AndroidManifest.xml`
- Risk: Privacy regressions can pass current suites.
- Priority: High

**Broad lifecycle and security verification is deferred:**
- What's not tested: Clean-install notification permission flow, Keystore migration, actual DataStore backup policy, log redaction/limits, and process-death recovery across devices.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`
- Risk: The 118 passing JVM tests do not establish Android OS, backup, or process-lifecycle behavior.
- Priority: High

**Drive sync operations are not tested as a product flow:**
- What's not tested: Orchestration, state transitions, restore conflicts, and visible failure handling.
- Files: `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`
- Risk: Connected account or enabled setting can be mistaken for completed backup.
- Priority: Medium

---

*Concerns audit: 2026-10-13*
