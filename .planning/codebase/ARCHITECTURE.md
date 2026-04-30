# Architecture

**Analysis Date:** 2026-04-30

## Pattern Overview

**Overall:** Single-module Android MVVM with layered packages, Hilt dependency injection, Room persistence, DataStore preferences, Compose UI, and service/use-case orchestration.

**Key Characteristics:**
- UI state is owned by `@HiltViewModel` classes under `app/src/main/java/com/recipegrabber/presentation/viewmodel/` and `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/`.
- Persistence is split between Room entities/DAOs in `app/src/main/java/com/recipegrabber/data/local/` and DataStore preferences in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- Recipe extraction flows through use cases in `app/src/main/java/com/recipegrabber/domain/usecase/`, provider abstractions in `app/src/main/java/com/recipegrabber/domain/llm/`, and external service wrappers in `app/src/main/java/com/recipegrabber/data/remote/`.
- Navigation is centralized in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt` using Compose Navigation routes.
- Long-running clipboard monitoring is isolated in `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt` and communicates URL detections back to `MainActivity` by app-scoped broadcast.

## Layers

**Application Bootstrap:**
- Purpose: Initialize Android application and Hilt graph.
- Location: `app/src/main/java/com/recipegrabber/RecipeGrabberApplication.kt`
- Contains: `@HiltAndroidApp` application class.
- Depends on: Android `Application`, Hilt.
- Used by: `app/src/main/AndroidManifest.xml` through `android:name=".RecipeGrabberApplication"`.

**Presentation Activity and Navigation:**
- Purpose: Host Compose content, decide onboarding vs. app routes, receive share intents, start clipboard monitoring.
- Location: `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`
- Contains: `ComponentActivity`, `NavHost`, route definitions, share intent handling, clipboard broadcast receiver.
- Depends on: `PreferencesRepository`, Compose, Compose Navigation, `ClipboardMonitorService`, screen composables.
- Used by: Android launcher and share intents declared in `app/src/main/AndroidManifest.xml`.

**Compose Screens:**
- Purpose: Render user workflows and call viewmodel actions.
- Location: `app/src/main/java/com/recipegrabber/presentation/ui/screens/`
- Contains: `RecipeListScreen.kt`, `RecipeDetailScreen.kt`, `SettingsScreen.kt`, `LogViewerScreen.kt`, `RecipeExtractionBottomSheet.kt`, and onboarding composables in `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/`.
- Depends on: Viewmodels via `hiltViewModel()`, Material 3 Compose, local entities for display models.
- Used by: `MainActivity.kt` navigation graph and onboarding gate.

**Viewmodels:**
- Purpose: Convert repositories/use cases into observable UI state and expose user actions.
- Location: `app/src/main/java/com/recipegrabber/presentation/viewmodel/` and onboarding viewmodels in `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/`.
- Contains: `RecipeListViewModel.kt`, `RecipeDetailViewModel.kt`, `RecipeExtractionViewModel.kt`, `SettingsViewModel.kt`, `LogViewerViewModel.kt`, `OnboardingViewModel.kt`, `OnboardingDriveViewModel.kt`, `OnboardingApifyViewModel.kt`, `OnboardingLlmViewModel.kt`.
- Depends on: `RecipeRepository`, `PreferencesRepository`, `GoogleDriveService`, `ExtractRecipeUseCase`, `AppLogger`, `LlmProviderFactory`.
- Used by: Compose screens through Hilt navigation compose.

**Domain Use Cases:**
- Purpose: Coordinate business workflows that span repositories, providers, settings, and logging.
- Location: `app/src/main/java/com/recipegrabber/domain/usecase/`
- Contains: `ExtractRecipeUseCase.kt`, `SaveRecipeUseCase.kt`.
- Depends on: `ApifyService`, `LlmProviderFactory`, `PreferencesRepository`, `RecipeRepository`, `AppLogger`.
- Used by: `RecipeExtractionViewModel.kt`; `RecipeListViewModel.kt` also performs a direct extraction path through `LlmProviderFactory`.

**LLM Provider Abstraction:**
- Purpose: Hide provider-specific recipe extraction APIs behind a common interface.
- Location: `app/src/main/java/com/recipegrabber/domain/llm/`
- Contains: `LlmProvider.kt`, `ProviderType.kt`, `LlmProviderFactory.kt`, `LlmModels.kt`, `OpenAiProvider.kt`, `GeminiProvider.kt`, `ClaudeProvider.kt`, `KimiProvider.kt`.
- Depends on: `PreferencesRepository` for model/API key selection, `AppLogger`, Retrofit or OkHttp, Gson, local `Recipe`, `Ingredient`, and `Step` entities.
- Used by: `ExtractRecipeUseCase.kt`, `RecipeListViewModel.kt`, onboarding and settings UI for provider/model selection.

**Repository Layer:**
- Purpose: Provide app-facing data APIs over Room and DataStore.
- Location: `app/src/main/java/com/recipegrabber/data/repository/`
- Contains: `RecipeRepository.kt`, `PreferencesRepository.kt`.
- Depends on: DAOs in `app/src/main/java/com/recipegrabber/data/local/dao/`, DataStore preferences, Hilt application context.
- Used by: Viewmodels, use cases, `MainActivity.kt`, `ClipboardMonitorService.kt`, and `GoogleDriveService` callers.

**Local Persistence:**
- Purpose: Store recipes, ingredients, and steps in a relational database.
- Location: `app/src/main/java/com/recipegrabber/data/local/`
- Contains: `RecipeDatabase.kt`, `Migrations.kt`, DAOs in `data/local/dao/`, entities in `data/local/entity/`.
- Depends on: Room annotations and runtime.
- Used by: `DatabaseModule.kt` and `RecipeRepository.kt`.

**Remote Services:**
- Purpose: Wrap external API/service concerns behind injectable singleton classes.
- Location: `app/src/main/java/com/recipegrabber/data/remote/`
- Contains: `GoogleDriveService.kt` and `apify/ApifyService.kt`.
- Depends on: Google Sign-In/Drive SDKs, Retrofit, OkHttp, coroutines, `AppLogger`.
- Used by: `SettingsViewModel.kt`, `OnboardingDriveViewModel.kt`, and `ExtractRecipeUseCase.kt`.

**Dependency Injection:**
- Purpose: Provide singleton database and DAO instances and enable constructor injection.
- Location: `app/src/main/java/com/recipegrabber/di/`
- Contains: `DatabaseModule.kt`, `LlmModule.kt`.
- Depends on: Hilt, Room, `RecipeDatabase`.
- Used by: Hilt-injected activities, services, viewmodels, repositories, services, and use cases.

**Foreground Service:**
- Purpose: Monitor clipboard changes for supported video URLs.
- Location: `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`
- Contains: Android `Service`, notification channel creation, clipboard listener, URL validation, broadcast dispatch.
- Depends on: `PreferencesRepository`, `AppLogger`, Android notification and clipboard APIs.
- Used by: `MainActivity.kt` after onboarding is complete.

## Data Flow

**Recipe List Flow:**

1. `RecipeListScreen.kt` obtains `RecipeListViewModel` with `hiltViewModel()`.
2. `RecipeListViewModel.kt` combines `RecipeRepository.getAllRecipesWithDetails()` with local `MutableStateFlow` search/loading/error values.
3. `RecipeRepository.kt` delegates to `RecipeDao.getAllRecipesWithDetails()`.
4. `RecipeDao.kt` emits `Flow<List<RecipeWithDetails>>` from Room.
5. Compose collects `uiState` and renders recipe cards in `RecipeListScreen.kt`.

**Recipe Extraction Flow:**

1. `MainActivity.kt` receives a share intent or clipboard broadcast and shows `RecipeExtractionBottomSheet.kt`.
2. `RecipeExtractionBottomSheet.kt` calls `RecipeExtractionViewModel.startExtraction(videoUrl)`.
3. `RecipeExtractionViewModel.kt` collects `ExtractRecipeUseCase.progress` and invokes `ExtractRecipeUseCase`.
4. `ExtractRecipeUseCase.kt` detects platform, optionally calls `ApifyService.kt` for TikTok/Instagram scraping, reads provider settings from `PreferencesRepository.kt`, and creates a provider through `LlmProviderFactory.kt`.
5. The selected provider in `OpenAiProvider.kt`, `GeminiProvider.kt`, `ClaudeProvider.kt`, or `KimiProvider.kt` calls its external API and maps JSON into `Recipe` with transient `ingredients` and `steps`.
6. `SaveRecipeUseCase.kt` calls `RecipeRepository.insertRecipeWithDetails()`.
7. `RecipeRepository.kt` inserts the parent recipe through `RecipeDao.kt`, then inserts children through `IngredientDao.kt` and `StepDao.kt`.
8. The saved `Recipe.id` is returned to `MainActivity.kt`, which navigates to `recipe_detail/{recipeId}`.

**Settings and Onboarding Flow:**

1. `MainActivity.kt` observes `PreferencesRepository.onboardingCompleted`.
2. When false, `OnboardingScreen.kt` renders the three-step flow: Google Drive, Apify, and LLM selection.
3. Onboarding viewmodels in `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/` write selected settings to `PreferencesRepository.kt`.
4. `OnboardingViewModel.completeOnboarding()` sets the onboarding completion flag.
5. When true, `MainActivity.kt` shows the app `NavHost` and starts `ClipboardMonitorService.kt` in `onStart()`.

**Clipboard URL Flow:**

1. `MainActivity.kt` starts `ClipboardMonitorService.kt` after onboarding.
2. `ClipboardMonitorService.kt` listens to `ClipboardManager.OnPrimaryClipChangedListener`.
3. The service checks `PreferencesRepository.clipboardMonitorEnabled` and `PreferencesRepository.autoExtractRecipes`.
4. For supported video URLs, the service sends `ACTION_RECIPE_URL_DETECTED` with `EXTRA_VIDEO_URL`.
5. `MainActivity.kt` receives the broadcast, stores the URL in `pendingVideoUrl`, and opens `RecipeExtractionBottomSheet.kt` during composition.

**State Management:**
- Use `StateFlow` for most viewmodel UI state: `RecipeListViewModel.kt`, `RecipeDetailViewModel.kt`, `RecipeExtractionViewModel.kt`, `SettingsViewModel.kt`, and onboarding viewmodels.
- Use Compose `mutableStateOf` only where local or simple Compose-backed state is already established, such as `LogViewerViewModel.kt` and screen-local dialog/search state in `RecipeListScreen.kt`.
- Use Room `Flow` for live database-backed recipe screens and DataStore `Flow` for settings and onboarding state.

## Key Abstractions

**Recipe Aggregate:**
- Purpose: Represent a recipe with ingredients and steps.
- Examples: `app/src/main/java/com/recipegrabber/data/local/entity/Recipe.kt`, `app/src/main/java/com/recipegrabber/data/local/entity/RecipeWithDetails.kt`, `app/src/main/java/com/recipegrabber/data/local/entity/Ingredient.kt`, `app/src/main/java/com/recipegrabber/data/local/entity/Step.kt`.
- Pattern: Room parent/child entities with `@Relation`; extracted providers temporarily attach child lists to `Recipe.ingredients` and `Recipe.steps` transient fields before saving.

**Repository Facade:**
- Purpose: Keep Room/DataStore details out of UI.
- Examples: `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- Pattern: Injectable `@Singleton` classes exposing `Flow` reads and suspend writes.

