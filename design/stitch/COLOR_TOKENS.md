# MasterWave: Color Tokens Specification

**Theme:** Obsidian & Crimson (Dark Mode Primary)

---

## 1. Palette Architecture

### 1.1 Primary Canvas & Surface Tokens
| Token Name | Hex Code | Purpose / Usage |
| :--- | :--- | :--- |
| `color-canvas-obsidian` | `#08080A` | Ultimate background canvas behind all views |
| `color-surface-charcoal` | `#121216` | Card backgrounds, shelf containers, bottom bars |
| `color-surface-elevated` | `#1A1A22` | Dialog surfaces, bottom sheets, search input field |
| `color-surface-glass-fill` | `rgba(255, 255, 255, 0.06)` | Translucent pills, chip backgrounds |
| `color-surface-glass-border`| `rgba(255, 255, 255, 0.12)` | Subtle 1dp borders on translucent elements |

### 1.2 Accent & Brand Tokens (Crimson & Burgundy)
| Token Name | Hex Code | Purpose / Usage |
| :--- | :--- | :--- |
| `color-accent-crimson` | `#E53935` | Primary brand accent, active play button, active seek head |
| `color-accent-crimson-hover`| `#EF5350` | Interactive touch highlight state |
| `color-accent-burgundy` | `#880E4F` | Secondary accent, ambient glow base, active tab indicator |
| `color-accent-burgundy-dim`| `#4A0025` | Deep background gradient tint |
| `color-accent-heart` | `#FF1744` | Liked track heart indicator |

### 1.3 Text & Iconography Contrast Tokens
| Token Name | Hex Code | Purpose / Usage |
| :--- | :--- | :--- |
| `color-text-primary` | `#F5F5F7` | Track titles, main headings, active lyrics lines |
| `color-text-secondary` | `#A1A1A8` | Artist names, album titles, durations, inactive tabs |
| `color-text-tertiary` | `#686873` | Inactive lyrics lines, timestamps, placeholder text |
| `color-icon-active` | `#FFFFFF` | Transport icons (Play, Pause, Skip, Like) |
| `color-icon-muted` | `#8E8E93` | Shuffle off, Repeat off, inactive menus |

---

## 2. Dynamic Palette Derivation (Artwork Extraction)

When a track is playing, MasterWave extracts 3 color seeds using `androidx.palette.graphics.Palette`:
1. **Dominant Ambient:** Blended into a Gaussian blur canvas at 35% opacity over `color-canvas-obsidian`.
2. **Vibrant Accent:** Replaces default crimson for seekbar glow and playback badge if user enables "Artwork Match Accent".
3. **Muted Companion:** Tints the background of the Lyrics staging view.
