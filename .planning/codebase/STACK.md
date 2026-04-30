# Technology Stack

**Analysis Date:** 2026-04-30

## Languages

**Primary:**
- Kotlin 2.0.0 - Android application source in `app/src/main/java/com/recipegrabber/**`, configured by `gradle/libs.versions.toml` and `app/build.gradle.kts`.

**Secondary:**
- Java 17 bytecode target - Android compile options and Kotlin JVM target in `app/build.gradle.kts`; project property `java.version=17` in `gradle.properties`.
- XML - Android manifest and resources in `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/*.xml`, `app/src/main/res/xml/*.xml`, and launcher resources in `app/src/main/res/drawable/*.xml`.
- Kotlin Gradle DSL - Build configuration in `settings.gradle.kts`, `build.gradle.kts`, and `app/build.gradle.kts`.
- TOML - Gradle version catalog in `gradle/libs.versions.toml`.
- JSON - Room schema output in `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json` and project-side model routing config in `openclaw-routing-config.json`.

## Runtime

**Environment:**
- Android app runtime with `minSdk = 33`, `targetSdk = 35`, and `compileSdk = 35` in `app/build.gradle.kts`.
- JVM 17 for compilation via `sourceCompatibility = JavaVersion.VERSION_17`, `targetCompatibility = JavaVersion.VERSION_17`, and `kotlinOptions.jvmTarget = "17"` in `app/build.gradle.kts`.
- Gradle wrapper 8.7 from `gradle/wrapper/gradle-wrapper.properties`.

**Package Manager:**
- Gradle wrapper (`./gradlew`) with repositories `google()`, `mavenCentral()`, and `gradlePluginPortal()` in `settings.gradle.kts`.
- Dependency versions are centralized in `gradle/libs.versions.toml`.
- Lockfile: Not detected. No Gradle dependency lock files are present in the project root or `gradle/`.

## Frameworks

