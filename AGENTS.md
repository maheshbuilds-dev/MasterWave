# MasterWave: Autonomous Agent & Developer Guide

## 1. Project Goals
MasterWave is a complete, premium, free-to-use Android music player inspired by LastWave Native, built for the Motorola Moto G45 5G (Android 16, 8 GB RAM, Snapdragon 6s Gen 3, 120 Hz).
- Zero mandatory subscriptions, paywalls, or inserted ads.
- Zero mandatory paid API keys or external server dependencies.
- Free, anonymous music streaming powered by YouTube Music InnerTube and NewPipeExtractor fallback.
- Real-time synchronized lyrics via open LRCLIB.
- Local audio file playback via Android MediaStore.
- Ultra-polished, premium dark UI inspired by `design/references/now-playing-reference.png`.

---

## 2. Technical Architecture
- **Language & Framework:** 100% Kotlin with Jetpack Compose (Material 3 Expressive).
- **Architecture Pattern:** Clean Architecture + MVI/MVVM with Unidirectional Data Flow (StateFlow / SharedFlow).
- **Audio Engine:** AndroidX Media3 ExoPlayer with FFmpeg software decoder (Opus itag 251 priority).
- **Service Architecture:** `MasterWavePlaybackService` running as an Android `mediaPlayback` foreground service with `MediaSessionCompat`.
- **Local Persistence:** Room SQLite for playlists, history, and favorites; DataStore for user preferences.

---

## 3. Coding Standards & Conventions
- **Clean Separation:** Domain use cases must not depend on Android UI frameworks or specific network clients.
- **Immutable State:** All UI state models must be annotated with `@Immutable` or `@Stable`.
- **Coroutines & Scopes:** Never use `GlobalScope`. Use structured concurrency with lifecycle-aware ViewModel scopes and service coroutine scopes.
- **Resource Cleanup:** Always release ExoPlayer instances, bitmaps, audio sinks, and cursors.
- **Error Handling:** Network and stream resolution failures must fail gracefully with actionable UI states (no unhandled crashes or ANRs).

---

## 4. Dependencies & Licensing
- **License:** GNU General Public License v3.0 (GPL-3.0-only).
- **Key Dependencies:**
  - `androidx.compose.*` (BOM 2024.12.01)
  - `androidx.media3:media3-exoplayer:1.2.1`
  - `org.jellyfin.media3:media3-ffmpeg-decoder:1.2.1+1` (GPL-3.0)
  - `com.github.TeamNewPipe:NewPipeExtractor:v0.26.5` (GPL-3.0)
  - `com.github.MetrolistGroup.innertubex:innertubex:0.11.2` (GPL-3.0)
  - `com.squareup.okhttp3:okhttp:4.12.0`
  - `io.coil-kt:coil-compose:2.7.0`
  - `androidx.room:*` (2.7.1)
  - `androidx.datastore:datastore-preferences:1.1.1`

---

## 5. Design Rules (MasterWave Identity)
- **Palette:** Deep Obsidian Black (`#08080A`), Dark Charcoal (`#121216`), Deep Crimson Accent (`#E53935`), Burgundy Muted (`#880E4F`), Translucent Scrims.
- **Hero Screen:** Full-screen immersive album art with dynamic dual-layer Gaussian blur, bold modern typography, and large tactile controls.
- **Performance:** 120 Hz smooth physics animations, zero dropped frames.

---

## 6. Testing & Device Safety Rules
- **Physical Device:** Motorola Moto G45 5G (`ZN4223CJK9`).
- **Safety Policy:**
  - READ-ONLY diagnostics permitted without prompting.
  - NEVER root, flash, modify system partitions, unlock bootloader, or uninstall user apps.
  - NEVER install APKs without explicit user confirmation.
- **Verification Rule:** Never claim a test passed without executing it and recording real output.

---

## 7. Current Progress & Milestones
- [x] Phase A: Environment & Tooling Verification.
- [x] Phase B: Complete LastWave Source Code Audit.
- [x] Phase C: Physical Device Audit on Moto G45 (Screenshots, UI hierarchies, MediaSession, AudioTrack, Network logs).
- [x] Phase D: Research & Architectural Documentation published.
- [/] Phase E: Design System & Screen Specifications (In Progress).
- [ ] Phase F: Android Project Implementation.
- [ ] Phase G: Testing on Moto G45.
- [ ] Phase H: Final Release APK.

---

## 8. Known Limitations
- Google Stitch tool is a web-based Google Labs design canvas without a local CLI or connected MCP tool in this environment.
- Reference image `design/references/now-playing-reference.png` is tracked as a project reference; we will provide visual design specifications accordingly.
- Unofficial InnerTube APIs are subject to periodic upstream signature changes; mitigated by multi-client rotation and NewPipeExtractor fallback.
