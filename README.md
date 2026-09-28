# DownTik

DownTik is a lightweight Android application for downloading TikTok videos and extracting audio from supported TikTok URLs.

The project is designed around a simple download workflow, local media storage, playback support, download history, and a built-in GitHub release update system.

## ✨ Features

- 📥 TikTok video downloader
- 🎵 Audio extractor
- 🔗 TikTok URL input
- 📤 Android share-to-download support
- 🎬 Video preview and playback
- 🎧 Built-in audio playback
- 📜 Download history
- 📁 Organized local media storage
- 🔄 Built-in application updater
- 📡 Stable and Beta release channels
- 🔐 SHA-256 APK verification
- 🌙 Light, Dark, and System themes
- 🎨 Android dynamic color support
- ⚡ Material 3 user interface

---

## 🛠️ Tech Stack

### Language

- **Kotlin 2.2.10**

Kotlin is used as the primary programming language for the entire Android application.

### Android

- **Android SDK**
- **compileSdk 36**
- **targetSdk 36**
- **minSdk 24**
- **AndroidX**
- **Jetpack Compose**

The application targets modern Android versions while maintaining compatibility with devices running Android 7.0 and newer.

---

## 🎨 UI & Design

DownTik uses **Jetpack Compose** with **Material 3** as its primary UI framework.

The interface is designed to be clean and focused around downloading media instead of filling the application with unnecessary controls.

### Design Principles

- Simple navigation
- Clear visual hierarchy
- Minimal interface
- Material 3 components
- Consistent spacing
- Clear download progress
- Dedicated video and audio sections
- Responsive light and dark themes

The main application uses a bottom navigation bar for switching between the two primary tools.

### Main Workflow

    TikTok URL
         ↓
      Process
         ↓
    Detect media
         ↓
      Download
         ↓
    Save locally

The application also contains dedicated screens for:

- Home
- Video downloader
- Audio extractor
- Download history
- Settings
- Video preview
- Application updates

---

## 🎨 Color System

DownTik uses a neutral Material 3 color system with a vivid indigo/blue primary accent.

### Light Theme

- Primary: `#1D58D8`
- Background: `#F8F9FA`
- Surface: `#FFFFFF`

### Dark Theme

- Primary: `#B5C4FF`
- Background: `#111418`
- Surface: `#1A1D21`

A dedicated green success color is also defined for successful operations.

The application supports:

- **System theme**
- **Light theme**
- **Dark theme**
- **Dynamic color on Android 12+**

Dynamic color allows Android to provide the application's color palette based on the device's system theme.

---

# 📚 Libraries

## Jetpack Compose

DownTik uses Jetpack Compose for its entire UI layer.

Used components include:

- `androidx.compose.ui`
- `androidx.compose.ui.graphics`
- `androidx.compose.material3`
- `androidx.compose.material:material-icons-core`
- `androidx.compose.material:material-icons-extended`

Compose provides the declarative UI architecture used throughout the application.

---

## Material 3

Material 3 provides:

- Top app bars
- Navigation bars
- Navigation items
- Buttons
- Cards
- Dialogs
- Text fields
- Progress indicators
- Theme system
- Typography

The application uses Material 3 rather than a custom UI framework.

---

## Navigation Compose

`androidx.navigation:navigation-compose`

Navigation Compose is used for managing Compose-based navigation and screen transitions.

---

## Lifecycle & ViewModel

DownTik uses AndroidX Lifecycle components:

- `androidx.lifecycle:lifecycle-runtime-ktx`
- `androidx.lifecycle:lifecycle-runtime-compose`
- `androidx.lifecycle:lifecycle-viewmodel-compose`

These components are used for:

- ViewModel state
- Lifecycle-aware UI state
- Compose state collection
- Application lifecycle handling

---

## Kotlin Coroutines

DownTik uses Kotlin Coroutines for asynchronous operations.

Libraries:

- `kotlinx-coroutines-core`
- `kotlinx-coroutines-android`

Coroutines are used for tasks such as:

- Network requests
- Video downloads
- Audio extraction
- Database operations
- Background processing

Download and extraction progress is exposed through Kotlin `Flow`.

---

# 🌐 Networking

DownTik uses the following networking stack:

    Retrofit
        ↓
      OkHttp
        ↓
     TikWM API

## Retrofit

- `com.squareup.retrofit2:retrofit`
- `com.squareup.retrofit2:converter-moshi`

