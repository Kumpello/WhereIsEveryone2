# WhereIsEveryone

WhereIsEveryone is a real-time location sharing Android application that allows users to see their friends' locations on a map, manage their friend list, and share their own profile using modern technologies like NFC and QR codes.

## 🚀 Features

- **Real-time Map**: Interactive map using Mapbox SDK to visualize friends' locations.
- **Location Tracking**: Foreground service for reliable background location updates.
- **Friend Management**:
    - **NFC Sharing**: Share your profile by tapping devices.
    - **QR/URI Support**: Add friends via deep links or scanning.
- **Authentication**: Secure login and sign-up flows with encrypted data storage.
- **Local Persistence**: Offline support and data caching using Room database.
- **Modern UI**: Fully built with Jetpack Compose following Material 3 guidelines.

## 🛠 Tech Stack

### UI & UX
- **Jetpack Compose**: Modern toolkit for building native UI.
- **Material 3**: Latest Android design system.
- **Compose Navigation**: Type-safe navigation between screens.
- **Splashscreen API**: For a smooth app startup experience.

### Architecture & DI
- **MVI / Clean Architecture**: Organized code for maintainability and testability.
- **Koin**: Lightweight dependency injection framework.
- **Kotlin Coroutines & Flow**: Asynchronous programming and reactive data streams.

### Networking & Data
- **Retrofit 2**: Type-safe HTTP client.
- **Moshi**: Modern JSON library for Android and Java.
- **Room Database**: SQLite object mapping library.
- **DataStore**: Modern data storage solution for preferences.

### Tools & Services
- **Mapbox Maps SDK**: High-performance map rendering.
- **Google Play Services**: Location and Code Scanner.
- **Timber**: Tree-based logging.
- **ZXing**: QR code generation and processing.
- **Tink**: Multi-platform, cross-language library for cryptographic tasks.

### Testing
- **JUnit 4**: Unit testing.
- **MockK**: Mocking library for Kotlin.
- **Turbine**: A small testing library for kotlinx.coroutines Flow.
- **Espresso / Compose UI Test**: Instrumented UI testing.

## 📂 Project Structure

The project follows a clean, feature-based MVI architecture. Below is the detailed structure of the `:app` module:

```text
com.kumpello.whereiseveryone/
├── authentication/             # Authentication & Onboarding
│   ├── login/                  # Login feature (UI, Domain, Presentation)
│   ├── signUp/                 # Sign Up feature (UI, Domain, Presentation)
│   ├── splash/                 # Splash screen
│   └── common/                 # Auth-specific shared components
├── main/                       # Core Application
│   ├── map/                    # Mapbox & Location Tracking
│   │   ├── ui/                 # Map Compose screens
│   │   ├── presentation/       # Map ViewModels & MVI logic
│   │   ├── domain/             # Location-related use cases
│   │   └── entity/             # Map-specific data models
│   ├── friends/                # Friend Management & NFC
│   │   ├── ui/                 # Friends list & profile UI
│   │   ├── presentation/       # Friends ViewModels
│   │   ├── nfc/                # NFC HCE (Host Card Emulation) service
│   │   └── domain/             # Friend-related business logic
│   └── settings/               # App configuration
│       ├── ui/                 # Settings screen
│       └── presentation/       # Settings ViewModels
└── common/                     # Shared Infrastructure
    ├── data/                   # Data providers & remote sources
    ├── domain/                 # Core repositories, managers, & use cases
    ├── database/               # Room Persistence (AppDatabase)
    ├── di/                     # Dependency Injection modules (Koin)
    ├── ui/                     # Shared UI components, theme, & entities
    ├── navigation/             # Type-safe navigation definitions
    └── extension/              # Kotlin extension functions
```

## Coroutine dispatchers

- ViewModel reducers, UI actions, and `SideEffect.AsyncWork` start on Main via
  `viewModelScope`. `trigger` accepts events from any thread and dispatches reduction
  and state assignment to Main. Async work should call main-safe suspend APIs.
