# Coding Conventions

**Analysis Date:** 2026-04-30

## Naming Patterns

**Files:**
- Use PascalCase Kotlin filenames for classes, interfaces, data models, ViewModels, Compose screens, and Hilt modules: `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeListViewModel.kt`, `app/src/main/java/com/recipegrabber/domain/llm/LlmProvider.kt`, `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`.
- Use feature-oriented screen filenames ending in `Screen` or `Step` for Compose UI: `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeListScreen.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/LlmSelectionStep.kt`.
- Use layer-specific suffixes for persistence and service types: `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt`, `app/src/main/java/com/recipegrabber/data/local/entity/Recipe.kt`, `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.
- Use test filenames ending in `Test`: `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`, `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.

**Functions:**
- Use lower camelCase for production functions: `getAllRecipes`, `insertRecipeWithDetails`, `extractRecipeFromVideo`, `startExtraction`, `setOpenAiApiKey`.
- Use suspend functions for repository writes, DAO operations, external calls, and use case work that crosses IO boundaries: `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.
- Use operator `invoke` for executable use cases: `suspend operator fun invoke(videoUrl: String)` in `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`.
- Use readable backtick names in tests: `fun \`should insert recipe successfully\`() = runTest { ... }` in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.

**Variables:**
- Use lower camelCase for values and state: `uiState`, `extractionState`, `pendingVideoUrl`, `snackbarHostState`, `showDeleteDialog`.
- Use private backing mutable state prefixed with `_` and expose read-only `StateFlow`: `_uiState` with `val uiState: StateFlow<...> = _uiState.asStateFlow()` in `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`.
- Use all-caps constants for Room preference keys and action strings where constants are grouped: `OPENAI_API_KEY`, `DRIVE_SYNC_ENABLED` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`; `ACTION_RECIPE_URL_DETECTED` in `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`.
- Use descriptive domain names instead of generic DTO names at public boundaries: `ExtractionUiState`, `RecipeListUiState`, `ProgressUpdate`, `ScrapedVideoData`.

**Types:**
- Use PascalCase for classes, interfaces, objects, data classes, enums, and sealed result types: `Recipe`, `LlmProvider`, `AppLogger`, `ProviderType`, `ExtractRecipeUseCase.ExtractionResult`.
- Keep UI state as immutable data classes with defaults: `RecipeListUiState` in `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeListViewModel.kt`, `SettingsUiState` in `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`.
- Model finite UI or domain states with enums or sealed classes: `ExtractionStep` in `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`, `ExtractionResult` in `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`.
- Name Retrofit request/response DTOs by API shape: `ExtractionRequest`, `ChatCompletionResponse`, `ActorRunRequest`, `RunStatusResponse` in `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt` and `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.

## Code Style

