---
last_mapped_commit: 3dfcf75a166b1af026a710d6e54c8de6b1c9c126
analysis_date: 2026-10-13
---

# Codebase Structure

**23 October visual refresh (`52b0490`):**
`presentation/ui/components/RecipeVisuals.kt` contains shared Coil-backed recipe
artwork, tooltip icon actions and metadata. `presentation/util/RecipeArtworkUrl.kt`
selects validated HTTPS thumbnails/public YouTube previews and has JVM tests.
`VisualRefreshTest.kt` covers the refreshed Compose controls and compact/large-font
layouts. Persistence/provider ownership remains unchanged.

**Analysis Date:** 2026-10-13
**Last mapped commit:** `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`

**Focused update at `575b965`:** `data/security/` contains `SecretCipher`, `AndroidKeystoreSecretCipher` and `CredentialMigration`; `domain/backup/` contains `RecipeBackupCodec` and `DriveBackupCoordinator`; `data/remote/DriveBackupAuthorization.kt` and `DriveBackupTransport.kt` implement the active backup boundary. `data/logging/DiagnosticSanitizer.kt` and `PrivateLogStore.kt` bound diagnostics. Room schemas 3/4 are exported; `src/audit/` names the isolated QA app. Native credential/privacy/backup/UI tests and gated `DriveBackupLiveProbeTest` supplement the baseline tree below.

## Directory Layout

```text
RecipeGrabberV2/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   ├── schemas/                         # Exported Room schemas
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/recipegrabber/
│       │   │   ├── RecipeGrabberApplication.kt
│       │   │   ├── data/                 # persistence, remote adapters, logging
│       │   │   ├── di/                   # Hilt modules
│       │   │   ├── domain/               # providers and use cases
│       │   │   ├── presentation/         # activity, screens, ViewModels, theme
│       │   │   └── service/              # clipboard foreground service
│       │   └── res/                      # Android resources
│       ├── test/                         # JVM unit tests
│       └── androidTest/                  # device/instrumentation tests
├── gradle/
│   └── libs.versions.toml                # Version catalog
├── .planning/codebase/                   # GSD maps
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── settings.gradle.kts
├── README.md
├── CONTRIBUTING.md
├── MODEL_ROUTING.md
└── openclaw-routing-config.json
```

## Directory Purposes

**`app/src/main/java/com/recipegrabber/data/local/`:** Room database, migrations, DAOs, entities, and relation projections. Key files are `RecipeDatabase.kt`, `Migrations.kt`, `dao/RecipeDao.kt`, `entity/Recipe.kt`, and `entity/RecipeWithDetails.kt`.

**`app/src/main/java/com/recipegrabber/data/repository/`:** App-facing facades. `RecipeRepository.kt` delegates recipe aggregate access to DAOs; `PreferencesRepository.kt` owns the application Preferences DataStore.

**`app/src/main/java/com/recipegrabber/data/remote/`:** External service adapters, including `GoogleDriveService.kt`, `GoogleAiAuthorizationRequestFactory.kt`, and `apify/ApifyService.kt`.

**`app/src/main/java/com/recipegrabber/domain/llm/`:** Provider abstraction and provider-specific HTTP/request parsing code: `LlmProvider.kt`, `LlmProviderFactory.kt`, `OpenAiProvider.kt`, `GeminiProvider.kt`, `ClaudeProvider.kt`, and `KimiProvider.kt`.

`GeminiFileUploader.kt` owns bounded direct-media download, small-video inline encoding, larger-file resumable upload, readiness polling and cleanup. `GeminiHttpCall.kt` keeps cancellation attached during body reads. `GeminiFileUploaderTest.kt` tests its call-factory boundaries, inline/Files routing and cleanup without paid requests.

**`app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt`:** Shared URL extraction and validation for `ACTION_SEND` text and clipboard text. It accepts HTTPS links without userinfo, validates supported video hosts/path forms, and returns the first matching URL.

**`app/src/main/java/com/recipegrabber/domain/usecase/`:** Multi-step workflows. `ExtractRecipeUseCase.kt` coordinates extraction; `SaveRecipeUseCase.kt` delegates aggregate persistence.

**`app/src/main/java/com/recipegrabber/presentation/`:** Android UI boundary. `MainActivity.kt` owns app-level navigation and intents; `ui/screens/` contains Compose screens; `ui/screens/onboarding/` contains onboarding screens and step ViewModels; `viewmodel/` contains primary screen ViewModels.

**`app/src/main/java/com/recipegrabber/service/`:** Lifecycle-managed Android services. `ClipboardMonitorService.kt` is the current foreground service.

**`app/src/main/res/`:** Manifest-referenced Android resources: launcher drawables/mipmaps, XML backup/file-provider paths, strings, and themes.

**`app/src/test/java/com/recipegrabber/`:** JVM tests for extraction, provider parsing/authentication, onboarding completion, scaling, and repository behavior.

