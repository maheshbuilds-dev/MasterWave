# MasterWave: Component Specification

---

## 1. Mini-Player Floating Dock (`MiniPlayerDock`)

- **Visual Style:** Floating pill surface elevated 8dp above the bottom navigation bar.
- **Dimensions:** Width `calc(100% - 24dp)`, Height `64dp`, Corner Radius `24dp`.
- **Background:** `color-surface-elevated` (`#1A1A22`) with 85% alpha and 24dp backdrop blur + 1dp border `rgba(255,255,255,0.1)`.
- **Components:**
  - Leading: 48x48dp rounded album thumbnail (corner radius 12dp).
  - Center: Column with Track Title (Title Medium, 1-line marquee) and Artist Name (Body Small, `#A1A1A8`).
  - Trailing: Play/Pause circular icon button (40x40dp), Next track icon button (40x40dp).
  - Bottom edge: Continuous 2dp progress indicator track colored with `color-accent-crimson`.
- **Interactions:**
  - Tap: Expands to full-screen Now Playing with spring physics.
  - Swipe Left/Right: Skips to next/previous track.
  - Swipe Down: Dismisses playback.

---

## 2. Now Playing Hero Screen Components

### 2.1 Hero Artwork (`ArtworkHero`)
- **Dimensions:** Square 1:1 aspect ratio, width `min(screenWidth - 48dp, 360dp)`.
- **Corner Radius:** 20dp with subtle inner border `rgba(255,255,255,0.08)`.
- **Shadow:** 24dp elevation shadow with dynamic color matching the album palette at 40% opacity.

### 2.2 Fluid Artwork Ambient Background (`AmbientBackdrop`)
- **Implementation:** Dual-layer canvas.
  - Layer 1: Gaussian-blurred album art bitmap (blur radius 45dp) scaled to fill viewport.
  - Layer 2: Radial gradient vignette (Dark Obsidian `#08080A` around screen edges and bottom 40%).

### 2.3 Progress & Wave Seekbar (`MasterWaveSeekBar`)
- **Height:** 32dp touch target with 4dp active track bar.
- **Track Color (Played):** `color-accent-crimson` (`#E53935`).
- **Track Color (Buffered):** `rgba(255, 255, 255, 0.25)`.
- **Track Color (Unplayed):** `rgba(255, 255, 255, 0.10)`.
- **Thumb:** 14dp circular head that expands to 18dp during drag gestures.

### 2.4 Transport Controls Row (`TransportControls`)
- **Layout:** Horizontal row, `SpaceEvenly`, vertical alignment centered.
- **Shuffle & Repeat:** 44x44dp touch targets, subtle white when inactive, glowing crimson when active.
- **Previous & Next:** 48x48dp touch targets, high-contrast `#FFFFFF`.
- **Play/Pause Hero Button:** 68x68dp circle filled with `color-accent-crimson` (`#E53935`), 28dp white vector icon, tactile haptic feedback on press.

---

## 3. Lyrics Components (`SyncedLyricsView`)

- **Staging:** Centered vertical list with 120dp top/bottom padding to allow active line to sit at eye level (top 38% of viewport).
- **Active Lyric Row:**
  - Typography: 22sp SemiBold, `#FFFFFF`, 100% opacity.
  - Animation: Scale transition (1.0 -> 1.05) and smooth auto-scroll on timestamp trigger.
- **Inactive Lyric Rows:**
  - Typography: 18sp Medium, `#686873`, 40% opacity.
  - Tap: Seeks playback to that timestamp immediately.
- **Header:** Lyrics sync status badge ("LINE SYNC" or "UNSYNCED"), sync offset button, font size adjuster.
