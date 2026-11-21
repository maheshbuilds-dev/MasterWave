# MasterWave: Changelog

All notable changes and milestones for the MasterWave Android project will be documented in this file.

## [Unreleased] - 2026-11-19

### Completed
- **Phase A: Environment Inspection:**
  - Audited macOS Sonoma host, x86_64, MacBook Air 2018.
  - Audited Android SDK at `~/Library/Android/sdk` with Android 36.1 and platform-tools.
  - Verified ADB connection to Motorola Moto G45 5G (`ZN4223CJK9`, Android 16).
  - Verified GitHub access and cloned LastWave Native repository into research directory.
- **Phase B: LastWave Source Code Audit:**
  - Audited complete codebase (`Clash-Projects/LastWave-Native` HEAD `961331508a482a32b35029252fb27cffd151f989`).
  - Traced music search, InnerTube client rotation (`TVHTML5`, `VISIONOS`, `WEB_REMIX`), BotGuard PoToken integration, and NewPipeExtractor fallback.
  - Analyzed LRCLIB synchronized lyrics, local MediaStore scanning, and optional lossless addon pipeline.
  - Audited third-party dependencies and confirmed GPL-3.0 copyleft requirements.
- **Phase C: Real Physical Device Audit (Moto G45):**
  - Confirmed installed package `com.lastwave.app` (v5.1.2, code 30).
  - Captured 15 high-resolution screenshots and UIAutomator XML hierarchies across all major screens.
  - Confirmed active playback in Android MediaSession (`state=PLAYING(3)`), 48 kHz stereo AudioTrack, and foreground service notification.
  - Verified background and lockscreen playback continuity.
- **Phase D: Research Documentation Delivery:**
  - Published `LASTWAVE_SOURCE_AUDIT.md`.
  - Published `LASTWAVE_STREAMING_ARCHITECTURE.md`.
  - Published `LASTWAVE_FEATURE_INVENTORY.md`.
  - Published `PROVIDER_AND_API_ANALYSIS.md`.
  - Published `DEPENDENCY_AND_LICENSE_AUDIT.md`.
  - Published `PRIVACY_AND_SECURITY_AUDIT.md`.
  - Published `MASTERWAVE_TECHNICAL_ARCHITECTURE.md`.
  - Published `IMPLEMENTATION_ROADMAP.md`.
  - Published `TEST_PLAN.md`.
  - Published device audit reports: `REAL_DEVICE_AUDIT.md`, `SCREEN_ANALYSIS.md`, `PLAYBACK_BEHAVIOUR.md`, `ANDROID_MEDIA_INTEGRATION.md`, `NETWORK_OBSERVATIONS.md`, `MASTERWAVE_COMPARISON.md`.
- **Platform and Home Experience Update:**
  - Removed the Audius, Internet Archive, and Jamendo streaming adapters; catalog search and resolution now use local files and YouTube Music.
  - Replaced the Home hero's playlist cards with playable recommended songs and reshaped the header and split glass navigation dock from the supplied recording.
- **Home Discovery Personalization:**
  - Added genre and artist mixes derived from favorites and recent plays, with starter genres when metadata is unavailable.
  - Verified with `:app:testDebugUnitTest` and `:app:assembleDebug`.
- **Home Discovery Refinements:**
  - Corrected the Home Downloads tile to count downloaded tracks rather than lookup keys.
  - Made the Home header stay over scrolling content with a responsive glass treatment.
  - Changed the song carousel to a side-peek layout and expanded it to up to 24 distinct playable recommendations.
  - Moved Quick Picks directly below the carousel and show up to 24 distinct tracks without repeating short result lists.
- **Personalized Home and Downloads:**
  - Rotate the recommendation carousel away from its previous songs when enough fresh candidates exist, and avoid recently played tracks and duplicate song identities.
  - Build Quick Picks from unused personal recommendations before falling back to language-matched discovery.
  - Replaced the oversized playlist showcase with a compact search-pick rail and removed unsupported daily-update and fixed-count claims.
  - Restrict Downloads to completed tracks registered by in-app downloads; show percentage, completion, and failure notifications.
  - Restore the saved Light, Dark, AMOLED, or System theme after restart and make System follow Android's current appearance.
