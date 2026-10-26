# Veloura — White Transparent Glassroom Music Player

**Veloura** adalah aplikasi Android pemutar musik modern premium yang mengusung identitas visual **“White Transparent Glassroom”**: serba putih, transparan, elegan, minimalis, soft blur, glassmorphism, animasi halus, dan memiliki kedalaman visual yang terasa seperti kaca.

Dibangun 100% menggunakan **Kotlin + Jetpack Compose** (tanpa XML layout), **Material 3**, **Android Media3 (ExoPlayer + MediaSession)**, serta arsitektur **Clean Architecture / MVVM**.

---

## 💎 Identitas Visual & Glassroom Design System

Aplikasi didesain dengan tema utama **WHITE GLASSROOM**:
- **Background Utama**: Soft white / off-white misty gradient (`#F9FAFD` ➔ `#EFF3F8` ➔ `#E8EEF5`).
- **Glass Cards & Surfaces**: Lapisan putih semi-transparan dengan border highlight halus 1dp (`#99FFFFFF`), soft drop shadows, dan rounded corners 20–32dp.
- **Glass Components**:
  - `GlassCard`: Card berlapis transparan dengan frosted gradient & bayangan lembut.
  - `GlassButton`: Tombol frosted dengan animasi interaksi sentuh.
  - `GlassIconButton`: Circular glass button untuk aksi cepat.
  - `GlassBottomBar`: Floating glass bottom bar melayang di atas navigasi bawah.
  - `GlassTopBar`: Header transparan dengan greeting dinamis sesuai waktu (*"Good morning"*, *"Good evening"*).
  - `GlassSearchBar`: Search input frosted dengan clear button & live debounce.
  - `GlassChip`: Filter kategori transparan.
  - `GlassDialog`: Modal pop-up kaca untuk import Spotify & buat playlist baru.
  - `GlassSheet` (`QueueSheet`): Bottom sheet transparan untuk antrean putar musik.
  - `GlassSlider`: Seekbar progress audio ramping dengan format timestamp menit & detik.
  - `GlassMiniPlayer`: Persistent floating player di atas bottom navigation dengan progress line bawah dan dukungan swipe-up / tap untuk ekspansi.
  - `GlassPlayerControl`: Kontrol playback lengkap (Shuffle, Previous, Play/Pause circle dengan animasi, Next, Repeat).
  - `GlassPlaylistCard`: Card playlist dengan badge *Spotify Import*.
  - `GlassSongCard`: List item lagu dengan artwork, judul, artis, durasi, dan tombol toggle favorit interaktif.
  - `GlassSkeleton`: Placeholder loading bersinar shimmer kaca saat memuat data.

---

## ⚡ Arsitektur Audio & Stream Resolution (Hybrid Engine)

Veloura menggunakan pendekatan **Hybrid Stream Resolution** yang andal untuk memastikan musik selalu dapat diputar tanpa hambatan:

```
[Veloura Android App]
   ├── 1. Primary: On-Device YouTube Engine (InnerTube API - ANDROID_VR)
   │      └── Langsung menggunakan IP HP pengguna (Bebas blokir Cloud / Anti-Bot)
   │
   └── 2. Fallback: Backend Service (backend/server.py / Vercel Cloud)
          └── Fallback stream, rekomendasi lagu, dan sinkronisasi playlist
```

### 1. On-Device YouTube Stream Resolution (Bebas Blokir Cloud IP)
- **Bypass Bot Check**: Menghindari pemblokiran IP datacenter cloud (*"Sign in to confirm you're not a bot"*) dengan menyelesaikan stream langsung dari HP pengguna.
- **InnerTube API (`ANDROID_VR`)**: Menyamar sebagai client Android VR resmi untuk mendapatkan direct stream URL (`googlevideo.com` itag 140 / AAC 128 kbps) tanpa perlu signature deciphering.
- **Auto-Search**: Jika track berasal dari Spotify atau track ID tidak valid/unplayable, aplikasi otomatis mencari video audio terbaik via YouTube Search API.
- **ExoPlayer Compatibility**: Menggunakan User-Agent Oculus VR pada `DefaultHttpDataSource` agar terhindar dari error `HTTP 403 Forbidden`.

### 2. On-Device Synced Lyrics (LRCLIB)
- Mengambil lirik sinkron (*synced lyrics / LRC*) dan lirik polos (*plain lyrics*) secara otomatis langsung dari database publik **LRCLIB**.
- 100% gratis tanpa memerlukan API key atau akun berbayar.

