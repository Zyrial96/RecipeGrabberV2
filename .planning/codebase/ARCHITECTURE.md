---
last_mapped_commit: 3dfcf75a166b1af026a710d6e54c8de6b1c9c126
analysis_date: 2026-10-13
---

<!-- refreshed: 2026-10-13 -->
# Architecture

**Analysis Date:** 2026-10-13
**Last mapped commit:** `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`

## System Overview

```text
Android launcher/share intent
        |
        v
`app/src/main/.../presentation/MainActivity.kt`
        | onboarding gate, Compose navigation, clipboard receiver
        v
Compose screens + Hilt ViewModels
        |                         \
        v                          v
Use cases (`domain/usecase`)       Settings (`PreferencesRepository`)
        |                          | DataStore preferences
        v                          |
Apify + LLM providers              |
        |                          |
        +-----------> `SaveRecipeUseCase` <---+
                                |
                                v
              `RecipeRepository` -> Room DAO transaction
                                |
                                v
                 recipes + ingredients + steps
```

The project is a single Android application module (`app/`) using Compose UI, Hilt injection, Room persistence, Preferences DataStore, Retrofit/OkHttp integrations, and coroutine-based ViewModels/services. Package boundaries provide layers, but the application is not split into Gradle modules.

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| Application bootstrap | Registers the Hilt application entry | `app/src/main/java/com/recipegrabber/RecipeGrabberApplication.kt` |
| Activity shell | Handles launcher/share intents through the shared parser, onboarding gate, navigation, extraction sheet, and clipboard broadcasts | `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt` |
| Video link parser | Extracts and validates HTTPS video links from shared text and clipboard content | `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt` |
| Compose UI | Renders recipes, settings, logs, extraction, and onboarding workflows | `app/src/main/java/com/recipegrabber/presentation/ui/screens/` |
| ViewModels | Own screen state and launch suspend workflows | `app/src/main/java/com/recipegrabber/presentation/viewmodel/` |
| Extraction orchestration | Validates URLs, scrapes optional video metadata, selects provider, validates output, and saves | `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt` |
| Persistence facade | Exposes recipe reads/writes without exposing DAO details to UI | `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt` |
| Settings facade | Exposes typed DataStore flows and setters | `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt` |
| Local database | Defines Room database, entities, relations, migrations, and DAOs | `app/src/main/java/com/recipegrabber/data/local/` |
| Clipboard service | Runs a foreground clipboard listener and broadcasts supported URLs | `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt` |

## Pattern Overview

**Overall:** Single-module Android MVVM with layered data/domain/presentation packages and dependency injection.

**Key Characteristics:**
- Compose screens obtain Hilt ViewModels and render `StateFlow`/DataStore/Room flows.
- `MainActivity` is the composition root for navigation and app-level intent/clipboard events.
- Repository and use-case classes are `@Singleton` constructor-injected services.
- Recipe parent/child persistence is grouped behind `RecipeDao` `@Transaction` methods.
- Provider selection uses `LlmProviderFactory` and the `LlmProvider` abstraction.

## Layers

**Presentation:**
- Purpose: Render UI and translate user/system events into ViewModel actions.
- Location: `app/src/main/java/com/recipegrabber/presentation/`
- Contains: `MainActivity.kt`, screens, onboarding, theme, utilities, and ViewModels.
- Depends on: Domain use cases, repositories, Android/Compose APIs.
- Used by: Android manifest entry points and Compose runtime.

**Domain orchestration:**
- Purpose: Coordinate extraction and save workflows independently of screen composition.
- Location: `app/src/main/java/com/recipegrabber/domain/usecase/`
- Contains: `ExtractRecipeUseCase.kt` and `SaveRecipeUseCase.kt`.
- Depends on: `ApifyService`, `LlmProviderFactory`, `PreferencesRepository`, `RecipeRepository`, and `AppLogger`.
- Used by: `RecipeExtractionViewModel.kt` and extraction UI flows.

