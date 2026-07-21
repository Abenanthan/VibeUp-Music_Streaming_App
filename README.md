<div align="center">

<img src="https://raw.githubusercontent.com/Abenanthan/VibeUp-Music_Streaming_App/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="120" height="120" alt="VibeUp Logo" />

# VibeUp 

### *Your Music. Your Vibe. Unleashed.*

**A premium Android music streaming & local player — built for audiophiles who refuse to compromise.**

<br/>

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![ExoPlayer](https://img.shields.io/badge/Media3-ExoPlayer-FF6D00?style=for-the-badge&logo=google&logoColor=white)](https://developer.android.com/guide/topics/media/media3)

<br/>

[![Download APK](https://img.shields.io/badge/⬇️%20Download%20Debug%20APK-Latest%20Build-8B5CF6?style=for-the-badge)](https://github.com/Abenanthan/VibeUp-Music_Streaming_App/releases/latest)

</div>

---

<br/>

## 📸 What Makes VibeUp Different?

> Most music apps give you a player. VibeUp gives you an **experience** — dual equalizers, real-time lyrics, smart shuffle, persistent sessions, and a UI that actually looks good.

<br/>

---

## ✨ Features at a Glance

<table>
<tr>
<td width="50%" valign="top">

### 🔊 Professional Audio Engine
**Dual-Mode Equalizer**
- **Hardware EQ** — Tap into your device's native audio engine. Deepfield on Vivo, Dolby Atmos on Samsung/OnePlus, Dirac on Xiaomi. All from inside VibeUp.
- **10-Band Software EQ** — High-precision DSP using Transposed Direct Form II (TDF2) filters with per-sample smoothing for zero "zipper noise" on any device.

**Effects Suite**
- Bass Boost · Virtualizer (Surround Sound) · Reverb Presets · Loudness Enhancer
- **Crossfade & Gapless** — Smooth transitions with customizable fade durations
- **Playback Speed** — 0.5× to 2.0× tempo control without pitch shift

</td>
<td width="50%" valign="top">

### 🧠 Intelligent Playback
- **Smart Shuffle** — Spotify-style similarity scoring using language, artist, album, and mood signals
- **Atomic Queue Reordering** — Real-time drag-and-drop with auto-scroll and instant response
- **Session Persistence** — Resumes exactly where you left off, even after a device restart or force-close

### 📝 Dynamic Lyrics
- **Synced LRC** — Animated, time-locked lyrics with tap-to-seek
- **Multilingual** — Auto-detect + manual switch between original and translated lyrics
- **Offline Cache** — Lyrics stored locally for instant, no-network access

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📊 Deep Listening Stats
- Total plays · Hours listened · Day streaks
- Top 5 songs & artists with play count bars
- 7-day weekly chart with bar visualization
- Peak listening hour detection
- Surfaced in a live widget on Profile & Library screens

</td>
<td width="50%" valign="top">

### 🎨 Themes & Visual Design
Six hand-crafted themes inspired by real design systems:

| Theme | Inspired By |
|---|---|
| **Classic Purple** | VibeUp original |
| **Obsidian** | Linear.app |
| **Aurora** | Darkroom |
| **Crimson** | Pocketcasts |
| **Sakura** | Bear app |
| **Void** | Vercel |
| **Ember** | Spotify Encore |

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🗺️ Artist Universe
- Full artist profiles with hero banner, follower count & biography
- Top songs + discography with album artwork
- "More from this artist" row in the player
- Multi-artist song credits (Primary + Featured) with individual Explore buttons

</td>
<td width="50%" valign="top">

### 📶 Offline-First
- Download manager with three quality tiers — **320kbps / 160kbps / 96kbps**
- Auto-detects no network → seamlessly switches to downloaded library
- Offline banner with "back online" auto-reload
- Downloads playable even when fully airplane-mode

</td>
</tr>
</table>

---

## 🛠️ Tech Stack

```
Language          Kotlin
UI                Jetpack Compose (Material 3)
Audio Engine      Android Media3 / ExoPlayer
Dependency Inject Hilt
Database          Room  — local cache & play history
Persistence       DataStore — session & theme management
Networking        Retrofit + OkHttp + custom interceptors
Image Loading     Coil (disk + memory cache)
Backend           Firebase Auth & Firestore
Music API         JioSaavn (self-hosted worker)
Lyrics API        LRCLib
```

---

## 📁 Project Structure

```
com.vibeup.android
├── data/
│   ├── local/          Room DAOs, entities (songs, history, downloads)
│   ├── remote/         Retrofit services, DTOs, mappers
│   └── repository/     Repository implementations
├── domain/
│   ├── model/          Song, Artist, Playlist, ArtistCredit
│   ├── repository/     Repository interfaces
│   └── usecase/        PlaySongUseCase, etc.
├── presentation/
│   ├── home/           HomeScreen + HomeViewModel
│   ├── player/         PlayerScreen, QueueScreen, LyricsScreen
│   ├── library/        LibraryScreen, DownloadsScreen
│   ├── search/         SearchScreen
│   ├── artist/         ArtistScreen + ArtistViewModel
│   ├── stats/          StatsScreen + StatsViewModel
│   └── settings/       SettingsScreen (Theme picker)
├── service/
│   ├── MusicPlayerService.kt   MediaLibraryService + notification
│   ├── PlayerManager.kt        ExoPlayer lifecycle + queue logic
│   ├── AudioEffectsManager.kt  Hardware EQ + Android audiofx
│   └── PlayerStateCache.kt     Session persistence to disk
└── ui/theme/
    ├── AppTheme.kt     VibeTheme enum + VibeColorScheme
    ├── ThemeManager.kt Singleton theme persistence
    └── LocalTheme.kt   CompositionLocal for theme access
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio **Hedgehog (2023.1.1)** or newer
- Android device or emulator running **Android 8.0 (API 26)+**

### Build from Source

```bash
# 1. Clone
git clone https://github.com/Abenanthan/VibeUp-Music_Streaming_App
cd VibeUp-Music_Streaming_App

# 2. Firebase setup
# - Create project at https://console.firebase.google.com
# - Add Android app with package: com.vibeup.android
# - Download google-services.json → place in app/

# 3. Build & run
./gradlew installDebug
```

### Or just download

[![Download APK](https://img.shields.io/badge/⬇️%20Download%20Debug%20APK-Latest%20Build-8B5CF6?style=for-the-badge)](https://github.com/Abenanthan/VibeUp-Music_Streaming_App/releases/latest)

> **Note:** If prompted, enable *Install from Unknown Sources* in your Android settings.

---

## 🔑 Key Technical Decisions

| Decision | Why |
|---|---|
| **Custom language interceptor** for JioSaavn API | Default wrapper excludes English songs for Indian IPs — OkHttp interceptor injects `languages=english,hindi,...` on every request |
| **Dual search services** | `JioSaavnDirectApiService` hits `jiosaavn.com/api.php` for search (language-aware); `SaavnApiService` hits the wrapper for song detail + stream URL (decrypted) |
| **`mutableStateListOf` in QueueScreen** | Prevents Compose from resetting drag gesture state mid-drag by mutating in-place rather than replacing the list reference |
| **`setMediaItems → prepare()` (no `play()`)** on session restore | Mirrors Echo Music's `onPlaybackResumption` pattern — puts ExoPlayer in `STATE_READY` at the saved position without auto-playing |
| **Per-event state saves** in `PlayerStateCache` | Saves on `onTimelineChanged`, `onIsPlayingChanged`, `onPositionDiscontinuity` — same three events Echo Music uses |

---

<div align="center">

Built with ❤️ and way too many late nights by [**Abenanthan**](https://github.com/Abenanthan)

*If VibeUp made your commute better, drop a ⭐ — it means a lot.*

[![GitHub stars](https://img.shields.io/github/stars/Abenanthan/VibeUp-Music_Streaming_App?style=social)](https://github.com/Abenanthan/VibeUp-Music_Streaming_App/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/Abenanthan/VibeUp-Music_Streaming_App?style=social)](https://github.com/Abenanthan/VibeUp-Music_Streaming_App/network/members)

</div>