**Core:**
- Android Gradle Plugin 8.5.2 - Android application build plugin in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- Jetpack Compose BOM 2024.06.00 - Declarative UI in `app/src/main/java/com/recipegrabber/presentation/ui/**`; enabled by `buildFeatures.compose = true` in `app/build.gradle.kts`.
- Material 3 - UI components and theme in `app/src/main/java/com/recipegrabber/presentation/ui/theme/Theme.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/*.kt`, and `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/*.kt`.
- Navigation Compose 2.7.7 - In-app navigation in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`.
- AndroidX Lifecycle 2.8.4 - ViewModel and StateFlow-driven UI state in `app/src/main/java/com/recipegrabber/presentation/viewmodel/*.kt` and onboarding view models in `app/src/main/java/com/recipegrabber/presentation/ui/screens/onboarding/*ViewModel.kt`.
- Hilt 2.51.1 - Dependency injection through `@HiltAndroidApp` in `app/src/main/java/com/recipegrabber/RecipeGrabberApplication.kt`, modules in `app/src/main/java/com/recipegrabber/di/*.kt`, and `@HiltViewModel` view models.
- Room 2.6.1 - Local SQLite persistence in `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`, DAOs in `app/src/main/java/com/recipegrabber/data/local/dao/*.kt`, entities in `app/src/main/java/com/recipegrabber/data/local/entity/*.kt`, migrations in `app/src/main/java/com/recipegrabber/data/local/Migrations.kt`, and schema export in `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json`.
- AndroidX DataStore Preferences 1.1.1 - User settings and API key storage in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- Retrofit 2.11.0 + Gson converter - REST clients for OpenAI, Anthropic, Moonshot/Kimi, and Apify in `app/src/main/java/com/recipegrabber/domain/llm/*.kt` and `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.
- OkHttp 4.12.0 - HTTP client with explicit timeouts in `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/KimiProvider.kt`, and `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.
- Google Play Services Auth 21.2.0 + Google API Client/Drive SDK - Google Drive sign-in and appDataFolder backup in `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`.
- Kotlin Coroutines 1.8.1 - Async work, Flow, and Play Services task bridging across repositories, use cases, services, and view models; configured in `app/build.gradle.kts`.
- Coil Compose 2.6.0 - Image loading dependency declared in `app/build.gradle.kts`.

**Testing:**
- JUnit Jupiter 5.11.2 - Unit test API and engine declared in `gradle/libs.versions.toml` and `app/build.gradle.kts`; tests in `app/src/test/java/com/recipegrabber/LlmProviderTest.kt` and `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`.
- MockK 1.13.12 - Mocking in `app/src/test/java/com/recipegrabber/RecipeRepositoryTest.kt`; Android variant declared for instrumentation tests in `app/build.gradle.kts`.
- AndroidX Test JUnit 1.2.1 - Instrumentation test dependency declared in `app/build.gradle.kts`.
- Robolectric 4.12.2 - Version declared in `gradle/libs.versions.toml`; not wired into `app/build.gradle.kts`.

**Build/Dev:**
- KSP 2.0.0-1.0.24 - Annotation processing for Hilt and Room in `app/build.gradle.kts`, including Room schema output argument `room.schemaLocation`.
- R8/ProGuard - Release minification enabled in `app/build.gradle.kts`; keep rules in `app/proguard-rules.pro`.
- Android Studio/IntelliJ project metadata - `.idea/` and `.vscode/settings.json` are present; `.idea/` is ignored by `.gitignore`.

## Key Dependencies

**Critical:**
- `androidx.room:room-runtime`, `androidx.room:room-ktx`, and `androidx.room:room-compiler` 2.6.1 - Own the local recipe database in `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`.
- `androidx.datastore:datastore-preferences` 1.1.1 - Stores onboarding status, provider selection, API keys, Drive sync flag, clipboard monitor flag, and theme settings in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- `com.google.dagger:hilt-android` and `hilt-android-compiler` 2.51.1 - Provides app-wide services, repositories, database, and view models from `app/src/main/java/com/recipegrabber/di/*.kt`.
- `com.squareup.retrofit2:retrofit` 2.11.0 and `converter-gson` 2.11.0 - Used for typed HTTP integrations in `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/KimiProvider.kt`, and `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`.
- `com.squareup.okhttp3:okhttp` 4.12.0 - Used directly in `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt` and as the Retrofit client in other providers.
- `com.google.android.gms:play-services-auth:21.2.0` and Google Drive API dependencies - Required by `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`.
- `androidx.compose.material3:material3` and Compose UI libraries - Main UI surface in `app/src/main/java/com/recipegrabber/presentation/ui/screens/*.kt`.

**Infrastructure:**
- `androidx.navigation:navigation-compose` 2.7.7 - Route graph in `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`.
- `androidx.hilt:hilt-navigation-compose` 1.2.0 - Compose `hiltViewModel()` usage in screens such as `app/src/main/java/com/recipegrabber/presentation/ui/screens/SettingsScreen.kt`.
- `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` 1.8.1 - Awaits Google sign-out tasks in `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`.
- `androidx.localbroadcastmanager:localbroadcastmanager` 1.1.0 - Declared in `app/build.gradle.kts`; clipboard flow uses Android service/broadcast patterns in `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt` and `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`.
- `io.coil-kt:coil-compose` 2.6.0 - Declared in `app/build.gradle.kts` for image loading support.

## Configuration

**Environment:**
- Runtime API keys are configured by the user in onboarding/settings and persisted via DataStore keys in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`: `openai_api_key`, `gemini_api_key`, `claude_api_key`, `kimi_api_key`, and `apify_api_key`.
- Google Drive state is persisted via `drive_sync_enabled` and `google_account_email` in `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`; OAuth account credentials are handled by Google Play Services in `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`.
- `.env` files: Not detected.
- `local.properties` is present and ignored by `.gitignore`; do not depend on committed values from `local.properties`.
- `google-services.json` is ignored by `.gitignore` but not detected in the project root or `app/`; Google Drive auth is implemented through Play Services sign-in, not Firebase config.

**Build:**
- Root build plugins: `build.gradle.kts`.
- App build configuration: `app/build.gradle.kts`.
- Version catalog: `gradle/libs.versions.toml`.
- Gradle properties: `gradle.properties`.
- Gradle wrapper: `gradle/wrapper/gradle-wrapper.properties`.
- Project settings and repositories: `settings.gradle.kts`.
- ProGuard/R8 rules: `app/proguard-rules.pro`.
- Android manifest permissions and components: `app/src/main/AndroidManifest.xml`.
- Room schema export: `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json`.

## Platform Requirements

**Development:**
- Use JDK 17, matching `java.version=17` in `gradle.properties` and JVM target settings in `app/build.gradle.kts`.
- Use Android SDK API 35 for compile/target and API 33+ for device/emulator runtime.
- Use Gradle via `./gradlew`; wrapper version is 8.7 from `gradle/wrapper/gradle-wrapper.properties`.
- Use Android Studio with Kotlin 2.0.0 and AGP 8.5.2 compatibility; README lists Android Studio Hedgehog or later in `README.md`.

**Production:**
- Deployment target is an Android APK/AAB for application ID `com.recipegrabber`, version `2.0.0`, version code `1` from `app/build.gradle.kts`.
- Release builds enable minification and use `app/proguard-rules.pro`.
- No web hosting, server runtime, container, or CI/CD deployment target is detected in `.github/`, root config, or build files.

---

*Stack analysis: 2026-04-30*