**Provider domain:**
- Purpose: Normalize multiple LLM APIs behind one extraction contract.
- Location: `app/src/main/java/com/recipegrabber/domain/llm/`
- Contains: provider interface, factory, provider implementations, request/response helpers, and model definitions.
- Depends on: HTTP clients, settings, entities, and logging.
- Used by: `ExtractRecipeUseCase.kt` and provider selection/onboarding code.

**Data:**
- Purpose: Implement persistence, remote service adapters, and logging.
- Location: `app/src/main/java/com/recipegrabber/data/`
- Contains: Room local data, repositories, Google Drive/Apify clients, and `AppLogger.kt`.
- Depends on: Android storage/context, Room, DataStore, Retrofit/OkHttp, and Google SDKs.
- Used by: domain and presentation layers through injected facades.

## Data Flow

### Primary Extraction Path

1. Android invokes `MainActivity.onCreate()` or `onNewIntent()` for launcher/share input (`app/src/main/java/com/recipegrabber/presentation/MainActivity.kt:60`).
2. `handleShareIntent()` passes shared text to `VideoLinkParser.findVideoUrl()` and stores only a parsed HTTPS video URL (`app/src/main/java/com/recipegrabber/presentation/MainActivity.kt:166`, `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt:10`).
3. `ClipboardMonitorService` uses the same parser before broadcasting the canonical extracted URL (`app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt:88`, `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt:10`). The in-process receiver handles `ACTION_RECIPE_URL_DETECTED` (`MainActivity.kt:50`).
4. `RecipeExtractionBottomSheet` calls `RecipeExtractionViewModel.startExtraction()` (`app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt:43`).
5. `ExtractRecipeUseCase.invokeWithProgress()` validates HTTPS URLs, reads DataStore provider settings, optionally calls Apify for TikTok/Instagram, selects an LLM provider, validates the returned aggregate, and delegates saving (`app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt:43`).
6. `SaveRecipeUseCase` passes the parent and transient child lists to `RecipeRepository` (`app/src/main/java/com/recipegrabber/domain/usecase/SaveRecipeUseCase.kt:16`).
7. `RecipeDao.insertRecipeWithDetails()` inserts the recipe and remaps child foreign keys inside one Room transaction (`app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt:51`).
8. The returned ID is exposed to the bottom sheet and `MainActivity` navigates to `recipe_detail/{recipeId}`.

### Read and Settings Paths

1. `RecipeListViewModel` collects `RecipeRepository.getAllRecipesWithDetails()` and exposes list state to `RecipeListScreen.kt`.
2. `RecipeDetailViewModel` collects `getRecipeWithDetails(id)` and issues favorite/delete/update actions through the repository.
3. `PreferencesRepository` maps `Context.dataStore` to typed flows; onboarding, theme, provider, credentials, Drive, and clipboard flags all use this store (`app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt:18`).
4. `MainActivity.onStart()` reads onboarding and clipboard flags with `first()`, then starts/stops the foreground service and registers its receiver (`MainActivity.kt:173`).

**State Management:** ViewModels expose `StateFlow` UI state; Room `Flow` drives recipe screens; DataStore `Flow` drives settings and onboarding; activity-owned Compose state holds pending URL and extraction-sheet visibility.

**Direct media:** `GeminiProvider` validates input before authorization. YouTube retains its URL media part. Direct files stream into bounded cache (100 MiB maximum). Up to 8 MiB, `GeminiFileUploader` attaches base64 inline video, avoiding Files calls and extra scope; larger files use resumable upload, ACTIVE polling and best-effort cleanup. The original source URL is retained. Google credentials never enter source requests; private media DNS and unsafe redirects are rejected. Both a real inline WebM and a 21.2MB Files WebM import/persistence/restart are verified; the latter uses the separately optional combined-scope authorization.

**Extraction lifecycle:** `RecipeExtractionViewModel` keeps the active extraction `Job`, cancels it before replacement, and cancels its progress collector in `finally` (`app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt:43`). `RecipeExtractionBottomSheet` starts work keyed by `videoUrl` and resets the ViewModel on disposal through `DisposableEffect` (`app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeExtractionBottomSheet.kt:68`). Scraping fallback results are mapped to their actual error or missing-provider message (`RecipeExtractionViewModel.kt:111`).

