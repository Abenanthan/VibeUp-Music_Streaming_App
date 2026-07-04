# VibeUp 🎧

VibeUp is a premium, high-performance Android music streaming and local player. Built with **Jetpack Compose** and **Media3 (ExoPlayer)**, it combines a stunning modern interface with a professional-grade audio engine.

## ✨ Key Features

### 🔊 Professional Audio Engine
- **Dual-Mode Equalizer**:
    - **Hardware EQ**: Access your device's built-in audio engine (Deepfield, Dolby Atmos, Dirac, etc.).
    - **10-Band Software EQ**: High-precision DSP using Transposed Direct Form II (TDF2) filters with per-sample smoothing for zero "zipper noise."
- **Audio Effects**: Bass Boost, Virtualizer (Surround Sound), Reverb presets, and Loudness Enhancer.
- **Crossfade & Gapless**: Smooth transitions between tracks with customizable fade durations.
- **Playback Speed**: Adjust tempo without affecting pitch.

### 🧠 Intelligent Playback
- **Smart Shuffle**: Spotify-style similarity scoring based on language, artist, and mood.
- **Atomic Reordering**: Real-time, continuous drag-and-drop queue management.
- **Session Persistence**: Resumes exactly where you left off, even after a device restart.

### 📝 Dynamic Lyrics
- **Synced (LRC)**: Beautifully animated, time-synced lyrics with tap-to-seek functionality.
- **Multilingual Support**: Automatic detection and manual switching between original and translated lyrics.
- **Offline Caching**: Lyrics are stored locally for instant access.

### 📊 Deep Integration
- **Advanced Stats**: Track your music journey with total plays, listening hours, streaks, and weekly charts.
- **Interactive Artist Profiles**: Explore full discographies, follower counts, and featured artist credits.
- **Offline Mode**: Comprehensive download manager with quality selection (320kbps/160kbps/96kbps).

### 🎨 Modern Experience
- **Adaptive UI**: Fully themed with a custom "Obsidian" palette and vibrant gradients.
- **Mini Player**: Persistent controls that follow you throughout the app.
- **Smart Search**: Real-time suggestions and popularity-based rankings powered by JioSaavn APIs.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Audio Engine**: [Android Media3 / ExoPlayer](https://developer.android.com/guide/topics/media/media3)
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Database**: [Room](https://developer.android.com/training/data-storage/room) (Local caching & Play History)
- **Persistence**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) (Session management)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) (with aggressive disk/memory caching)
- **Backend**: Firebase Auth & Firestore

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer.
- Android device or emulator running **Android 8.0 (API 26)** or higher.

### Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/Abenanthan/VibeUp-Music_Streaming_App
   ```
2. **Setup Firebase**:
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with the package name `com.vibeup.android`.
   - Download `google-services.json` and place it in the `app/` directory.
3. **Build**:
   - Clean and Rebuild project in Android Studio.
   - Deploy to your device.

## 📜 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for more details.

---
Developed with ❤️ by [Abenanthan](https://github.com/Abenanthan)
