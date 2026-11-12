# MasterWave: Complete Screen Specifications (12 Major Screens)

---

## Screen A: Home Screen
- **Route:** `home`
- **Top App Bar:** Personalized greeting ("Good evening"), date indicator, search shortcut icon, settings button.
- **Content Blocks:**
  1. *Category Chips:* "All", "Trending", "Workout", "Chill", "Focus".
  2. *Hero Section ("Made For You"):* Infinite Radio station card with direct "Play" button.
  3. *Quick Access Shelf:* Grid of 4 pinned cards ("Liked Songs", "Recent Mix", "Heavy Rotation", "Local Files").
  4. *Genre Discovery Carousel:* High-contrast visual cards for Pop, Rock, Hip-Hop, Electronic, Classical.
  5. *New Releases Shelf:* Horizontal scrollable cards with album artwork, title, and artist.
- **Dock:** Mini-player anchored at bottom above navigation bar.

---

## Screen B: Search Screen
- **Route:** `search`
- **Header:** Full-width search bar with leading search icon, clear button, and voice search.
- **Filter Pills:** "Tracks", "Artists", "Albums", "Playlists", "Local".
- **States:**
  - *Initial / Idle:* Shows "Recent searches" with "Clear all" and genre browse tiles.
  - *Typing / Suggestions:* Real-time debounced query suggestions from Google Suggest.
  - *Results:* "Top Song" card followed by matching songs list with 3-dot action menus.

---

## Screen C: Now Playing (Hero Screen)
- **Route:** `player/full`
- **Visuals:** Immersive full-screen album art with dual-layer dynamic Gaussian ambient blur, inspired by `design/references/now-playing-reference.png`.
- **Top Bar:** Minimize chevron (left), "NOW PLAYING" centered title, 3-dot options menu (right).
- **Center Body:** 1:1 high-resolution album artwork hero card.
- **Metadata:** Track title (Headline Medium), artist name (Body Large), Favorite heart button, Lyrics toggle button.
- **Seeker:** Waveform/smooth progress bar with elapsed and remaining timecodes.
- **Audio Quality:** Pill badge displaying stream resolution (e.g. "OPUS 160K").
- **Transport Bar:** Shuffle, Previous, Large Crimson Play/Pause, Next, Repeat.
- **Gestures:** Swipe UP opens Queue; Swipe DOWN minimizes back to previous view.

---

## Screen D: Mini-Player Dock
- **Component:** Embedded floating dock across all top-level screens.
- **Layout:** 64dp height, 24dp corner radius, 12dp horizontal margin, translucent frosted glass surface.
- **Controls:** Thumbnail, Track title marquee, Artist, Play/Pause, Next, linear progress line on bottom border.

---

## Screen E: Library Screen
- **Route:** `library`
- **Sections:**
  - Pinned cards: "Liked Songs" (with total count) and "Local Audio".
  - Filter chips: "Playlists", "Downloaded", "Albums", "Artists".
  - User custom playlists with creation date, track count, and custom thumbnail grids.
  - Floating Action Button (FAB): "Create Playlist".

---

## Screen F: Album Page
- **Route:** `album/{albumId}`
- **Header:** Sticky collapsing toolbar with prominent album art, album title, artist link, release year, total duration.
- **Action Bar:** "Play All" (filled button), "Shuffle" (outlined button), "Save to Library" (icon).
- **Track List:** Numbered track list (1, 2, 3...) showing title, featured artists, duration, and individual option menus.

---

## Screen G: Artist Page
- **Route:** `artist/{artistId}`
- **Header:** Full-width artist hero banner image with dark gradient scrim, artist name, and monthly listener count.
- **Action Buttons:** "Play Artist", "Artist Radio", "Follow".
- **Sections:**
  - "Top Songs": Top 5 most popular songs with one-tap playback.
  - "Albums & Singles": Horizontal carousel of discography.
  - "About": Expandable artist biography with Wikipedia attribution.

---

## Screen H: Playlist Page
- **Route:** `playlist/{playlistId}`
- **Header:** Playlist cover (dynamic 4-album mosaic or custom art), title, author, track count, total playback time.
- **Actions:** "Play", "Shuffle", "Edit Playlist", "Export M3U".
- **Track List:** Reorderable track list with drag handles and swipe-to-delete.

---

## Screen I: Synchronized Lyrics Screen
- **Route:** `player/lyrics`
- **Visuals:** Full-screen dimmed ambient backdrop.
- **Header:** "LINE SYNC" indicator, lyrics sync offset adjuster (+/- ms), text size dialog.
- **Content:** Center-staged real-time scrolling lyrics powered by LRCLIB. Active line highlighted in crisp `#FFFFFF`; inactive lines dimmed at 40% opacity.
- **Bottom Strip:** Persistent seek bar and transport controls so playback can be controlled without leaving lyrics.

---

## Screen J: Playing Queue Screen
- **Route:** `player/queue`
- **Header:** "PLAYING QUEUE", currently playing track indicator, total upcoming count and duration.
- **Toolbar:** "Unlimited Queue" toggle, "Clear Queue", "Save as Playlist".
- **List:** Reorderable list of upcoming tracks with drag handles, track removal buttons, and next-up previews.

---

## Screen K: Settings Screen
- **Route:** `settings`
- **Categories:**
  - *Audio & Playback:* Streaming Quality, Equalizer (15-band parametric), Audio Normalization (ReplayGain), Crossfade duration, Gapless playback toggle, Audio focus policy.
  - *Appearance & Visuals:* Dark Obsidian Theme, Accent Color picker (Crimson default), AMOLED Mode, Dynamic Album Color, Liquid Glass effects.
  - *Storage & Cache:* Disk cache size, Clear artwork cache, Clear search history.
  - *About & Privacy:* Zero telemetry guarantee, Open-source licenses, GPL-3.0 notice, Version info.

---

## Screen L: Local Music Screen
- **Route:** `local`
- **Header:** Device audio overview, storage status, "Rescan Device Audio" button.
- **Tabs:** "All Songs", "Albums", "Artists", "Folders".
- **Features:** Instant MediaStore scanning, ID3 tag extraction, folder tree navigation, and offline playback.
