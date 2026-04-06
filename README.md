# Recipe Grabber V2

A modern Android application for extracting and managing recipes from video content using AI-powered LLM providers.

## Features

- **AI-Powered Recipe Extraction**: Automatically extract recipes from YouTube, TikTok, Instagram, and other video platforms
- **Multiple LLM Providers**: Support for OpenAI GPT-4 and Google Gemini
- **Google Drive Sync**: Backup and sync your recipes across devices
- **Clipboard Monitoring**: Automatically detect video URLs in your clipboard
- **Portion Scaling**: Easily scale recipes for different serving sizes
- **Dark Mode First**: Beautiful dark theme as default with light mode option
- **Offline Support**: Full offline capability with local Room database

## Technical Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0 |
| Min SDK | 33 (Android 13) |
| Target SDK | 35 |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt 2.51.1 |
| Database | Room 2.6.1 |
| Networking | Retrofit 2.11 + OkHttp 4.12 |
| AI | OpenAI GPT-4 / Google Gemini |
| Auth | Google Identity Services 1.1 |
| Async | Kotlin Coroutines + Flow |
| Testing | JUnit 5 + MockK |

## Project Structure

```
app/src/main/java/com/recipegrabber/
├── di/                     # Hilt dependency injection modules
├── domain/llm/             # LLM provider interfaces and implementations
│   ├── LlmProvider.kt       # Core interface
│   ├── OpenAiProvider.kt   # OpenAI implementation
│   ├── GeminiProvider.kt   # Gemini implementation
│   └── LlmProviderFactory.kt
├── data/
│   ├── local/              # Room database layer
│   │   ├── entity/         # Database entities
│   │   ├── dao/            # Data access objects
│   │   └── RecipeDatabase.kt
│   ├── repository/         # Repository implementations
│   └── remote/            # Google Drive service
├── presentation/
│   ├── MainActivity.kt
│   ├── ui/
│   │   ├── screens/       # Compose UI screens
│   │   ├── components/    # Reusable UI components
│   │   └── theme/         # Material 3 theming
│   └── viewmodel/         # ViewModels with StateFlow
└── service/               # Foreground services
    └── ClipboardMonitorService.kt
```

## Setup

### Prerequisites

- Android Studio Hedgehog or later
- JDK 17
- Android SDK with API 33+

### Configuration

1. **OpenAI API Key** (optional for OpenAI provider):
   - Get your key from [OpenAI Platform](https://platform.openai.com/api-keys)
   - Enter in Settings > OpenAI API Key

2. **Gemini API Key** (optional for Gemini provider):
   - Get your key from [Google AI Studio](https://aistudio.google.com/app/apikey)
   - Enter in Settings > Gemini API Key

3. **Google Drive Sync** (optional):
   - Enable in Settings to backup recipes to your Google Drive

### Build

```bash
./gradlew assembleDebug    # Debug build
./gradlew assembleRelease # Release build
```

### Test

```bash
./gradlew test             # Run unit tests
./gradlew connectedAndroidTest  # Run instrumentation tests
```

## Usage

### Extract a Recipe

1. Tap the **+** FAB button on the main screen
2. Paste a video URL (YouTube, TikTok, etc.)
3. Tap **Extract** and wait for AI processing
4. View your extracted recipe with ingredients and steps

### Scale Portions

1. Open any recipe
2. Use the **+/-** buttons to adjust servings
3. All ingredient amounts automatically scale

### Auto-Extract from Clipboard

1. Enable **Clipboard Monitor** in Settings
2. Copy any recipe video URL
3. The app will automatically detect and offer to extract

### Sync to Google Drive

1. Enable **Drive Sync** in Settings
2. Sign in with your Google account
3. Recipes automatically backup to your private Drive folder

## Architecture

The app follows **Clean Architecture** with three layers:

- **Domain Layer**: Business logic and use cases (LLM providers)
- **Data Layer**: Repositories, Room database, DataStore preferences
- **Presentation Layer**: Compose UI with MVVM pattern

### State Management

- `StateFlow` for UI state
- `Flow` for reactive data streams from Room
- `DataStore` for user preferences

### Testing Strategy

- **Unit Tests**: JUnit 5 with MockK for repositories and providers
- **UI Tests**: Compose testing with `createComposeRule`
- **Integration Tests**: Room database with `Room.inMemoryDatabaseBuilder`

## License

MIT License - See LICENSE file for details

## Contributing

Contributions welcome! Please read CONTRIBUTING.md for guidelines.