Retrofit is used to communicate with the TikWM API.

The application uses the TikWM endpoint to retrieve information and downloadable media URLs from supported TikTok links.

---

## OkHttp

- `com.squareup.okhttp3:okhttp`
- `com.squareup.okhttp3:logging-interceptor`

OkHttp handles HTTP requests and direct media downloads.

The downloader uses OkHttp to stream video data directly to local storage.

The client also handles:

- User-Agent headers
- Request headers
- Redirects
- Connection timeout
- Read timeout
- Write timeout
- Basic HTTP logging

---

## Moshi

- `com.squareup.moshi:moshi-kotlin`
- `com.squareup.moshi:moshi-kotlin-codegen`

Moshi is used for JSON serialization and deserialization of API responses.

Retrofit uses the Moshi converter to convert TikWM API responses into Kotlin data models.

---

# 📥 Download System

The video downloader is implemented using Kotlin Coroutines and `Flow`.

The general workflow is:

    TikTok URL
         ↓
      TikWM API
         ↓
    Video information
         ↓
      Download URL
         ↓
        OkHttp
         ↓
    MediaStore / File
         ↓
    Download/DownTik/Video

The downloader exposes several states:

- Idle
- Downloading
- Completed
- Failed

During a download, the application reports:

- Downloaded bytes
- Total bytes
- Progress
- Percentage
- File size

This allows the UI to display real-time download progress.

---

# 🎵 Audio Extraction

DownTik can extract the audio track from downloaded video content.

The extraction system uses Android's native media APIs:

- `MediaExtractor`
- `MediaMuxer`
- `MediaCodec.BufferInfo`

The audio extraction workflow is:

    Video
      ↓
    Temporary video file
      ↓
    MediaExtractor
      ↓
    Audio track
      ↓
    MediaMuxer
      ↓
    M4A / MP4 audio
      ↓
    Download/DownTik/Audio

The audio track is demuxed instead of being re-encoded.

This allows the original audio stream to be extracted without an additional encoding step.

The extraction process reports multiple stages:

    Downloading source
          ↓
    Extracting audio
          ↓
      Saving audio
          ↓
       Completed

---

# ▶️ Media Playback

DownTik uses **AndroidX Media3 ExoPlayer** for media playback.

Libraries:

- `androidx.media3:media3-exoplayer`
- `androidx.media3:media3-ui-compose-material3`

The application maintains separate playback instances for:

- Video preview
- Audio playback

The player manager also contains recovery handling for certain transient audio playback errors commonly encountered with streamed media.

Audio playback includes metadata such as:

- Title
- Author
- Duration
- Current playback state

---

# 🖼️ Image Loading

DownTik uses **Coil** for image loading:

- `io.coil-kt:coil-compose`

Coil integrates directly with Jetpack Compose and is used for loading remote or local images inside the UI.

---

# 💾 Local Database

DownTik uses **Room** for local persistence.

Libraries:

- `androidx.room:room-runtime`
- `androidx.room:room-ktx`
- `androidx.room:room-compiler`

The database stores download history.

The architecture is:

    Room Database
         │
         └── DownloadHistoryDao
                    │
                    └── DownloadHistoryEntity

The database is named:

`downtik_database`

The current database version is `2`.

---

# 📂 Storage

Downloaded media is stored inside the public Downloads directory.

### Video

    Download/
    └── DownTik/
        └── Video/

### Audio

    Download/
    └── DownTik/
        └── Audio/

DownTik uses Android `MediaStore` on Android 10 and newer.

Older Android versions use the legacy public Downloads directory and media scanning.

This allows downloaded files to remain accessible through the device's file manager and other media applications.

---

# 🧹 File Naming

Downloaded filenames are sanitized before being written to storage.

Unsupported filename characters are replaced to prevent invalid filesystem names.

Long filenames are also limited to keep generated filenames manageable.

If a valid title cannot be generated, DownTik creates a timestamp-based filename:

`DownTik_YYYYMMDD_HHMMSS`

---

# 📜 Download History

DownTik keeps a local history of downloaded media.

The history system is backed by Room and allows the application to keep track of previously processed downloads without relying on a remote database.

---

# 🔄 Update System

DownTik includes an integrated GitHub-based update system.

