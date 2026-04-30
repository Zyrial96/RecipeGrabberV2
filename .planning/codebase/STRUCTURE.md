# Codebase Structure

**Analysis Date:** 2026-04-30

## Directory Layout

```text
RecipeGrabberV2/
├── app/                         # Android application module
│   ├── build.gradle.kts         # App plugin, Android, Compose, Room, Hilt, network, test dependencies
│   ├── proguard-rules.pro       # Release shrinker rules
│   ├── schemas/                 # Exported Room schema JSON files
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/recipegrabber/
│       │   │   ├── RecipeGrabberApplication.kt
│       │   │   ├── data/
│       │   │   ├── di/
│       │   │   ├── domain/
│       │   │   ├── presentation/
│       │   │   └── service/
│       │   └── res/
│       └── test/java/com/recipegrabber/
├── build.gradle.kts             # Root Gradle plugin aliases
├── gradle/libs.versions.toml     # Version catalog
├── gradle.properties            # Gradle/Android build properties
├── settings.gradle.kts          # Single-module Gradle settings
├── README.md                    # Project overview
├── MODEL_ROUTING.md             # Model routing documentation
├── openclaw-routing-config.json # Model routing configuration document
└── .planning/codebase/           # GSD codebase analysis documents
```

## Directory Purposes

**Root:**
- Purpose: Gradle wrapper, root build configuration, documentation, routing notes, and planning output.
- Contains: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradlew`, `README.md`, `MODEL_ROUTING.md`, `openclaw-routing-config.json`, `.planning/codebase/`.
- Key files: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`.

**`app/`:**
- Purpose: Main Android application module.
- Contains: Android Gradle configuration, source sets, exported Room schemas, ProGuard config, generated build output.
- Key files: `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json`.

**`app/src/main/java/com/recipegrabber/`:**
- Purpose: Kotlin application source root for package `com.recipegrabber`.
- Contains: Application bootstrap plus `data`, `di`, `domain`, `presentation`, and `service` packages.
- Key files: `app/src/main/java/com/recipegrabber/RecipeGrabberApplication.kt`.

**`app/src/main/java/com/recipegrabber/data/`:**
- Purpose: Persistence, remote service, repository, and logging implementation layer.
- Contains: `local/`, `remote/`, `repository/`, `logging/`.
- Key files: `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.

**`app/src/main/java/com/recipegrabber/data/local/`:**
- Purpose: Room database definition, migrations, DAOs, and entities.
- Contains: `RecipeDatabase.kt`, `Migrations.kt`, `dao/`, `entity/`.
- Key files: `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`, `app/src/main/java/com/recipegrabber/data/local/Migrations.kt`.

**`app/src/main/java/com/recipegrabber/data/local/dao/`:**
- Purpose: Room query interfaces.
- Contains: `RecipeDao.kt`, `IngredientDao.kt`, `StepDao.kt`.
- Key files: `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt`.

**`app/src/main/java/com/recipegrabber/data/local/entity/`:**
- Purpose: Room table models and relation projections.
- Contains: `Recipe.kt`, `Ingredient.kt`, `Step.kt`, `RecipeWithDetails.kt`.
- Key files: `app/src/main/java/com/recipegrabber/data/local/entity/Recipe.kt`, `app/src/main/java/com/recipegrabber/data/local/entity/RecipeWithDetails.kt`.

**`app/src/main/java/com/recipegrabber/data/remote/`:**
- Purpose: External service clients that are injectable from viewmodels/use cases.
- Contains: `GoogleDriveService.kt`, `apify/ApifyService.kt`.
- Key files: `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.

**`app/src/main/java/com/recipegrabber/data/repository/`:**
- Purpose: App-facing data access facades.
- Contains: `RecipeRepository.kt`, `PreferencesRepository.kt`.
- Key files: `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.

**`app/src/main/java/com/recipegrabber/data/logging/`:**
- Purpose: File-backed and Logcat-backed logging.
- Contains: `AppLogger.kt`.
- Key files: `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.

**`app/src/main/java/com/recipegrabber/domain/`:**
- Purpose: Business workflows and provider abstractions.
- Contains: `usecase/` and `llm/`.
- Key files: `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`, `app/src/main/java/com/recipegrabber/domain/llm/LlmProvider.kt`.