**LLM Provider Strategy:**
- Purpose: Select recipe extraction implementation based on user settings.
- Examples: `app/src/main/java/com/recipegrabber/domain/llm/LlmProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/LlmProviderFactory.kt`, `app/src/main/java/com/recipegrabber/domain/llm/ProviderType.kt`.
- Pattern: Interface plus enum-backed factory, with provider classes injected as singletons.

**Use Case Orchestrator:**
- Purpose: Encapsulate multi-step workflows and progress reporting.
- Examples: `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`, `app/src/main/java/com/recipegrabber/domain/usecase/SaveRecipeUseCase.kt`.
- Pattern: Callable classes with `operator fun invoke`, coroutine dispatching, `Result`/sealed result objects, and logging.

**Settings Store:**
- Purpose: Persist onboarding, API keys, selected models, and app toggles.
- Examples: `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- Pattern: DataStore `Preferences` keys exposed as typed `Flow` properties with matching suspend setters.

**Log Facility:**
- Purpose: Centralize app logging and shareable log export.
- Examples: `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/LogViewerViewModel.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/LogViewerScreen.kt`.
- Pattern: Injectable singleton writing Android logs and app log files under internal storage.

## Entry Points

**Application Class:**
- Location: `app/src/main/java/com/recipegrabber/RecipeGrabberApplication.kt`
- Triggers: Android process startup through `app/src/main/AndroidManifest.xml`.
- Responsibilities: Enable Hilt injection.

**Main Activity:**
- Location: `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`
- Triggers: Launcher intent and `ACTION_SEND` text share intent declared in `app/src/main/AndroidManifest.xml`.
- Responsibilities: Theme setup, onboarding gate, navigation graph, share URL validation, extraction sheet state, clipboard monitor lifecycle.

**Foreground Clipboard Service:**
- Location: `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`
- Triggers: `MainActivity.startClipboardMonitor()` after onboarding.
- Responsibilities: Foreground notification, clipboard listener registration, video URL detection, recipe URL broadcast.

**Room Database:**
- Location: `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`
- Triggers: Hilt provider in `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`.
- Responsibilities: Define database version, entities, DAOs, and schema export.

**Hilt Modules:**
- Location: `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`, `app/src/main/java/com/recipegrabber/di/LlmModule.kt`
- Triggers: Hilt component creation.
- Responsibilities: Provide `RecipeDatabase`, `RecipeDao`, `IngredientDao`, and `StepDao`; LLM providers use constructor injection.

## Error Handling

**Strategy:** Viewmodels catch action-level exceptions and map them into UI state; use cases and service wrappers return sealed results or `Result<T>`; logs are written through `AppLogger`.

**Patterns:**
- Use sealed result objects for user-facing extraction outcomes in `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`.
- Use Kotlin `Result<T>` for provider/service operations in `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`, `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, and `app/src/main/java/com/recipegrabber/domain/llm/LlmProvider.kt`.
- Catch exceptions inside viewmodel actions and expose `error` or `message` fields in UI state, as in `RecipeListViewModel.kt`, `RecipeDetailViewModel.kt`, `RecipeExtractionViewModel.kt`, and `SettingsViewModel.kt`.
- Log operational failures with `AppLogger.e()` in provider, use case, service, and detail/extraction viewmodel code.

## Cross-Cutting Concerns

**Logging:** Use `AppLogger` from `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt` for app logs, Android logcat output, file export, and log clearing. Inject it where workflow diagnostics matter.

**Validation:** URL validation is duplicated in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt` and `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`. Provider/API key readiness is validated in `ExtractRecipeUseCase.kt` and provider classes before network calls.

**Authentication:** API keys are user-provided and stored through `PreferencesRepository.kt`; Google Drive authentication is handled by `GoogleDriveService.kt` with Google Sign-In and `DriveScopes.DRIVE_APPDATA`.

**Persistence:** Use Room for recipe data through `RecipeDatabase.kt` and DAO interfaces; use DataStore for app settings through `PreferencesRepository.kt`.

**Networking:** Use Retrofit for Apify, OpenAI, Claude, and Kimi clients; use OkHttp directly for Gemini in `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`.

---

*Architecture analysis: 2026-04-30*