The update architecture consists of:

    GitHub Releases
          ↓
    UpdateChecker
          ↓
    ReleaseChannelResolver
          ↓
    VersionComparator
          ↓
    APK validation
          ↓
    SHA-256 verification
          ↓
    UpdateDownloader
          ↓
    UpdateInstaller

The application can check GitHub Releases and determine whether a newer version is available.

---

## 📡 Release Channels

DownTik supports two update channels:

| Channel | Description |
|---|---|
| Stable | Regular production releases |
| Beta | Pre-release builds for testing |

The selected channel determines which GitHub releases are considered by the updater.

---

## 🔐 APK Verification

Before an update is accepted, the APK is validated.

The updater checks:

- Release information
- APK filename
- APK availability
- Version information
- SHA-256 digest

The expected APK filename follows:

`downtik-*.apk`

This prevents unrelated release assets from being treated as application updates.

---

# ⚙️ Settings

The Settings screen contains application configuration such as:

- Theme mode
- Update channel
- Update checking
- Application information

Theme modes include:

- System
- Light
- Dark

---

# 🧱 Architecture

DownTik separates the application into several functional layers.

    com.example
    │
    ├── data/
    │   ├── local/
    │   │   ├── AppDatabase
    │   │   ├── DownloadHistoryDao
    │   │   └── DownloadHistoryEntity
    │   │
    │   ├── model/
    │   │   └── API data models
    │   │
    │   ├── network/
    │   │   ├── NetworkClient
    │   │   └── TikwmApiService
    │   │
    │   └── repository/
    │       └── SettingsRepository
    │
    ├── downloader/
    │   ├── DownTikStorage
    │   ├── VideoDownloader
    │   └── AudioExtractor
    │
    ├── player/
    │   └── DownTikPlayerManager
    │
    ├── update/
    │   ├── GitHubRelease
    │   ├── ReleaseChannel
    │   ├── ReleaseChannelResolver
    │   ├── UpdateChecker
    │   ├── UpdateConfig
    │   ├── UpdateDownloader
    │   ├── UpdateInstaller
    │   ├── UpdateState
    │   ├── VersionComparator
    │   └── ui/
    │       └── UpdateDialog
    │
    └── ui/
        └── theme/

This structure keeps networking, storage, downloading, playback, updating, and UI logic separated from each other.

---

# 🧪 Testing

The project includes both unit tests and Android instrumentation tests.

Testing tools include:

- JUnit
- AndroidX Test
- Espresso
- Compose UI Test
- Robolectric
- Kotlin Coroutines Test

The project also contains dedicated tests for version comparison used by the update system.

---

# 🔧 Build Configuration

### Android Gradle Plugin

`9.1.1`

### Kotlin

`2.2.10`

### Gradle

The project uses the Gradle wrapper included in the repository.

### Java

The Android module is configured for:

`Java 11`

### SDK

    minSdk    = 24
    targetSdk = 36
    compileSdk = 36

---

# 🚀 Building the Project

## Requirements

- Android Studio
- JDK 11
- Android SDK
- Android SDK Platform 36
- Git

Clone the repository:

    git clone https://github.com/antoneeh10/DownTik-Android.git
    cd DownTik-Android

Build a debug APK:

    ./gradlew assembleDebug

Build a release APK:

    ./gradlew assembleRelease

---

# 📦 Release

The first public version of DownTik is:

`v1.0.0`

Version configuration:

    versionCode = 1
    versionName = "1.0.0"

Release APKs are distributed through GitHub Releases.

The built-in updater can use these releases to detect and download newer application versions.

---

# 🔒 Privacy & Permissions

DownTik primarily processes downloaded media locally on the device.

Network access is required to:

- Communicate with the TikWM API
- Retrieve TikTok media information
- Download media
- Check GitHub releases for application updates

Downloaded files are saved to the device's Downloads directory.

---

# ⚠️ Disclaimer

DownTik is an independent third-party application and is not affiliated with or endorsed by TikTok.

Users are responsible for ensuring that downloaded content is used in accordance with applicable laws, platform terms, and the rights of content creators.

---

# 📄 License

DownTik is licensed under the **MIT License**.

See the [LICENSE](LICENSE) file for the full license text.

---

## 👨‍💻 Development

DownTik is developed with a focus on keeping the application lightweight, maintainable, and straightforward.

The project favors targeted changes over unnecessary refactoring, with separate components for networking, downloading, media processing, storage, playback, and updates.

---

**DownTik**

*Lightweight TikTok media downloader for Android.*
