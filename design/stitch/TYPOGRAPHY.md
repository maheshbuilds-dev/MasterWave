# MasterWave: Typography Specification

---

## 1. Type Hierarchy & Scale

MasterWave employs a clean, high-legibility geometric sans-serif type system (Inter / Roboto) optimized for dark OLED screens:

| Style Name | Size | Line Height | Weight | Letter Spacing | Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Display Large** | 32sp | 40sp | Bold (700) | -0.5sp | Hero greetings ("Good evening"), Artist name |
| **Headline Medium**| 24sp | 30sp | SemiBold (600) | -0.2sp | Now Playing song title (Portrait) |
| **Headline Small** | 20sp | 26sp | SemiBold (600) | 0sp | Section titles ("Made For You", "Top Songs") |
| **Title Medium** | 16sp | 22sp | Medium (500) | 0.1sp | Track row titles in search and playlists |
| **Body Large** | 16sp | 24sp | Regular (400) | 0.15sp | Synced lyrics lines (active focus) |
| **Body Medium** | 14sp | 20sp | Regular (400) | 0.25sp | Artist subtitle, album description |
| **Label Large** | 14sp | 18sp | SemiBold (600) | 0.1sp | Pill buttons ("Play", "Filter chips") |
| **Label Small** | 11sp | 14sp | Bold (700) | 0.5sp | Uppercase badges ("NOW PLAYING", "AUDIO") |
| **Timecode Mono** | 12sp | 16sp | Medium (500) | 0sp | Elapsed & remaining time (Tabular figures) |

---

## 2. Typographic Rules

1. **Tabular Figures for Timecodes:** Elapsed and remaining time (`03:42`, `-01:15`) must use monospace or tabular numbers (`FontFeatureSettings = "tnum"`) to eliminate layout jitter during active playback.
2. **Title Marquee Behavior:** Titles longer than the available width scroll smoothly using an auto-repeating horizontal marquee with a 1.2s lead-in pause and 30dp fading edge mask.
3. **Multi-Script Unicode Support:** Correct line height and font fallback rendering for non-Latin scripts (Devanagari, Telugu, Tamil, Japanese, Korean) ensuring diacritics and matras are never clipped.
