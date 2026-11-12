# LastWave Native: Physical Device Screen-by-Screen Visual & UI Audit

**Device Inspected:** Motorola Moto G45 5G (Android 16, 720 x 1600 px @ 120 Hz)  
**Audit Date:** 2026-11-12  
**Screenshots Directory:** `docs/device-audit/screenshots/`

---

## Screen Inventory & Detailed UI Analysis

### 1. Home Screen
- **Screenshot:** [01_home_screen.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/01_home_screen.png)
- **UI Hierarchy:** [01_home_screen.xml](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/01_home_screen.xml)
- **Layout Structure:**
  - Top header: Personalized time greeting ("Good afternoon"), current date, Downloads icon, Search shortcut icon, Settings avatar.
  - Horizontal chip row: "All", "Podcasts", "Workout", "Feel good", "Energize".
  - Hero Card ("MADE FOR YOU"): "Infinite Radio" with dynamic background and "Play" button.
  - "Quick access" horizontal shelf: "Liked Songs", "Discover", "Discover Mix".
  - "My genres" horizontal chips: "Pop", "Rock", "Hip Hop", "Indie".
  - Floating dock Mini-Player: Track title, artist, play/pause, next button, and progress bar.
  - Bottom navigation bar: 3 tabs ("Feed", "Stats", "Playlists").

### 2. Now Playing (Full Screen Player)
- **Screenshot:** [02_now_playing.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/02_now_playing.png)
- **UI Hierarchy:** [02_now_playing.xml](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/02_now_playing.xml)
- **Layout Structure:**
  - Header: Minimize chevron button (left), "NOW PLAYING" centered label, 3-dots "Song options" menu (right).
  - Center: Dominant square album artwork with dynamic dual-layer blurred ambient backdrop.
  - Song Info: Song title in bold typography, artist name below, Heart ("Like song") icon, and Lyrics toggle button.
  - Seek bar: Custom wavy progress bar, elapsed time ("0:00") and remaining time ("-3:56").
  - Audio quality pill: "Audio" badge displaying current stream resolution.
  - Core transport controls: Previous track, large pill-shaped Play/Pause button, Next track.
  - Bottom controls: Shuffle toggle, Audio controls sheet toggle (EQ), Watch music video toggle, Repeat toggle.

### 3. Synced Lyrics View
- **Screenshot:** [03_lyrics.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/03_lyrics.png)
- **UI Hierarchy:** [03_lyrics.xml](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/03_lyrics.xml)
- **Layout Structure:**
  - Header: Back to player chevron, "LYRICS" label, song title & artist marquee, lyrics sync offset adjuster, song options.
  - Mode indicator: "LINE SYNC" badge.
  - Main area: Vertically scrolling synchronized lyric lines with active line highlighted in bright white and inactive lines dimmed. Correctly renders complex non-Latin scripts (e.g. Telugu).
  - Bottom toolbar: Fullscreen lyrics expander, lyrics options menu, and persistent compact playback strip with seek bar and mini controls.

### 4. Playing Queue
- **Screenshot:** [04_queue.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/04_queue.png)
- **UI Hierarchy:** [04_queue.xml](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/04_queue.xml)
- **Layout Structure:**
  - Header: Back to player chevron, "PLAYING QUEUE" label, search in queue, song options.
  - Queue management toolbar: "Up next", count ("1 playing"), "Unlimited queue" toggle, "Lock queue", "Scroll to current song", "Clear queue except playing song".
  - Track list: Numbered rows with thumbnail, title, artist, 3-dots menu, and dedicated drag handle for drag-and-drop reordering.

### 5. Search Screen
- **Screenshot:** [05_search.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/05_search.png)
- **UI Hierarchy:** [05_search.xml](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/05_search.xml)
- **Layout Structure:**
  - Top search input bar with back button, placeholder "Search YouTube Music...", search icon, and microphone icon ("Search by voice").
  - Filter category chips: "Tracks", "Podcasts", "Artists", "Albums".
  - "Recent searches" list with "Clear all" action and individual remove 'X' icons.
  - "Explore genres & moods" quick access button.
  - Persistent mini player anchored above bottom bar.

### 6. Search Results
- **Screenshot:** [06_search_results.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/06_search_results.png)
- **UI Hierarchy:** [06_search_results.xml](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/06_search_results.xml)
- **Layout Structure:**
  - "TOP SONG" highlight card featuring primary match with one-tap play button.
  - Matching song results list displaying high-res thumbnail, title, artist, album name, and 3-dots options menu.

### 7. Playlists & Library
- **Screenshot:** [07_playlists_library.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/07_playlists_library.png)
- **UI Hierarchy:** [07_playlists_library.xml](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/07_playlists_library.xml)
- **Layout Structure:**
  - Top search bar for filtering user playlists.
  - Pinned cards: "Liked Songs" and "Local songs".
  - Section header: "2 Playlists • 0 Tracks" with "Create custom playlist" and "Sort playlists" actions.

### 8. Settings & Sub-Settings Screens
- **Settings Main:** [08_settings.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/08_settings.png)
- **Audio & Playback Settings:** [09_audio_settings.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/09_audio_settings.png)
  - Features: Streaming Quality selector ("Max (Up to 24-bit / 192 kHz) • YouTube Music fallback"), Download Quality, Bit-Perfect USB Mode, AutoMix beat-matching toggle, Music Haptics, Autoplay, Crossfade slider, Keep Screen Awake on Lyrics.
- **Appearance & Visuals Settings:** [12_appearance_settings.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/12_appearance_settings.png)
  - Features: Theme (Light/Dark/System), App Icon picker, AMOLED pure black mode, Dynamic Color (Material You), Liquid Glass translucent shaders, Material 3 Floating Navigation Bar toggle, Gesture-only navigation.
- **Provider Modules & Addons:** [11_provider_modules.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/11_provider_modules.png)
- **Last.fm & Discord Integration:** [10_settings_lastfm.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/10_settings_lastfm.png)

### 9. Artist & Album Pages
- **Artist Page:** [13_artist_page.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/13_artist_page.png)
  - Layout: Large hero artist banner, monthly audience metric ("3.5M"), action buttons ("Shuffle Artist", "Artist Radio", "Play Artist"), full expandable biography with Wikipedia attribution, and popular track list.
- **Album Page:** [14_album_page.png](file:///Users/mahesh/MasterWave/docs/device-audit/screenshots/14_album_page.png)
  - Layout: Large album artwork hero, title, artists, release metadata ("2026 • 40 minutes"), "Shuffle Album", "Play Album", "Save album to library", and numbered tracklist.
