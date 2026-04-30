# Testing Patterns

**Analysis Date:** 2026-04-30

## Test Framework

**Runner:**
- JUnit Jupiter 5.11.2 is declared through `libs.junit`, `libs.junit.params`, and `libs.junit.engine` in `gradle/libs.versions.toml`.
- Unit test dependencies are declared in `app/build.gradle.kts` with `testImplementation(libs.junit)`, `testImplementation(libs.junit.params)`, `testImplementation(libs.mockk)`, and `testRuntimeOnly(libs.junit.engine)`.
- Android instrumentation dependencies are declared in `app/build.gradle.kts` with `androidTestImplementation(libs.androidx.junit)` and `androidTestImplementation(libs.mockk.android)`.
- JUnit Platform activation is not configured: no `testOptions { unitTests.all { useJUnitPlatform() } }` block is present in `app/build.gradle.kts`.
- `kotlinx.coroutines.test.runTest` is imported in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`, but `kotlinx-coroutines-test` is not declared in `gradle/libs.versions.toml` or `app/build.gradle.kts`.
- The current shell uses OpenJDK 25.0.1. `./gradlew test --dry-run --stacktrace` fails during Gradle Kotlin DSL initialization with `IllegalArgumentException: 25.0.1`. The project declares Java 17 source/target compatibility in `app/build.gradle.kts` and `java.version=17` in `gradle.properties`, but no local JDK 17 installation is detected by `/usr/libexec/java_home -V`.

**Assertion Library:**
- JUnit Jupiter assertions are used: `Assertions.assertEquals` and `Assertions.assertTrue` in `app/src/test/java/com/recipegrabber/LlmProviderTest.kt` and `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.

**Run Commands:**
```bash
./gradlew test                         # Run all local JVM unit tests when a compatible JDK is active
./gradlew testDebugUnitTest            # Run debug variant JVM unit tests
./gradlew connectedAndroidTest         # Run instrumentation tests on a device or emulator
./gradlew lint                         # Run Android lint checks
```

## Test File Organization

**Location:**
- JVM unit tests are placed under `app/src/test/java/com/recipegrabber/`.
- Android instrumentation source set exists only by Gradle convention; no files are present under `app/src/androidTest/`.
- Tests are package-level under `com.recipegrabber` rather than mirroring production packages exactly.

**Naming:**
- Use `*Test.kt` filenames: `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`, `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.
- Test classes use production subject names plus `Test`: `LlmProviderTest`, `RecipeRepositoryTest`.
- Individual tests use backtick names with behavior phrasing: `should create valid Recipe entity`, `should insert recipe successfully`.
- Add `@DisplayName` to classes, nested groups, and tests for readable output.

**Structure:**
```
app/src/test/java/com/recipegrabber/
├── LlmProviderTest.kt          # Entity construction and ProviderType enum checks
└── RecipeRepositoryTest.kt     # Repository delegation tests with mocked DAOs
```

## Test Structure

**Suite Organization:**
```kotlin
@DisplayName("Repository Tests")
class RecipeRepositoryTest {
    private lateinit var recipeDao: RecipeDao
    private lateinit var ingredientDao: IngredientDao
    private lateinit var stepDao: StepDao
    private lateinit var repository: RecipeRepository

    @BeforeEach
    fun setup() {
        recipeDao = mockk(relaxed = true)
        ingredientDao = mockk(relaxed = true)
        stepDao = mockk(relaxed = true)
        repository = RecipeRepository(recipeDao, ingredientDao, stepDao)
    }

    @Nested
    @DisplayName("Recipe Operations")
    inner class RecipeOperations {
        @Test
        @DisplayName("Should insert recipe successfully")
        fun `should insert recipe successfully`() = runTest {
            coEvery { recipeDao.insertRecipe(any()) } returns 1L

            val result = repository.insertRecipe(testRecipe)

            assertEquals(1L, result)
            coVerify { recipeDao.insertRecipe(testRecipe) }
        }
    }
}
```

**Patterns:**
- Use `@Nested` inner classes to group related behavior: `Recipe Operations`, `Ingredient Operations`, and `Step Operations` in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`; `Recipe Parsing Tests` and `ProviderType Tests` in `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`.
- Use `@BeforeEach` for repeated mock setup and subject construction in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.
- Use Arrange/Act/Assert spacing inside tests: stub with `coEvery`, call the repository, then assert and verify.
- Use `runTest` for suspend repository calls and Flow collection in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.
- Use `.first()` to collect a single Flow emission when DAOs return `flowOf(...)`.

## Mocking

**Framework:** MockK 1.13.12

**Patterns:**
```kotlin
private lateinit var recipeDao: RecipeDao

@BeforeEach
fun setup() {
    recipeDao = mockk(relaxed = true)
}

@Test
fun `should get all recipes as flow`() = runTest {
    coEvery { recipeDao.getAllRecipes() } returns flowOf(listOf(testRecipe))

    val recipes = repository.getAllRecipes().first()

    assertEquals(1, recipes.size)
}
```

