DownTik

DownTik is a lightweight Android application for downloading TikTok videos and extracting audio from supported TikTok URLs.

The project focuses on a simple, fast, and focused user experience while keeping the application architecture easy to maintain and extend.

✨ Features

- 📥 TikTok video downloader
- 🎵 Audio extraction
- 🔗 TikTok URL input and handling
- 📤 Android share-to-download workflow
- 📁 Separate video and audio output folders
- 🔄 Built-in GitHub release update system
- 📡 Stable and Beta update channels
- ⚡ Lightweight and focused interface
- 📦 APK release distribution through GitHub Releases

🧰 Tech Stack

DownTik is built for Android using a modern Android development stack.

Android

- Kotlin — primary programming language
- Android SDK — Android application platform
- Jetpack / AndroidX — Android application components and utilities
- Material 3 — UI components and design foundation
- Media3 — media playback and media-related functionality

Downloader

- TikWM — used as the TikTok downloader service/API
- HTTP networking — used for communicating with the downloader service and retrieving downloadable media

Update System

DownTik includes a GitHub-based update system that checks releases from the project's GitHub repository.

The update system supports:

- Stable channel
- Beta channel
- GitHub Releases API
- APK validation
- SHA-256 digest verification
- Automatic detection of supported APK release assets

Only APK assets matching the expected release filename pattern are considered valid:

^downtik-.*\.apk$

The release system is designed to work without requiring GitHub authentication for reading public releases.

🎨 Design

DownTik uses a deliberately simple interface rather than trying to imitate the TikTok application itself.

The design focuses on:

- Clear visual hierarchy
- Minimal navigation
- Fast access to downloading
- Material 3 components
- Consistent spacing and typography
- Clear download states
- Simple status and feedback messages

The UI and UX are designed to remain consistent across updates. Changes should generally be small and targeted rather than redesigning the application unnecessarily.

Design Philosophy

«Simple interface, focused workflow.»

The application should make the main flow obvious:

TikTok URL
    ↓
Process
    ↓
Select / detect media
    ↓
Download
    ↓
Save locally

The interface is intentionally kept lightweight so the downloader remains the main focus.

📂 File Organization

Downloaded media is separated by type:

Videos/
└── DownTik/

Audio/
└── DownTik/

This keeps downloaded videos and extracted audio organized independently.

🏗️ Architecture

DownTik is structured around separated responsibilities so that downloader logic, media processing, UI, and update functionality do not become tightly coupled.

Major components include:

UI
│
├── URL input
├── Download controls
├── Download status
└── Update interface
        │
        ▼
Application Logic
│
├── URL handling
├── TikWM integration
├── Download management
└── Audio extraction
        │
        ▼
Storage
│
├── Video output
└── Audio output

GitHub Releases
        │
        ▼
Update System
├── Channel selection
├── Release detection
├── APK validation
└── SHA-256 verification

🔄 Update Channels

DownTik supports two release channels:

Channel| Purpose
Stable| Recommended production releases
Beta| Testing newer builds before stable release

The updater retrieves release information from GitHub and validates the APK before offering it as an update.

🛠️ Development

Requirements

- Android Studio
- Android SDK
- JDK compatible with the project's Gradle configuration
- Git

Clone

git clone https://github.com/antoneeh10/DownTik-Android.git
cd DownTik-Android

Build

Build the debug APK using Gradle:

./gradlew assembleDebug

For a release build:

./gradlew assembleRelease

The resulting APK can then be found in the project's Gradle build output directory.

📱 Application ID

The application uses:

com.downtik.android

The existing namespace remains:

com.example

This separation is intentional and does not require changing the existing UI structure.

🔐 Release Signing

Release APKs should be signed using the project's configured release signing setup.

Before publishing a release, verify:

1. The APK is correctly signed.
2. The APK filename matches the expected pattern.
3. The SHA-256 digest is available.
4. The GitHub Release contains the correct APK asset.
5. The selected release channel is correct.

📦 GitHub Releases

DownTik distributes public builds through GitHub Releases.

Repository:

antoneeh10/DownTik-Android

Release assets are validated before being considered by the in-app updater.

The updater expects APK assets following:

downtik-*.apk

and verifies their SHA-256 digest before installation.

🧪 Development Philosophy

DownTik follows a small, targeted change approach.

The goal is to avoid unnecessary changes to the existing application structure.

Changes should:

- Keep the existing UI/UX intact unless a change is specifically required.
- Preserve the existing architecture where possible.
- Address one focused problem at a time.
- Keep commits small and understandable.
- Avoid unrelated refactoring.
- Verify the application after each meaningful change.

📄 License

Add the project's license information here when a license has been selected.

---

DownTik
A lightweight TikTok downloader for Android.