**`app/src/main/java/com/recipegrabber/domain/usecase/`:**
- Purpose: Orchestrated application actions.
- Contains: `ExtractRecipeUseCase.kt`, `SaveRecipeUseCase.kt`.
- Key files: `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`.

**`app/src/main/java/com/recipegrabber/domain/llm/`:**
- Purpose: LLM provider strategy and provider-specific HTTP clients/parsers.
- Contains: `LlmProvider.kt`, `ProviderType.kt`, `LlmModels.kt`, `LlmProviderFactory.kt`, `OpenAiProvider.kt`, `GeminiProvider.kt`, `ClaudeProvider.kt`, `KimiProvider.kt`.
- Key files: `app/src/main/java/com/recipegrabber/domain/llm/LlmProviderFactory.kt`, `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`.

**`app/src/main/java/com/recipegrabber/di/`:**
- Purpose: Hilt modules.
- Contains: `DatabaseModule.kt`, `LlmModule.kt`.
- Key files: `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`.

**`app/src/main/java/com/recipegrabber/presentation/`:**
- Purpose: Activity entry point plus Compose UI and viewmodels.
- Contains: `MainActivity.kt`, `ui/`, `viewmodel/`.
- Key files: `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`.

**`app/src/main/java/com/recipegrabber/presentation/ui/screens/`:**
- Purpose: Feature screens and reusable screen-local composables.
- Contains: `RecipeListScreen.kt`, `RecipeDetailScreen.kt`, `SettingsScreen.kt`, `LogViewerScreen.kt`, `RecipeExtractionBottomSheet.kt`, `onboarding/`.
- Key files: `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeListScreen.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeExtractionBottomSheet.kt`.

**`app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/`:**
- Purpose: Onboarding screens, steps, and onboarding-specific viewmodels.
- Contains: `OnboardingScreen.kt`, `GoogleDriveStep.kt`, `ApifyStep.kt`, `LlmSelectionStep.kt`, `OnboardingViewModel.kt`, `OnboardingDriveViewModel.kt`, `OnboardingApifyViewModel.kt`, `OnboardingLlmViewModel.kt`.
- Key files: `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/OnboardingScreen.kt`.

**`app/src/main/java/com/recipegrabber/presentation/ui/theme/`:**
- Purpose: Compose Material theme configuration.
- Contains: `Theme.kt`, `Color.kt`, `Type.kt`.
- Key files: `app/src/main/java/com/recipegrabber/presentation/ui/theme/Theme.kt`.

**`app/src/main/java/com/recipegrabber/presentation/viewmodel/`:**
- Purpose: Shared app screen viewmodels.
- Contains: `RecipeListViewModel.kt`, `RecipeDetailViewModel.kt`, `RecipeExtractionViewModel.kt`, `SettingsViewModel.kt`, `LogViewerViewModel.kt`.
- Key files: `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`.

**`app/src/main/java/com/recipegrabber/service/`:**
- Purpose: Android service layer for app background/foreground behaviors.
- Contains: `ClipboardMonitorService.kt`.
- Key files: `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`.

**`app/src/main/res/`:**
- Purpose: Android resources.
- Contains: launcher assets, theme XML, strings, backup rules, file provider paths.
- Key files: `app/src/main/res/values/strings.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/xml/file_paths.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`.

**`app/src/test/java/com/recipegrabber/`:**
- Purpose: JVM unit tests.
- Contains: `LlmProviderTest.kt`, `RecipeRepositoryTest.kt`.
- Key files: `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`, `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/recipegrabber/RecipeGrabberApplication.kt`: Hilt application bootstrap.
- `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`: Main activity, navigation graph, onboarding gate, share intent handling, clipboard receiver.
- `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`: Foreground service for clipboard URL monitoring.
- `app/src/main/AndroidManifest.xml`: Declares application class, activity intents, foreground service, FileProvider, and required permissions.

**Configuration:**
- `settings.gradle.kts`: Defines the root project and includes only `:app`.
- `build.gradle.kts`: Declares root plugin aliases for Android, Kotlin, KSP, and Hilt.
- `app/build.gradle.kts`: Configures Android SDK levels, Compose, Room schema export, Hilt, networking, Google Drive, and tests.
- `gradle/libs.versions.toml`: Central version catalog used by Gradle plugin and dependency aliases.
- `gradle.properties`: Gradle and Android build properties.
- `app/proguard-rules.pro`: App-specific ProGuard/R8 rules.