**`app/src/androidTest/java/com/recipegrabber/`:** Instrumented tests for Room persistence, recipe detail UI, persisted recipe-card counts, media payload/rejections, and optional Gemini network probes. `RecipeCardTest` was added at `cd783e4` as a focused follow-up to the original map.

**`app/schemas/`:** Committed Room schema export for `RecipeDatabase`, currently version `3` in `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json`.

## Key File Locations

**Entry Points:**
- `app/src/main/AndroidManifest.xml`: application, exported launcher/share activity, non-exported clipboard service, and FileProvider declarations.
- `app/src/main/java/com/recipegrabber/RecipeGrabberApplication.kt`: Hilt application bootstrap.
- `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`: UI and system-event entry point.
- `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt`: shared link-recognition boundary used by both entry points.

**Configuration:**
- `settings.gradle.kts`: single `:app` module and repository policy.
- `app/build.gradle.kts`: Android SDK, build types, Compose, Room, Hilt, networking, Google, and test dependencies.
- `gradle/libs.versions.toml`: dependency/plugin versions.
- `gradle.properties`: Gradle project properties.

**Core Logic:**
- `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`: extraction data flow and output validation.
- `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`: recipe persistence facade.
- `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt`: aggregate transaction ownership.
- `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`: settings authority.

**Testing:**
- `app/src/test/java/com/recipegrabber/`: unit tests.
- `app/src/androidTest/java/com/recipegrabber/`: instrumented tests.

## Naming Conventions

**Files:** Kotlin classes use PascalCase filenames matching the primary class, such as `RecipeDetailViewModel.kt`; Compose screen files use the screen name, such as `RecipeListScreen.kt`; DAO/entity files are similarly named by type.

**Directories:** Lowercase package directories follow Kotlin package names: `data/local/dao`, `domain/usecase`, `presentation/viewmodel`.

**Routes:** Compose navigation routes are lowercase snake-style literals in `MainActivity.kt`, including `recipe_list`, `recipe_detail/{recipeId}`, `settings`, and `logs`.

## Where to Add New Code

**New feature:** Put user-facing Compose code in `app/src/main/java/com/recipegrabber/presentation/ui/screens/`, its state/action owner in `app/src/main/java/com/recipegrabber/presentation/viewmodel/`, and cross-layer workflow coordination in `app/src/main/java/com/recipegrabber/domain/usecase/`.

**New persistence model:** Add the Room entity and relation projection under `app/src/main/java/com/recipegrabber/data/local/entity/`, DAO operations under `app/src/main/java/com/recipegrabber/data/local/dao/`, and register the entity/database version in `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`. Add a migration in `Migrations.kt`, wire it in `DatabaseModule.kt`, and export/update `app/schemas/` through the build.

**New external provider:** Add the shared contract/model in `app/src/main/java/com/recipegrabber/domain/llm/`, provider implementation and factory branch there, settings keys/setters in `PreferencesRepository.kt`, and UI selection in onboarding/settings. Keep credentials out of source and documentation.

**New link recognition rule:** Extend `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt` and its tests; keep `MainActivity.kt` and `ClipboardMonitorService.kt` as callers rather than duplicating host/path predicates.

**New remote integration:** Place the adapter in `app/src/main/java/com/recipegrabber/data/remote/` or its service subdirectory, inject it through Hilt, and keep orchestration in a use case rather than a Compose screen.

**New component/module:** Place reusable Compose pieces beside related screens in `presentation/ui/`; place non-UI shared formatters in `presentation/util/`. There is no separate feature-module convention.

**Tests:** Co-locate JVM tests by package under `app/src/test/java/com/recipegrabber/`; use `app/src/androidTest/java/com/recipegrabber/` when Android runtime, Room instrumentation, or Compose UI infrastructure is required.

## Special Directories

**`app/build/`, `.gradle/`, `.kotlin/`:** Generated Gradle/Kotlin build state. Do not add application source here; treat as generated/untracked build output.

**`app/schemas/`:** Generated/exported Room schema artifacts. Committed for migration review; update through the Room build configuration when the schema changes.

**`app/src/main/res/`:** Android resource tree. Manifest references must resolve to resources here, including `xml/file_paths.xml` for the FileProvider.

**`.planning/codebase/`:** Generated GSD analysis documents. Refresh only the assigned map document when parallel mappers own the others.

## Current Layout Notes

- The repository is a single-module Android project; `settings.gradle.kts` includes only `:app`.
- The current source tree contains separate `data`, `domain`, `presentation`, and `service` packages under one application package.
- Share and clipboard recognition now converge through `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt`.
- No `graphify-out/graph.json` is present in the live repository, so structure verification uses the checked-out source and configuration.
- The old Desktop checkout is absent; all paths in this map are relative to `/Users/zyrial/Projects/RecipeGrabberV2`.

---

*Structure analysis: 2026-10-13*
*Last mapped commit: `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`*
