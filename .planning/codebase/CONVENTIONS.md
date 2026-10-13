---
last_mapped_commit: 3dfcf75a166b1af026a710d6e54c8de6b1c9c126
analysis_date: 2026-10-13
---

# Coding Conventions

**Analysis Date:** 2026-10-13
**Last mapped commit:** `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`

## Naming Patterns

**Files:**
- Use PascalCase Kotlin filenames for production types, Compose screens, ViewModels, repositories, DAOs, parsers, and tests: `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeDetailScreen.kt`, `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/domain/llm/VideoLinkParser.kt`, and `app/src/test/java/com/recipegrabber/VideoLinkParserTest.kt`.
- Use responsibility suffixes such as `Screen`, `ViewModel`, `Repository`, `Dao`, `Service`, `Provider`, `Factory`, `Parser`, and `Test`.

**Functions:**
- Use lower camelCase: `getAllRecipes`, `insertRecipeWithDetails`, `extractRecipeFromVideo`, and `startExtraction`.
- Use `suspend` for Room writes, repository mutations, provider calls, and service calls crossing IO boundaries. Use `operator fun invoke` for executable use cases in `app/src/main/java/com/recipegrabber/domain/usecase/`.
- Use behavior-oriented backtick names in tests, for example `should insert recipe successfully` in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.

**Variables and types:**
- Use lower camelCase for values and state. Private mutable state uses an underscore and exposes a read-only `StateFlow`, as in `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`.
- Use PascalCase for classes, interfaces, data classes, enums, sealed results, immutable UI state types, and parser results.
- Use uppercase names for grouped preference keys and intent action constants in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt` and `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`.

## Code Style

**Formatting:**
- Kotlin official style is enabled by `kotlin.code.style=official` in `gradle.properties`; use four-space indentation.
- Format multi-line constructors and calls one argument per line. Put chained builders and Compose modifier calls on separate lines.
- Prefer named `modifier = Modifier...` arguments and trailing lambdas for Compose and Flow collection.

**Imports:**
1. Android framework imports.
2. AndroidX and Jetpack imports.
3. Application imports under `com.recipegrabber.*`.
4. Third-party imports.
5. Java and `javax` imports, following the local ordering.

No Kotlin path aliases or barrel files are configured. Import concrete application files directly.

## Error Handling

- Return `Result<T>` from fallible provider and remote-service boundaries, convert exceptions at the boundary, and log through `AppLogger`: `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`, and `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`.
- Use sealed outcomes for UI-relevant orchestration branches in `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`.
- ViewModels catch mutation failures and expose user-facing error state; Compose screens render it through `SnackbarHostState`, as in `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeListScreen.kt`.
- Use explicit local fallbacks only for non-critical malformed persisted or remote data, and keep them visible in the owning repository/provider.

## Logging

- Inject `AppLogger` into IO and workflow components. Use stable tags and severity levels consistently in `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`.
- Never log credential values. Provider logs may report missing authentication without printing secrets.

## Function and Module Design

- Keep simple DAO/repository delegations as small expressions; use explicit typed parameters for small operations.
- Return `Flow<T>` for reactive reads, suspend values for one-shot operations, `Result<T>` for fallible boundaries, and sealed results for UI branching.
- Keep Room entities and DAOs under `app/src/main/java/com/recipegrabber/data/local/`, repositories under `app/src/main/java/com/recipegrabber/data/repository/`, remote clients under `app/src/main/java/com/recipegrabber/data/remote/`, use cases under `app/src/main/java/com/recipegrabber/domain/usecase/`, ViewModels under `app/src/main/java/com/recipegrabber/presentation/viewmodel/`, and Compose UI under `app/src/main/java/com/recipegrabber/presentation/ui/`.
- Prefer Hilt constructor injection for concrete classes; reserve modules such as `app/src/main/java/com/recipegrabber/di/DatabaseModule.kt` for framework-created dependencies.

## Comments and Documentation

- Use short comments for lifecycle edge cases and grouped preferences. Do not restate function names.
- Kotlin KDoc is uncommon; add it only for non-obvious public cross-layer APIs.

---

*Convention analysis: 2026-10-13*