**What to Mock:**
- Mock DAOs when testing repository delegation and data transformation: `RecipeDao`, `IngredientDao`, and `StepDao` in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.
- Mock external service/provider boundaries in new unit tests for use cases and ViewModels: `ApifyService` from `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`, `LlmProviderFactory` from `app/src/main/java/com/recipegrabber/domain/llm/LlmProviderFactory.kt`, `PreferencesRepository` from `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- Mock Android framework objects in local JVM tests unless using Robolectric. Robolectric is listed in `gradle/libs.versions.toml` as `robolectric = "4.12.2"` but is not declared as a dependency in `app/build.gradle.kts`.

**What NOT to Mock:**
- Do not mock simple data classes and Room entities. Construct `Recipe`, `Ingredient`, and `Step` directly as in `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`.
- Do not mock repository methods when testing repository implementation. Mock the DAO dependencies and verify DAO calls, as in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.
- Do not hit real LLM, Google Drive, or Apify APIs in unit tests. Provider classes in `app/src/main/java/com/recipegrabber/domain/llm/` and services in `app/src/main/java/com/recipegrabber/data/remote/` require API keys and network.

## Fixtures and Factories

**Test Data:**
```kotlin
private val testRecipe = Recipe(
    id = 1,
    title = "Test Recipe",
    description = "A test recipe description",
    servings = 4,
    prepTimeMinutes = 10,
    cookTimeMinutes = 20,
    sourceUrl = "https://example.com",
    sourceType = "VIDEO",
    thumbnailUrl = null,
    createdAt = System.currentTimeMillis()
)
```

**Location:**
- Fixtures are inline inside test classes. `testRecipe` lives in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.
- Entity construction examples are repeated inline in `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`.
- Shared fixture/factory files are not detected under `app/src/test/java/com/recipegrabber/`.

**Factory Guidance:**
- For additional repository tests, prefer private helper functions in the same test class until duplication spans multiple files.
- When shared fixtures become necessary, place them under `app/src/test/java/com/recipegrabber/` or mirror production package paths under `app/src/test/java/com/recipegrabber/data/`.

## Coverage

**Requirements:** None enforced.

**View Coverage:**
```bash
./gradlew testDebugUnitTest             # Produces standard Gradle test reports when tests run
```

**Coverage Tools:**
- Jacoco, Kover, and Android coverage configuration are not detected in `build.gradle.kts`, `app/build.gradle.kts`, or `gradle/libs.versions.toml`.
- No coverage threshold or report task is configured.

## Test Types

**Unit Tests:**
- Existing tests are local JVM tests under `app/src/test/java/com/recipegrabber/`.
- `RecipeRepositoryTest` verifies repository delegation, Flow returns, and suspend DAO calls using MockK in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.
- `LlmProviderTest` verifies entity construction and `ProviderType` enum values in `app/src/test/java/com/recipegrabber/LlmProviderTest.kt`.

**Integration Tests:**
- README describes Room integration testing with `Room.inMemoryDatabaseBuilder`, but no such integration test exists under `app/src/test/java/com/recipegrabber/`.
- Room test dependencies are declared with `testImplementation(libs.androidx.room.runtime)`, `testImplementation(libs.androidx.room.ktx)`, and `kspTest(libs.androidx.room.compiler)` in `app/build.gradle.kts`.

**E2E Tests:**
- Not used. No Espresso, Compose UI test, or Playwright-style E2E configuration is detected.

**Instrumentation Tests:**
- The Android test runner is configured as `androidx.test.runner.AndroidJUnitRunner` in `app/build.gradle.kts`.
- No `app/src/androidTest/` files are present.
- AndroidX JUnit and MockK Android dependencies are declared in `app/build.gradle.kts`, but Compose UI testing dependencies are not declared.

## Common Patterns

**Async Testing:**
```kotlin
@Test
@DisplayName("Should search recipes by query")
fun `should search recipes by query`() = runTest {
    coEvery { recipeDao.searchRecipes("Test") } returns flowOf(listOf(testRecipe))

    val recipes = repository.searchRecipes("Test").first()

    assertEquals(1, recipes.size)
    assertTrue(recipes[0].title.contains("Test"))
}
```

**Error Testing:**
```kotlin
@Test
@DisplayName("Should have correct enum values")
fun `should have correct enum values`() {
    assertEquals(ProviderType.OPENAI, ProviderType.valueOf("OPENAI"))
}
```

**Repository Delegation Testing:**
```kotlin
@Test
@DisplayName("Should delete recipe by id")
fun `should delete recipe by id`() = runTest {
    coEvery { recipeDao.deleteRecipeById(any()) } returns Unit

    repository.deleteRecipeById(1L)

    coVerify { recipeDao.deleteRecipeById(1L) }
}
```

**Where To Add New Tests:**
- Add typed error-path tests for `ExtractRecipeUseCase.ExtractionResult` in `app/src/main/java/com/recipegrabber/domain/usecase/ExtractRecipeUseCase.kt`.
- Add ViewModel state transition tests for `RecipeExtractionViewModel` and `SettingsViewModel` under `app/src/test/java/com/recipegrabber/presentation/viewmodel/` when coroutine test dependencies are declared.
- Add Room in-memory integration tests for migrations and DAO behavior under `app/src/test/java/com/recipegrabber/data/local/`.

---

*Testing analysis: 2026-04-30*