### 3. Backend Service & Fallback Engine
Backend Python (`backend/server.py`) tetap tersedia sebagai penyedia pencarian, metadata, dan secondary fallback:
- `GET /api/health` ➔ Status kesehatan backend.
- `GET /api/search?q={query}&category={songs}` ➔ Pencarian audio melalui `ytsearch15:{query}`.
- `GET /api/track/{id}` ➔ Metadata detail track.
- `GET /api/stream/{id}` ➔ Ekstraksi audio stream URL cadangan.
- `GET /api/recommendations` ➔ Rekomendasi lagu populer, made for you, dan trending.
- `POST /api/spotify/import` ➔ Menerima URL playlist Spotify dan membaca metadata track.

#### Menjalankan Backend (Opsional):
```bash
python backend/server.py
```
Backend berjalan pada port `5000` (`http://localhost:5000` atau `http://10.0.2.2:5000` untuk Android Emulator). URL backend dapat dikonfigurasi langsung dari menu **Settings** di dalam aplikasi.

---

## 🏛️ Arsitektur Aplikasi

```
com.glassroom.music
├── data
│   ├── local
│   │   ├── GlassroomDbHelper.kt      (SQLite Open Helper + reactive invalidation signals)
│   │   ├── LocalDataSource.kt        (CRUD Playlist, PlaylistTrack, Track, Favorites, History)
│   │   └── PreferencesManager.kt     (DataStore Preferences: theme, audio, backend URL)
│   ├── remote
│   │   ├── MusicApiService.kt        (Retrofit Endpoints)
│   │   ├── NetworkClient.kt          (Dynamic base URL Retrofit & OkHttp)
│   │   └── dto/NetworkDto.kt         (DTO Models)
│   └── repository
│       ├── MusicRepositoryImpl.kt    (On-device stream resolution, remote search, Spotify import, lyrics)
│       └── PlaylistRepositoryImpl.kt (Manajemen playlist & favorit lokal)
│
├── domain
│   ├── model
│   │   ├── Track.kt                  (Domain Track model)
│   │   ├── Playlist.kt               (Playlist & PlaylistWithTracks)
│   │   ├── SpotifyImportState.kt     (Progress import: Reading, Matching, Complete)
│   │   └── MusicSource.kt            (Abstraction layer sumber audio)
│   └── repository
│       ├── MusicRepository.kt
│       └── PlaylistRepository.kt
│
├── playback
│   ├── MusicService.kt               (Android Media3 MediaSessionService + Foreground Notification)
│   └── PlayerManager.kt              (ExoPlayer controller, Oculus VR User-Agent, queue, shuffle, repeat)
│
├── ui
│   ├── components/                   (Reusable Glassroom Compose Components)
│   ├── theme/                        (Color, Type, Theme tokens)
│   ├── navigation/                   (Screen navigation routes)
│   ├── home/                         (HomeScreen + HomeViewModel)
│   ├── search/                       (SearchScreen + SearchViewModel)
│   ├── library/                      (LibraryScreen + LibraryViewModel)
│   ├── playlist/                     (PlaylistsScreen + PlaylistDetailScreen + PlaylistViewModel)
│   ├── player/                       (FullPlayerScreen)
│   ├── queue/                        (QueueSheet)
│   ├── settings/                     (SettingsScreen)
│   └── GlassroomApp.kt               (Root NavHost, Floating MiniPlayer, Animated FullPlayer)
│
├── di
│   └── GlassroomContainer.kt         (Singleton container injects Repositories & Player)
│
└── GlassroomApplication.kt           (App entry point & Coil disk cache setup)
```

---

## 🎧 Media Playback (Android Media3 / ExoPlayer)

1. **Background Playback**: Dilayani oleh `MusicService` (`MediaSessionService`) yang dideklarasikan dengan `foregroundServiceType="mediaPlayback"`.
2. **Audio Focus & Noisy Event**: Otomatis menjeda saat earphone/headset dicabut atau ada panggilan telepon masuk.
3. **Android Media Notification & Lock Screen**: Terhubung langsung dengan `MediaSession`.
4. **Queue & Controls**: Shuffle, Repeat (Off, All, One), Skip Next, Skip Previous, Seek scrubbing halus via `GlassSlider`.

---


## 📦 Build & Instalasi

Untuk mengompilasi APK debug:
```bash
.\gradlew assembleDebug
```
Output APK berada di:
`app/build/outputs/apk/debug/app-debug.apk`
