---
last_mapped_commit: 3dfcf75a166b1af026a710d6e54c8de6b1c9c126
analysis_date: 2026-10-13
---

# Testing Patterns

**Analysis Date:** 2026-10-13
**Last mapped commit:** `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`

## Test Framework

**Runner and dependencies:**
- JVM tests use JUnit Jupiter 5.11.2, MockK 1.13.12, and `kotlinx-coroutines-test`, declared in `gradle/libs.versions.toml` and wired in `app/build.gradle.kts`.
- `app/build.gradle.kts` enables `unitTests.all { it.useJUnitPlatform() }`.
- Android tests use `androidx.test.runner.AndroidJUnitRunner`, AndroidX JUnit and Compose UI test JUnit4. MockK is configured in the JVM source set, not as an Android-test dependency.

**Run commands:**
```bash
./gradlew testDebugUnitTest
./gradlew lintDebug assembleDebug assembleRelease
./gradlew connectedDebugAndroidTest
```

`.github/workflows/android.yml` runs Java 17, executes unit tests, lint, and both APK builds in `build`, then runs instrumentation on API 31 and API 35 emulator matrix entries. Reports are uploaded from `app/build/reports/`, `app/build/test-results/`, and `app/build/outputs/androidTest-results/`.

## Current Evidence

- `/tmp/recipegrabber-lifecycle-build.txt` records successful `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest`, and `assembleRelease` tasks, ending with `BUILD SUCCESSFUL`.
- Live XML under `app/build/test-results/testDebugUnitTest/` totals 118 JVM tests with zero failures, errors, or skips.
- Inline routing tests prove small video bytes/MIME are attached without Files calls and threshold-exceeding inputs still use the larger-file upload path. Upload-only fixtures explicitly isolate that branch; the 100 MiB download cap is unchanged.
- `GeminiFileUploaderTest` verifies bounded streams, auth-header isolation, upload-origin validation, ACTIVE/FAILED/timeout handling, cleanup and private DNS rejection using isolated call factories. `GeminiUploadFailureTest` verifies structured scope diagnostics without raw server-content leakage. `GeminiHttpCallTest` covers cancellation during response consumption as well as header waiting.
- `VideoLinkParserTest` covers URL parsing and unsafe input handling in `app/src/test/java/com/recipegrabber/VideoLinkParserTest.kt`.
- `RecipeExtractionLifecycleTest` covers the extraction lifecycle and records three regressions that failed before fixes and passed after the fixes in `app/src/test/java/com/recipegrabber/RecipeExtractionLifecycleTest.kt`.
- `VideoInputTest` is an Android test with two cases validating Shorts and MP4/WebM/MOV request payloads plus unsafe inputs without external network access: `app/src/androidTest/java/com/recipegrabber/VideoInputTest.kt`.
- At production checkpoint `8324c13`, the Mi 11 suite passed ten core tests and skipped three opt-in probes in 9.203 seconds (`/tmp/recipegrabber-final-device-tests.txt`). VideoInputTest covers native payload construction, not remote media retrieval.
- After updating the opt-in probe, the installed test APK's default suite again passed ten tests and skipped three in 8.918 seconds (`/tmp/recipegrabber-current-device-tests.txt`).
- After the Files uploader changes, the Mi 11 default suite passed ten tests and skipped three in 8.876 seconds (`/tmp/recipegrabber-files-device-tests.txt`). Full local lint/debug/test/release build succeeded. The earlier published checkpoint `bffe751` is also fully green in hosted run 38381879227; the new uploader is not covered by that historical CI result.
- Hosted run [38385620263](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/38385620263) at `9b875e3` is now fully green for build and API 31/35 instrumentation, including the uploader and its tests. Default external probes remain skipped; hosted success does not establish positive remote-file acceptance.
- With inline checkpoint `3dfcf75`, the Mi 11 default suite passed ten core tests and skipped three in 8.869 seconds (`/tmp/recipegrabber-inline-device-tests.txt`). One real shared WebM separately succeeded, saved four ingredients/seven steps and survived restart, using the unchanged retriever-only OAuth scope. Hosted CI for this newer source still needs verification.
- A separately selected live WebM diagnostic failed with HTTP 400 / `INVALID_ARGUMENT` and `URL_UNREACHABLE-UNREACHABLE_5xx`. This is external failure evidence, not a default-suite failure or successful media acceptance. The probe can select `probeModel` and `probeVideoUrl` explicitly; flags remain disabled in CI.

## Test File Organization

**JVM unit tests:**
- Place local tests under `app/src/test/java/com/recipegrabber/`. Current coverage includes provider HTTP/request parsing, authentication routing, model defaults, onboarding, portion scaling, recipe-list extraction, extraction lifecycle, URL parsing, and repository delegation.
- Keep filenames ending in `Test.kt`, with classes named after the subject plus `Test`.

**Instrumentation tests:**
- Place device tests under `app/src/androidTest/java/com/recipegrabber/`.
- Current files include `GeminiNetworkProbeTest.kt`, `RecipeDetailScreenTest.kt`, `RecipePersistenceTest.kt`, and `VideoInputTest.kt`.
- Keep network probes opt-in/assumption-gated; local payload validation belongs in instrumentation when it depends on Android request construction.

## Test Structure

Use JUnit annotations and readable display names. Repository tests group behavior with nested classes and create relaxed DAO mocks in `@BeforeEach`, as shown in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`:

```kotlin
@Nested
@DisplayName("Recipe Operations")
inner class RecipeOperations {
    @Test
    @DisplayName("Should insert recipe successfully")
    fun `should insert recipe successfully`() = runTest {
        coEvery { recipeDao.insertRecipe(any()) } returns 1L
        assertEquals(1L, repository.insertRecipe(testRecipe))
        coVerify { recipeDao.insertRecipe(testRecipe) }
    }
}
```

- Use `@BeforeEach` and `@AfterEach` for repeatable setup and dispatcher cleanup in JVM tests.
- Use `runTest` for coroutine unit tests and `runBlocking` where Android instrumentation tests exercise Room directly.
- Construct simple `Recipe`, `Ingredient`, and `Step` values directly; mock DAOs or external boundaries rather than entities.

## Mocking and Boundaries

- Mock `RecipeDao`, `IngredientDao`, and `StepDao` for repository delegation tests.
- Mock `ApifyService`, `LlmProviderFactory`, and `PreferencesRepository` when testing orchestration or ViewModel behavior.
- Do not call live LLM, Google Drive, or Apify APIs from JVM tests. Network-dependent Android probes belong in `app/src/androidTest/java/com/recipegrabber/GeminiNetworkProbeTest.kt` and must remain explicitly gated.

## Fixtures, Room, and Coverage

- Fixtures are private inline values/helpers in the owning test class; shared fixture directories and factories are not detected.
- Room integration and persistence tests belong under `app/src/androidTest/java/com/recipegrabber/`, where `Room.inMemoryDatabaseBuilder` and Android database behavior can be exercised. Do not add Room integration tests under the local JVM source set merely because Room runtime dependencies are available there.
- No Jacoco, Kover, coverage threshold, or dedicated coverage task is configured. Use Gradle XML/HTML reports for observed counts.

## Test Types and Gaps

- Unit tests cover pure parsing, request construction, error routing, state logic, extraction lifecycle, and repository delegation.
- Instrumentation covers Room persistence transactions/cascades, Compose detail interactions and Android request payload validation. The current positive native result is ten passes and three skipped diagnostics.
- There is no separate end-to-end suite. OAuth authorization, actual media extraction, and physical-device acceptance remain external concerns and should not be inferred from local reports.

---

*Testing analysis: 2026-10-13*
