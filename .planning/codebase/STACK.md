---
last_mapped_commit: 3069fd99abb8629a520ee646984315e8f32648f4
analysis_date: 2026-10-13
---

# Technology Stack

**Analysis Date:** 2026-10-13
**Last mapped commit:** `3069fd99abb8629a520ee646984315e8f32648f4`

## Languages

**Primary:**
- Kotlin 2.0.0 - Android application, domain logic, data access, Compose UI, unit and instrumentation tests (`app/src/main` and `app/src/test`)
- XML - Android manifest and resources (`app/src/main/AndroidManifest.xml`, `app/src/main/res`)

**Secondary:**
- Kotlin DSL - Gradle configuration (`build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`)
- Java - Generated Hilt/KSP/Room output only (`app/build/generated`); do not add hand-written code there

## Runtime

**Environment:**
- Android API 31 minimum, API 35 target and compile SDK (`app/build.gradle.kts`)
- Java 17 source/target and Kotlin JVM target (`app/build.gradle.kts`)

**Package Manager:**
- Gradle wrapper (`gradlew`, `gradle/wrapper`)
- Lockfile: Gradle dependency locking is not detected; the version catalog is `gradle/libs.versions.toml`

## Frameworks

**Core:**
- Android Gradle Plugin 8.5.2 and Kotlin Android 2.0.0 - Android build (`gradle/libs.versions.toml`)
- Jetpack Compose BOM 2024.06.00, Material 3, Navigation Compose 2.7.7 - UI/navigation (`app/build.gradle.kts`)
- Hilt 2.51.1 - dependency injection (`app/src/main/java/com/recipegrabber/di`)
- Room 2.6.1 - local recipe database and migrations (`app/src/main/java/com/recipegrabber/data/local`)
- AndroidX DataStore Preferences 1.1.1 - persisted settings (`PreferencesRepository.kt`)

**Testing:**
- JUnit Jupiter 5.11.2, MockK 1.13.12, and coroutines-test 1.8.1 - JVM tests (`app/src/test`)
- AndroidX JUnit 1.2.1, Test Runner 1.6.2, and Compose UI test - device tests (`app/src/androidTest`)

**Build/Dev:**
- KSP 2.0.0-1.0.24 - Room/Hilt code generation
- GitHub Actions Android workflow - tests, lint, builds, and API 31/35 instrumentation (`.github/workflows/android.yml`)

## Key Dependencies

**Critical:**
- `androidx.room:*:2.6.1` - recipe, ingredient, and step persistence
- `com.google.dagger:hilt-android:2.51.1` - application services and ViewModels
- `com.squareup.okhttp3:okhttp:4.12.0` - Gemini transport and cancellation
- `GeminiFileUploader.kt` uses existing OkHttp `Call.Factory` boundaries for download/upload/poll/delete, bounded cache streaming and public-only media DNS; no new transfer library was added.
- `com.squareup.retrofit2:retrofit:2.11.0` - Apify and Kimi clients
- Google Identity/Play Services Auth - OAuth flows
- Google API client Android 2.4.1 and Drive API v3 revision 20240521 - Drive app-data operations

**Infrastructure:**
- Gson converter/factory - JSON for Retrofit, Gemini, and Drive
- Coil Compose 2.6.0 - image loading
- kotlinx.coroutines 1.8.1, including Play Services integration - async work

## Configuration

**Environment:**
- Runtime settings are stored by `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`.
- API keys are entered through Settings/onboarding and remain outside source control.
- Google OAuth depends on package `com.recipegrabber` and the installed signing-certificate registration documented in `.planning/audits/2026-10-13-google-oauth-registration.md`.

**Build:**
- `settings.gradle.kts` defines the project/repositories.
- `gradle/libs.versions.toml` is the dependency/version catalog.
- `app/build.gradle.kts` defines SDK levels, Java 17, Compose, KSP, release minification, and tests.
- `app/proguard-rules.pro` is used for minified release builds.

## Platform Requirements

**Development:**
- Android SDK/API 35 and Java 17 are required by the module configuration.
- An authorized Android device/emulator is required for `connectedDebugAndroidTest`; external OAuth/video checks are opt-in.

**Production:**
- The deliverable is an Android APK; release builds are minified but unsigned according to `CONTRIBUTING.md`.
- Network access is required for extraction and integrations; local recipe browsing and Room persistence work offline.

## Current Capability Boundary

- The source implements Gemini API-key and Google OAuth modes, three additional LLM adapters, Google Drive helpers, and Apify scrapers.
- The dated device audit live-verified Google OAuth plus one public YouTube extraction with Gemini 3.1 Flash-Lite, including Room persistence and restart readback.
- Gemini 3.8 Flash had observed HTTP 503 and timeout failures in the same audit; this is an external observation, not a proven stack defect or OAuth-scope diagnosis.

---

*Stack analysis: 2026-10-13; last_mapped_commit: `3069fd99abb8629a520ee646984315e8f32648f4`*
