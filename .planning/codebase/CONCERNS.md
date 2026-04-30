# Codebase Concerns

**Analysis Date:** 2026-04-30

## Tech Debt

**Duplicated LLM provider pipeline:**
- Issue: Provider classes duplicate prompt construction, HTTP client setup, response extraction, and JSON-to-`Recipe` mapping instead of sharing a common request/parser layer.
- Files: `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/KimiProvider.kt`
- Impact: Prompt changes, JSON schema changes, parsing fixes, logging policy, and timeout behavior must be changed in multiple places. Providers can drift silently.
- Fix approach: Extract a shared recipe prompt builder, a shared `ExtractedRecipe` mapper, and provider-specific HTTP adapters behind `LlmProvider`.

**Recipe detail writes are not transactional:**
- Issue: `insertRecipeWithDetails` and `updateRecipeWithDetails` perform separate DAO calls for recipe, ingredients, and steps without a Room `@Transaction` boundary.
- Files: `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt`, `app/src/main/java/com/recipegrabber/data/local/dao/IngredientDao.kt`, `app/src/main/java/com/recipegrabber/data/local/dao/StepDao.kt`
- Impact: A failure after recipe insertion can leave recipes without ingredients/steps. A failure during update can delete old details and only partially insert replacements.
- Fix approach: Move multi-table operations into a Room DAO with `@Transaction`, or wrap repository writes with `database.withTransaction`.

**Parallel extraction paths:**
- Issue: `RecipeExtractionViewModel` uses `ExtractRecipeUseCase`, while `RecipeListViewModel.extractRecipeFromUrl` calls the selected `LlmProvider` directly and saves the recipe itself.
- Files: `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeListViewModel.kt`, `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeListScreen.kt`
- Impact: The add dialog path skips Apify scraping, provider API-key prechecks, progress updates, and shared extraction error mapping. Fixes in `ExtractRecipeUseCase` do not cover all user entry points.
- Fix approach: Route all recipe extraction entry points through `ExtractRecipeUseCase` and remove direct provider/save orchestration from `RecipeListViewModel`.

**Google Drive sync is sign-in only:**
- Issue: `GoogleDriveService` has upload/download/delete methods, and Room tracks `isSynced`, but no production flow calls `uploadRecipe`, `downloadRecipes`, `deleteRecipe`, `getUnsyncedRecipes`, or `updateSyncStatus`.
- Files: `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`
- Impact: Enabling Drive sync stores sign-in state but recipes do not actually synchronize. Users can assume cloud backup exists when no upload path is active.
- Fix approach: Add an explicit sync coordinator that observes unsynced recipes, uploads JSON to Drive, marks successful rows as synced, handles deletion, and exposes sync status/errors.

**Foreground clipboard service lifecycle is coarse:**
- Issue: `MainActivity.onStart` starts `ClipboardMonitorService` whenever onboarding is complete, regardless of `clipboardMonitorEnabled`; the service checks the setting only after clipboard changes.
- Files: `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`, `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`, `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
- Impact: The foreground notification and service lifecycle can exist even when monitoring is disabled, and stopping/disabling behavior is not centralized.
- Fix approach: Start/stop the service based on `clipboardMonitorEnabled`, handle setting changes from `SettingsViewModel`, and call `stopSelf` when disabled.

## Known Bugs

**Apify fallback retry repeats the same failing path:**
- Symptoms: For TikTok/Instagram with an Apify key, `ExtractRecipeUseCase` returns `ScrapingFailed` when Apify returns null. `RecipeExtractionViewModel` retries by calling the same use case with the same URL and settings, so it reaches the same Apify failure path again.
- Files: `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`
- Trigger: Start extraction for TikTok or Instagram when Apify scraping fails and an Apify key is configured.
- Workaround: Disable or clear the Apify key before retrying direct LLM extraction.

**Settings screen cannot initiate Google Drive sign-in:**
- Symptoms: The Settings toggle calls `setDriveSyncEnabled(true)`, which only emits a snackbar message; `SettingsScreen` has no `ActivityResultLauncher` or button that calls `getSignInIntent`.
- Files: `app/src/main/java/com/recipegrabber/presentation/ui/screens/SettingsScreen.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/GoogleDriveStep.kt`
- Trigger: Skip Drive sign-in during onboarding, then try to enable Drive sync from Settings.
- Workaround: Use the onboarding Drive step path where `GoogleDriveStep` launches `getSignInIntent`.

**Unit tests do not run with the installed JDK:**
- Symptoms: `./gradlew test` fails during Kotlin DSL initialization with `java.lang.IllegalArgumentException: 25.0.1`; `java -version` reports OpenJDK `25.0.1`.
- Files: `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`, `build.gradle.kts`, `app/build.gradle.kts`
- Trigger: Run Gradle with JDK 25.
- Workaround: Install and run Gradle with JDK 17, or upgrade Gradle/Kotlin tooling to versions that support the installed JDK.

**JUnit 5 test configuration is incomplete:**
- Symptoms: Test sources import JUnit Jupiter and `kotlinx.coroutines.test.runTest`, but `app/build.gradle.kts` does not configure `useJUnitPlatform()` and does not declare `kotlinx-coroutines-test`.
- Files: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`, `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`
- Trigger: Run unit tests after the JDK issue is resolved.
- Workaround: Add `testOptions.unitTests.all { it.useJUnitPlatform() }` and a `kotlinx-coroutines-test` test dependency.