**Core Logic:**
- `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`: Recipe extraction orchestration.
- `app/src/main/java/com/recipegrabber/domain/usecase/SaveRecipeUseCase.kt`: Recipe aggregate persistence.
- `app/src/main/java/com/recipegrabber/domain/llm/LlmProvider.kt`: Provider interface.
- `app/src/main/java/com/recipegrabber/domain/llm/LlmProviderFactory.kt`: Provider selection.
- `app/src/main/java/com/recipegrabber/domain/llm/LlmModels.kt`: Provider/model metadata.
- `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`: OpenAI extraction implementation.
- `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`: Gemini extraction implementation.
- `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`: Claude extraction implementation.
- `app/src/main/java/com/recipegrabber/domain/llm/KimiProvider.kt`: Kimi extraction implementation.

**Data Access:**
- `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`: Room database.
- `app/src/main/java/com/recipegrabber/data/local/Migrations.kt`: Room migration definitions.
- `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt`: Recipe queries and mutations.
- `app/src/main/java/com/recipegrabber/data/local/dao/IngredientDao.kt`: Ingredient queries and mutations.
- `app/src/main/java/com/recipegrabber/data/local/dao/StepDao.kt`: Step queries and mutations.
- `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`: Recipe data facade.
- `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`: DataStore settings facade.

**External Services:**
- `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`: Apify actor runs and dataset polling.
- `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`: Google Sign-In and Drive app data operations.

**Presentation:**
- `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeListScreen.kt`: Recipe list and manual URL entry.
- `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeDetailScreen.kt`: Recipe detail, scaling, sharing, favorite/delete actions.
- `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeExtractionBottomSheet.kt`: Extraction progress/success/error bottom sheet.
- `app/src/main/java/com/recipegrabber/presentation/ui/screens/SettingsScreen.kt`: Provider, API key, Google Drive, and app setting controls.
- `app/src/main/java/com/recipegrabber/presentation/ui/screens/LogViewerScreen.kt`: Log display/share/clear screen.
- `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/OnboardingScreen.kt`: Three-step onboarding coordinator.

**Testing:**
- `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`: LLM provider unit tests.
- `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`: Repository unit tests.

## Naming Conventions

**Files:**
- Use PascalCase Kotlin file names matching the primary class or composable: `RecipeListViewModel.kt`, `ExtractRecipeUseCase.kt`, `GoogleDriveService.kt`.
- Use `*Screen.kt` for full Compose screens: `RecipeListScreen.kt`, `SettingsScreen.kt`, `OnboardingScreen.kt`.
- Use `*ViewModel.kt` for viewmodels: `RecipeDetailViewModel.kt`, `OnboardingLlmViewModel.kt`.
- Use `*UseCase.kt` for domain workflow classes: `ExtractRecipeUseCase.kt`, `SaveRecipeUseCase.kt`.
- Use `*Repository.kt` for repository facades: `RecipeRepository.kt`, `PreferencesRepository.kt`.
- Use `*Dao.kt` for Room DAOs: `RecipeDao.kt`, `IngredientDao.kt`, `StepDao.kt`.
- Use `*Service.kt` for service/client wrappers and Android services: `GoogleDriveService.kt`, `ApifyService.kt`, `ClipboardMonitorService.kt`.

**Directories:**
- Use package-layer directories under `app/src/main/java/com/recipegrabber/`: `data`, `domain`, `presentation`, `di`, `service`.
- Place persistence subpackages under `data/local/dao` and `data/local/entity`.
- Place external client subpackages under `data/remote/`, with provider-specific grouping like `data/remote/apify/`.
- Place Compose screens under `presentation/ui/screens/`; group multi-file flows under a nested feature directory like `presentation/ui/screens/onboarding/`.
- Place app-wide screen viewmodels under `presentation/viewmodel/`; onboarding-specific viewmodels live beside onboarding step composables in `presentation/ui/screens/onboarding/`.

## Where to Add New Code