**Formatting:**
- Kotlin official style is configured in `gradle.properties` with `kotlin.code.style=official`.
- Use 4-space indentation for Kotlin blocks. Multi-line constructor and function arguments are indented one level, as in `app/src/main/java/com/recipegrabber/data/local/entity/Recipe.kt` and `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`.
- Prefer trailing lambdas for Compose and Flow collection: `setContent { ... }` in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`, `collect { ... }` in `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`.
- Chain builders on separate lines with one call per line: `Room.databaseBuilder(...).addMigrations(...).build()` in `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`, `Retrofit.Builder()` in `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`.
- Compose modifiers are passed with `modifier = Modifier...` and chained vertically for multi-step layout configuration in `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeListScreen.kt`.

**Linting:**
- Android Gradle Plugin lint is available through the Android plugin in `app/build.gradle.kts`.
- Dedicated Kotlin format/lint tools are not detected: no `.editorconfig`, `ktlint`, `detekt`, `.prettierrc`, `biome.json`, or ESLint configuration exists at the project root.
- Use Gradle/Android Studio formatting as the source of truth for Kotlin until a dedicated formatter config is added.

## Import Organization

**Order:**
1. Android framework imports first: `android.content.Context`, `android.os.Bundle` in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`.
2. AndroidX and Jetpack imports next: Compose, lifecycle, navigation, Room annotations, DataStore in `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeListScreen.kt` and `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt`.
3. Application imports after framework/library imports: `com.recipegrabber.data.*`, `com.recipegrabber.domain.*`, `com.recipegrabber.presentation.*`.
4. Third-party dependency imports after application imports when present: Hilt, Retrofit, MockK, Coroutines, Gson.
5. Java and `javax` imports appear at the end in most files: `javax.inject.Inject`, `javax.inject.Singleton` in `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`; `java.util.concurrent.atomic.AtomicReference` is an exception in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`.

**Path Aliases:**
- No Kotlin path aliases are configured. Use full package imports under `com.recipegrabber.*`.
- Gradle dependencies use version catalog aliases from `gradle/libs.versions.toml` in `app/build.gradle.kts`, for example `implementation(libs.androidx.room.ktx)` and `testImplementation(libs.mockk)`.

## Error Handling

**Patterns:**
- Use Kotlin `Result<T>` for provider and external-service methods that can fail and are consumed with `fold`: `LlmProvider.extractRecipeFromVideo` in `app/src/main/java/com/recipegrabber/domain/llm/LlmProvider.kt`, `ApifyService.scrapeTikTokVideo` in `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`, `GoogleDriveService` in `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`.
- Convert external exceptions into `Result.failure(e)` at IO boundaries and log with `AppLogger.e`: `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.
- Use sealed classes for higher-level use case outcomes where UI needs typed branches: `ExtractRecipeUseCase.ExtractionResult.Success`, `Error`, `ScrapingFailed`, and `NoApiKey` in `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`.
- ViewModels catch exceptions from repository mutations and store user-facing messages in UI state: `deleteRecipe` and `toggleFavorite` in `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeListViewModel.kt`; `handleSignInResult` in `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`.
- Compose screens observe error state and surface messages through `SnackbarHostState`: `LaunchedEffect(uiState.error)` in `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeListScreen.kt`.
- Use local fallbacks when malformed persisted or remote data is non-critical: invalid provider strings default to `ProviderType.OPENAI` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`; malformed LLM JSON returns a generic recipe in `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`.

## Logging

**Framework:** Custom `AppLogger` plus Android `Log`

**Patterns:**
- Inject `AppLogger` into services, providers, ViewModels, and use cases that perform workflow or IO operations: `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.
- Use short, stable tags such as `"OpenAI"`, `"Apify"`, `"ExtractRecipe"`, and `"ExtractionVM"`.
- Log lifecycle/progress at info level with `logger.i`, branch details at debug level with `logger.d`, recoverable issues at warning level with `logger.w`, and exceptions at error level with `logger.e`.
- `AppLogger` writes log lines to `context.filesDir/logs/app.log`, rotates at 5 MB, and exposes export/clear helpers in `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.
- Avoid logging API key values. Provider files such as `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt` log missing API keys but do not print key contents.

## Comments

**When to Comment:**
- Use short comments to separate preference groups and clarify Android lifecycle edge cases: `PreferencesRepository.PreferencesKeys` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`, `handleShareIntent(intent)` in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`.
- Avoid comments that restate the function name. Most repositories, DAOs, entities, and ViewModels are self-documenting through names and types.
- Keep comments in the same language as the surrounding file when a file already uses localized comments. `app/src/main/java/com/recipegrabber/di/LlmModule.kt` uses German comments.

**JSDoc/TSDoc:**
- Not applicable. Kotlin KDoc is not used in the current source files.
- Add KDoc only for non-obvious public APIs shared across layers, for example new domain interfaces near `app/src/main/java/com/recipegrabber/domain/llm/LlmProvider.kt` or new use cases under `app/src/main/java/com/recipegrabber/domain/usecase/`.

## Function Design

**Size:** Keep simple repository and DAO methods one expression when they only delegate: `getAllRecipes`, `insertRecipe`, and `toggleFavorite` in `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`.

**Parameters:** Prefer explicit typed parameters over parameter bags for small operations: `toggleFavorite(id: Long, isFavorite: Boolean)`, `setDriveSyncEnabled(enabled: Boolean)`, `extractRecipeFromVideo(videoUrl: String)`.

**Return Values:** Return `Flow<T>` for reactive data reads, `suspend` values for one-shot writes or network calls, `Result<T>` for fallible provider/service calls, and sealed results for use case outcomes that drive UI branches.

**Coroutine Boundaries:**
- Use `viewModelScope.launch` for UI-triggered async work in ViewModels: `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeListViewModel.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`.
- Use `withContext(Dispatchers.IO)` around network and parsing provider work: `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.
- Use a lifecycle-owned `CoroutineScope(SupervisorJob() + Dispatchers.Main)` for Android services: `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`.

## Module Design

**Exports:**
- Kotlin files export top-level data classes and API DTOs from their package. Keep DTOs close to the service/provider that owns the external contract, as in `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt` and `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.
- Use Hilt constructor injection for concrete classes where possible: `RecipeRepository`, `PreferencesRepository`, `ExtractRecipeUseCase`, `OpenAiProvider`.
- Use Hilt modules for framework-created dependencies and Room providers: `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`. Keep empty modules minimal when constructor injection is sufficient: `app/src/main/java/com/recipegrabber/di/LlmModule.kt`.

**Barrel Files:**
- Barrel files are not used. Import concrete files by package path from `com.recipegrabber.*`.

**Layer Placement:**
- Put Room entities and DAOs under `app/src/main/java/com/recipegrabber/data/local/`.
- Put repositories under `app/src/main/java/com/recipegrabber/data/repository/`.
- Put external API clients under `app/src/main/java/com/recipegrabber/data/remote/` or provider implementations under `app/src/main/java/com/recipegrabber/domain/llm/` when they implement the LLM abstraction.
- Put pure orchestration/business flow under `app/src/main/java/com/recipegrabber/domain/usecase/`.
- Put stateful UI logic under `app/src/main/java/com/recipegrabber/presentation/viewmodel/`.
- Put Compose UI under `app/src/main/java/com/recipegrabber/presentation/ui/screens/` and shared theme values under `app/src/main/java/com/recipegrabber/presentation/ui/theme/`.

---

*Convention analysis: 2026-04-30*
