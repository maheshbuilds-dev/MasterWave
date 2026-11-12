# LastWave vs. MasterWave: Architectural & Experiential Comparison

**Date:** 2026-11-12  
**Baseline Reference:** LastWave Native v5.1.2 on Motorola Moto G45 5G  
**New Application Target:** MasterWave (`com.masterwave.player`)

---

## 1. Feature & Architecture Comparison Matrix

| Architectural Area | LastWave Native (Observed & Audited) | MasterWave Target Design | Value & Rationale for MasterWave |
| :--- | :--- | :--- | :--- |
| **Catalog & Search** | YouTube Music InnerTube + Google Suggestions | YouTube Music InnerTube + Google Suggestions | Identical free, massive global music catalog without accounts |
| **Stream Resolution** | Multi-client InnerTube fast-path + BotGuard PoToken + NewPipe | Multi-client InnerTube fast-path + BotGuard PoToken + NewPipe fallback | Retains the rock-solid, zero-cost streaming mechanism |
| **Addon System** | External Tidal/FLAC addon with HMAC native signing + `.lwp` modules | Clean provider interface; no mandatory external addon or encrypted binary packages | Eliminates dependency on external servers and maintenance overhead |
| **Audio Decoding** | Jellyfin Media3 FFmpeg software decoder (GPL-3.0) + ExoPlayer | Media3 ExoPlayer + FFmpeg software decoder (Opus itag 251 prioritized) | Guarantees deterministic, bug-free audio decoding across all OEMs |
| **Lyrics Experience** | LRCLIB line-sync, syllable TTML, translation, custom offset | LRCLIB millisecond line-sync with enhanced smooth auto-scroll & typography | Cleaner, more readable lyrics staging; fluid karaoke scroll |
| **Now Playing UI** | Electric Lime & Teal theme, Liquid Glass backdrop | Premium Dark Obsidian & Deep Crimson/Burgundy aesthetic inspired by reference design | Superior, more elegant, immersive visual identity |
| **Local Music** | MediaStore auto-discovery + SAF multi-tree folder indexing | MediaStore auto-discovery + folder browsing + `.lrc` sidecar loader | First-class offline listening capability out of the box |
| **Background Playback**| Foreground service (`mediaPlayback`) + MediaSessionCompat | Foreground service (`mediaPlayback`) + MediaSessionCompat | Uninterrupted playback through lockscreen, sleep, and app switching |
| **Performance Profile**| ~192 MB PSS memory footprint | < 150 MB PSS target footprint on Snapdragon 6s Gen 3 | Lighter memory usage, faster cold start, silky 120 Hz Compose frame rate |

---

## 2. Key Enhancements Delivered in MasterWave

1. **Original Visual Identity:** Instead of LastWave's Electric Lime branding, MasterWave embraces a deep graphite/charcoal canvas with deep crimson and burgundy accents, elevated typography, and dynamic artwork-driven ambient glows.
2. **Simplified, Autonomous Architecture:** Normal streaming is 100% self-contained. No external companion servers or proprietary addon formats are required.
3. **Optimized for 120 Hz Displays:** Compose micro-interactions are tuned with hardware-accelerated spring physics and low layout overhead to achieve locked 120 FPS on the Moto G45's Snapdragon 6s Gen 3.
4. **Enhanced Queue Ergonomics:** Improved one-handed queue drag-and-drop operations with instant swipe-to-dismiss and persistent queue caching.