## Transaction Ownership

`RecipeDao` owns the database transaction boundary for inserting and updating the recipe aggregate. `RecipeRepository` delegates without opening a second transaction, and `SaveRecipeUseCase` owns orchestration/error logging rather than persistence atomicity. The `@Transaction` methods insert/update the recipe, remove or create children, and assign generated parent IDs (`app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt:51`). Room is built as a singleton with explicit migrations in `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt:21`.

## Entry Points

**Android application:** `app/src/main/AndroidManifest.xml` names `.RecipeGrabberApplication` and exported `presentation.MainActivity`.

**Main activity:** `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt` receives launcher and `ACTION_SEND` text intents, gates onboarding, hosts routes, and controls clipboard monitoring.

**Foreground service:** `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt` is non-exported, starts as `specialUse`, listens to clipboard changes, and emits package-scoped broadcasts.

**Optional Files authorization update at `aa4a8eb`:** Settings has separate base/Files result launchers and an explicit permission disclosure. The uploader invokes a Files-token callback only after the downloaded size exceeds 8 MiB; the provider then selects a combined-scope token for file operations and generation. Base authorization remains unchanged. After personal Google confirmation, the Mi 11 successfully imported/persisted/reopened a 21.2MB WebM.

**Persisted list-card update at `cd783e4`:** `RecipeCard` counts the loaded `RecipeWithDetails.ingredients` relation, never the transient embedded `Recipe.ingredients`. No repository/DAO/schema contract change is involved; two native regressions and the real imported recipes verify the fix.

**Dependency injection:** `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt` provides Room and DAOs; `app/src/main/java/com/recipegrabber/di/LlmModule.kt` provides LLM dependencies; Hilt supplies the rest through constructors.

## Architectural Constraints

- **Modules:** One Gradle module, `:app`, configured by `settings.gradle.kts`.
- **Runtime:** Android `minSdk 31`, `targetSdk 35`, Java/Kotlin JVM target 17 (`app/build.gradle.kts`).
- **Global state:** Singleton repositories, use cases, services, and logger are managed by Hilt; `PreferencesRepository` owns one application DataStore delegate.
- **Process lifecycle:** Clipboard monitoring is a foreground service with `START_STICKY`; activity registration is lifecycle-bound.
- **Persistence model:** `Recipe` carries transient `ingredients` and `steps`; persisted relations are represented by `RecipeWithDetails`.

## Current Issues Requiring Parent Action

- `ExtractRecipeUseCase.kt:91` computes `contextUrl` and `description` from scraped data, but the provider call receives only `contextUrl`; scraped description is only used as a fallback description. Confirm this is intentional for provider behavior.

## Error Handling

`ExtractRecipeUseCase` returns sealed extraction outcomes and preserves coroutine cancellation. Provider/service operations commonly return `Result`; ViewModels map outcomes and exceptions into screen state. Persistence exceptions are logged and rethrown by `SaveRecipeUseCase` (`app/src/main/java/com/recipegrabber/domain/usecase/SaveRecipeUseCase.kt:26`).

## Cross-Cutting Concerns

**Logging:** Use `AppLogger` (`app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`) for operational diagnostics and log export.

**Validation:** `VideoLinkParser` centralizes HTTPS, credential-free URL, host, and video-path validation for share and clipboard entry points; extraction performs its own URI and recipe completeness checks.

**Lifecycle cleanup:** Extraction cancellation/reset and bottom-sheet disposal are explicit; replacement URLs cancel the prior extraction before starting the new one.

**Authentication:** User provider credentials and Google authorization state are read/written through `PreferencesRepository`; no credential values belong in architecture documentation.

**Preferences:** The settings store is the authority for onboarding completion, selected provider/model, provider auth mode, Drive sync, theme, clipboard monitoring, and auto-extraction toggles.

---

*Architecture analysis: 2026-10-13*
*Last mapped commit: `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`*