**New Feature:**
- Primary UI: add screen composables under `app/src/main/java/com/recipegrabber/presentation/ui/screens/` or a feature subdirectory like `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/`.
- UI state/actions: add a `@HiltViewModel` under `app/src/main/java/com/recipegrabber/presentation/viewmodel/` unless the feature is tightly scoped to onboarding.
- Navigation: add route entries in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`.
- Domain workflow: add callable use case classes under `app/src/main/java/com/recipegrabber/domain/usecase/`.
- Tests: add JVM tests under `app/src/test/java/com/recipegrabber/`.

**New Component/Module:**
- Shared screen-level Compose components: keep near the screen file in `app/src/main/java/com/recipegrabber/presentation/ui/screens/` when used by one feature.
- Reusable app-wide UI components: create a focused package under `app/src/main/java/com/recipegrabber/presentation/ui/` only when multiple screens share the component.
- New Android service: add under `app/src/main/java/com/recipegrabber/service/` and declare it in `app/src/main/AndroidManifest.xml`.
- New Hilt provider module: add under `app/src/main/java/com/recipegrabber/di/`.

**Utilities:**
- Shared logging stays in `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.
- Data-layer helpers belong under `app/src/main/java/com/recipegrabber/data/`.
- Domain-only abstractions belong under `app/src/main/java/com/recipegrabber/domain/`.
- Avoid placing business logic in `app/src/main/java/com/recipegrabber/presentation/ui/screens/`; route it through viewmodels and use cases.

**New Persistence Model:**
- Entity: add to `app/src/main/java/com/recipegrabber/data/local/entity/`.
- DAO: add to `app/src/main/java/com/recipegrabber/data/local/dao/`.
- Database registration: update `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`.
- Migration: add to `app/src/main/java/com/recipegrabber/data/local/Migrations.kt`.
- Hilt provider: add DAO provider to `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt`.
- Repository method: expose app-facing operations from `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt` or a new repository in `app/src/main/java/com/recipegrabber/data/repository/`.

**New LLM Provider:**
- Provider enum: add a value to `app/src/main/java/com/recipegrabber/domain/llm/ProviderType.kt`.
- Model metadata: add models to `app/src/main/java/com/recipegrabber/domain/llm/LlmModels.kt`.
- Provider implementation: add `NewProvider.kt` under `app/src/main/java/com/recipegrabber/domain/llm/` implementing `LlmProvider`.
- Factory selection: update `app/src/main/java/com/recipegrabber/domain/llm/LlmProviderFactory.kt`.
- Settings persistence: add DataStore keys and setters/getters to `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- Settings/onboarding UI: update `app/src/main/java/com/recipegrabber/presentation/ui/screens/SettingsScreen.kt` and `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/LlmSelectionStep.kt`.

**New External Integration:**
- Client/service wrapper: add under `app/src/main/java/com/recipegrabber/data/remote/`.
- User-configurable settings: add keys and flows to `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- Workflow orchestration: call the service from a use case in `app/src/main/java/com/recipegrabber/domain/usecase/`.
- UI configuration: expose settings through `SettingsViewModel.kt` and `SettingsScreen.kt`.

## Special Directories

**`app/build/`:**
- Purpose: Android Gradle generated outputs.
- Generated: Yes.
- Committed: No.

**`build/`:**
- Purpose: Root Gradle generated outputs.
- Generated: Yes.
- Committed: No.

**`.gradle/`:**
- Purpose: Gradle cache and task state.
- Generated: Yes.
- Committed: No.

**`.kotlin/`:**
- Purpose: Kotlin tooling/session generated state.
- Generated: Yes.
- Committed: No.

**`.idea/`:**
- Purpose: IntelliJ/Android Studio project metadata.
- Generated: Tool-managed.
- Committed: Partially project-dependent.

**`.vscode/`:**
- Purpose: VS Code workspace metadata.
- Generated: Tool-managed.
- Committed: Project-dependent.

**`app/schemas/`:**
- Purpose: Room schema exports for migration validation.
- Generated: Yes by Room/KSP.
- Committed: Yes, keep schema JSON such as `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json`.

**`app/src/main/res/mipmap-*`:**
- Purpose: Launcher icon density assets.
- Generated: Tool-managed assets.
- Committed: Yes.

**`.planning/codebase/`:**
- Purpose: GSD architecture, stack, conventions, testing, integration, and concerns maps.
- Generated: Yes by mapping commands.
- Committed: Yes when planning artifacts are tracked.

---

*Structure analysis: 2026-04-30*