- Map and friends view-state calculations run on an injectable `Default` dispatcher.
  The initial view state uses the cheap, empty constructor state; the latest populated
  state is mapped asynchronously when collected. Keep `toViewState` pure: it computes
  an immutable snapshot on Default, then returns to Main for publication. API friend-list
  conversion also runs on Default before sending a result event to the reducer.
- Initialization launches independent operations on Main; suspending I/O releases Main
  while waiting. Username loading must not gate location observation. Reducers only
  return state and side-effect descriptions; they must not launch requests themselves.
- Encrypted preferences initialize the keyset and encrypt/decrypt on an injectable
  `IO` dispatcher. Device-ID settings fallback and synchronous database clearing also
  use `IO`. Proximity distance calculations use `Default`.
- Retrofit suspend calls, Room suspend DAOs, and DataStore manage their own I/O.
  Thin use cases and lightweight validation/mapping functions inherit the caller's
  context; they don't need an extra I/O switch.
- `PreferencesManager.getCached` may block on a cache miss and is reserved for
  OkHttp's synchronous worker-thread interceptor. Coroutine callers use `get` or `observe`.
- Rethrow `CancellationException`; cancellation must not become a failure toast,
  authentication fallback, or retry. Tests can supply dispatchers through constructors.

## Logging

Logging follows the build type (`BuildConfig.DEBUG`), independently of the server flavor.
All debug variants use Timber's `DebugTree`; all release variants use `ProductionTree`.
Release logs go to Android Logcat. They are not uploaded or persisted to an app log file.

| Level | Use | Release output |
|-------|-----|----------------|
| Verbose / Debug | UI and service lifecycle, polling, successful operations, intermediate retries, NFC protocol negotiation | No |
| Info | Optional routine milestones | No |
| Warn | Recoverable failures, HTTP 4xx, exhausted network retries, device capability fallbacks | Yes |
| Error / Assert | HTTP 5xx, unexpected failures, permission failures preventing location updates, encrypted storage failures | Yes |

Log HTTP failures once in the repository via `Timber.tag(TAG).httpFailure("Operation", statusCode)`.
Callers may log debug context or exceptions that bypass the repository's response handling.
Use stable tags and operation names. Never log credentials, tokens, usernames, coordinates,
user messages, full URLs/deep links, HTTP bodies, or NFC payloads, even at debug level.
Pass exceptions as the throwable argument with a safe message, e.g.
`Timber.tag(TAG).w(error, "Unable to update status")`; never interpolate `error.message`.
Debug builds retain full exception details for diagnosis. Release logs retain only the
safe context and exception type; throwable messages, causes, and stack traces are removed.
Coroutine cancellation is omitted from release logs.

Koin's direct Android logger is disabled in release builds so it cannot bypass this
policy with arbitrary dependency messages. Third-party SDK and Android system logs
are outside the Timber policy.

To inspect production diagnostics, select the installed app process in Android Studio
Logcat and filter with `level:WARN`, or use `adb logcat --pid=<app-pid> '*:W'`.
Production logging does not require a server or dependency change.

## 📄 Licenses

### Project License
Copyright (c) 2026 Michał Kukulski. Licensed under the **Apache License, Version 2.0**. See the [LICENSE](LICENSE) file for details.

### Dependency Licenses
This project uses several open-source libraries. Below is a list of their respective licenses:

| Dependency | License |
|------------|---------|
| AndroidX Libraries | Apache License 2.0 |
| Jetpack Compose | Apache License 2.0 |
| Kotlin Coroutines / Serialization | Apache License 2.0 |
| Koin | Apache License 2.0 |
| Retrofit / OkHttp / Moshi | Apache License 2.0 |
| Room Persistence | Apache License 2.0 |
| Mapbox Maps SDK | Mapbox Terms of Service |
| Timber | Apache License 2.0 |
| ZXing (Core) | Apache License 2.0 |
| Tink (Cryptography) | Apache License 2.0 |
| MockK | Apache License 2.0 |
| Turbine | Apache License 2.0 |
| JUnit 4 | Eclipse Public License 1.0 |