## Security Considerations

**API keys stored in plaintext preferences:**
- Risk: OpenAI, Gemini, Claude, Kimi, and Apify credentials are stored directly in Jetpack DataStore preferences without encryption.
- Files: `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/SettingsScreen.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/LlmSelectionStep.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/ApifyStep.kt`
- Current mitigation: UI fields use `PasswordVisualTransformation` by default.
- Recommendations: Store secrets with Android Keystore-backed encryption, avoid exposing raw keys through long-lived UI state, and provide explicit key clearing.

**Backups are enabled while secrets are stored locally:**
- Risk: `android:allowBackup="true"` is set, and backup rule files exclude only a `sharedpref` path named `datastore.preferences_pb`; Jetpack DataStore preferences normally live under the app files datastore path, not `sharedpref`.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`
- Current mitigation: Backup rules attempt to exclude `datastore.preferences_pb`.
- Recommendations: Use correct DataStore backup exclusions for the actual file location or disable backup for secret-bearing data; encryption is still required for stored API keys.

**Logs contain user URLs, LLM output, account email, and errors:**
- Risk: Logs persist to app-private files and can be exported through a share intent. Logged values include video URLs, Drive account email, LLM raw response snippets, API error bodies, recipe titles, and stack traces.
- Files: `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`, `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/KimiProvider.kt`, `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`
- Current mitigation: Logs are kept under `context.filesDir/logs` and exported only through explicit UI.
- Recommendations: Redact URLs, email addresses, API error bodies, and LLM response content before writing logs; make log export clearly user-confirmed and bounded.

**Gemini API key is placed in the request URL:**
- Risk: `GeminiProvider` sends the key as a query parameter, increasing the chance of the key appearing in network tooling, errors, proxies, or logs outside this app.
- Files: `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`
- Current mitigation: The app does not log the full Gemini request URL with the key.
- Recommendations: Prefer an API-key header if supported by the target API/client, and sanitize all Retrofit/OkHttp logging before enabling network logging.

**Clipboard monitoring handles sensitive OS data:**
- Risk: The service reads clipboard contents and logs matching video URLs. Clipboard contents can be private even when they match supported host patterns.
- Files: `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`, `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`, `app/src/main/AndroidManifest.xml`
- Current mitigation: The service is non-exported and runs as a foreground service.
- Recommendations: Start monitoring only after explicit opt-in, stop it when disabled, avoid logging clipboard contents, and request notification/foreground-service permissions before service start.

## Performance Bottlenecks

**Apify polling is duplicated and fixed-delay:**
- Problem: Instagram and TikTok scraping each poll up to 30 times with a 2-second delay, tying one extraction to roughly 60 seconds before timeout.
- Files: `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`
- Cause: The service uses identical hand-written polling loops with no shared timeout policy, cancellation status surface, or backoff.
- Improvement path: Extract a shared `waitForRun` helper with bounded timeout, cancellation propagation, status logging, and configurable polling interval/backoff.

**Recipe list filtering is in-memory:**
- Problem: `RecipeListViewModel` loads all recipes with details and filters the list in Compose/ViewModel state.
- Files: `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeListViewModel.kt`, `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt`
- Cause: `getAllRecipesWithDetails` is always collected and `searchRecipes` is not used for the displayed search path.
- Improvement path: Use DAO-level search for list queries and load details lazily for visible/detail screens when recipe count grows.

**Log export reads all log files into memory:**
- Problem: `getLogContent` reads every `.log` file and builds a single string, and `createShareIntent` blocks with `runBlocking`.
- Files: `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/LogViewerViewModel.kt`
- Cause: Export builds a full in-memory text blob and writes from a synchronous method.
- Improvement path: Make export suspendable, stream logs to the cache file, enforce a total retention cap, and call it from `viewModelScope`.

## Fragile Areas

**LLM JSON parsing silently creates placeholder recipes:**
- Files: `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/KimiProvider.kt`
- Why fragile: Parser exceptions are swallowed and converted into generic placeholder `Recipe` objects. The UI receives success even when model output is malformed.
- Safe modification: Return parser errors as `Result.failure`, validate required fields, and add tests for code-fenced JSON, extra prose, malformed JSON, empty ingredients, and provider-specific response shapes.
- Test coverage: `app/src/test/java/com/recipegrabber/LlmProviderTest.kt` does not exercise provider parsing or API response handling.

**Custom logger uses `GlobalScope`:**
- Files: `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`
- Why fragile: Background writes are detached from application lifecycle, flush failures are not surfaced, and concurrent `getLogContent` can call `flushBuffer` while a background flush is active.
- Safe modification: Inject an application-scoped `CoroutineScope`, guard file IO with a mutex, and make export/clear operations coordinate with pending writes.
- Test coverage: No tests cover logging rotation, export, clearing, or concurrent writes.

**Room migrations are untested:**
- Files: `app/src/main/java/com/recipegrabber/data/local/Migrations.kt`, `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`, `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json`
- Why fragile: Migration SQL copies/drops tables manually and schema export includes only version 3 in the repository.
- Safe modification: Add Room migration tests with historical schemas for versions 1, 2, and 3 before changing entities.
- Test coverage: `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt` mocks DAOs and does not open a Room database.

**Release shrinking lacks complete external-library rules:**
- Files: `app/proguard-rules.pro`, `app/build.gradle.kts`, `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`
- Why fragile: Release builds enable minification, but keep rules are broad for app packages and narrow for external Google/HTTP libraries. Retrofit/Gson model reflection and Google Drive client classes need release-build verification.
- Safe modification: Run release builds in CI and add keep rules based on actual R8 diagnostics instead of broad app-level retention.
- Test coverage: No CI or release smoke test is detected.

## Scaling Limits

**All user data is local-device first:**
- Current capacity: Recipes are stored in a local Room database and optional Drive sync does not run automatically.
- Limit: Multi-device conflict resolution, remote restore, and deletion propagation are not implemented.
- Scaling path: Introduce a sync table with remote IDs, dirty flags, conflict strategy, and background sync scheduling.

**Manual provider clients grow linearly with each provider:**
- Current capacity: Four providers are implemented separately.
- Limit: Adding providers multiplies prompt, parser, timeout, error, and test maintenance.
- Scaling path: Centralize provider-neutral extraction behavior and keep provider adapters thin.

## Dependencies at Risk

**Desktop OAuth dependency in Android app:**
- Risk: `com.google.oauth-client:google-oauth-client-jetty` is a Jetty-based OAuth client dependency that is not aligned with Android app flows.
- Impact: It can increase method count, introduce desktop/server transitive dependencies, and complicate release shrinking.
- Migration plan: Remove it if unused and rely on Android Google Sign-In / Identity Services dependencies already present in `app/build.gradle.kts`.

**Deprecated LocalBroadcastManager dependency:**
- Risk: `androidx.localbroadcastmanager:localbroadcastmanager:1.1.0` is declared but not used by the current broadcast implementation.
- Impact: It adds stale dependency surface and can mislead future work into using deprecated local broadcasts.
- Migration plan: Remove the dependency from `app/build.gradle.kts`; keep using package-scoped or lifecycle-scoped in-app event delivery.

## Missing Critical Features

**No automated sync worker:**
- Problem: Drive sync has sign-in and raw API calls but no scheduling, upload, download, conflict handling, or deletion handling.
- Blocks: Reliable backup/restore and "sync across devices" behavior described in `README.md`.

**No CI/build verification detected:**
- Problem: No GitHub Actions, Gradle CI script, or local verification wrapper is detected.
- Blocks: Catching JDK/toolchain breakage, unit-test configuration regressions, release shrinking issues, and migration failures before release.

**No runtime notification permission flow:**
- Problem: `POST_NOTIFICATIONS` is declared, but no runtime request path is detected before starting the foreground service.
- Blocks: Predictable foreground-service notification behavior on Android 13+.

## Test Coverage Gaps

**Extraction pipeline is mostly untested:**
- What's not tested: Apify failure paths, direct fallback behavior, provider API-key validation, progress updates, malformed LLM responses, and recipe persistence after extraction.
- Files: `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`, `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`
- Risk: Core recipe extraction can regress without failing tests.
- Priority: High

**Security-sensitive storage is untested:**
- What's not tested: API key persistence, key clearing, backup exclusion behavior, and log redaction.
- Files: `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`
- Risk: Secrets can be stored, backed up, or exported in ways users do not expect.
- Priority: High

**Database behavior is mocked rather than integration-tested:**
- What's not tested: Room constraints, cascades, search SQL, transactional writes, migrations, and schema compatibility.
- Files: `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`, `app/src/main/java/com/recipegrabber/data/local/Migrations.kt`, `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`
- Risk: Persistence bugs can pass DAO-mock tests.
- Priority: High

**Google Drive sync behavior is not tested:**
- What's not tested: Sign-in result handling, upload/download/delete result mapping, sync state transitions, and failure messaging.
- Files: `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/OnboardingDriveViewModel.kt`
- Risk: Drive UI can report enabled sync while remote operations do not run or fail silently.
- Priority: Medium

---

*Concerns audit: 2026-04-30*
