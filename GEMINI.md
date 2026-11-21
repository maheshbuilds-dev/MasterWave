# MasterWave Lifetime App Release & Update Rules

These rules apply permanently to all MasterWave releases, builds, and updates without exception:

1. **Release Deployment Sequence:**
   - Step 1: Compile release APK (`./gradlew assembleRelease`).
   - Step 2: Deploy to website first ([masterwave.vercel.app](https://masterwave.vercel.app)) with updated `assets/MasterWave.apk`.
   - Step 3: Install on the user's phone via ADB (`adb install -r ...`) when connected.
   - Step 4: Wait for user review and explicit confirmation before broadcasting the update.
   - Step 5: Upon user confirmation, update `update.json` (`versionCode`, `versionName`, `releaseNotes`) on the website to trigger the update for all installed apps.

2. **App Update Lifecycle & Play Protect Optimization:**
   - **Dialog Integrity:** The app must remain open when the system update confirmation dialog appears. Do NOT close the app prematurely upon checking, downloading, or committing the session.
   - **Real Button Confirmation & Silent Background Update:** When the user clicks the real system PackageInstaller [Update] button in the prompt, immediately close/terminate the active app process (`finishAffinity()`, stop foreground playback service, minimize to home) so that Android's `PackageInstaller` executes the in-place package update silently in the background without file-lock collisions, UI freezing, or Play Protect scan alerts.
   - **Cancel Safety:** If the user clicks [Cancel] in the system PackageInstaller dialog, the app must remain open normally without quitting.
   - **Data Preservation:** Replace only the app binary; never delete user playlists, favorites, preferences, or offline downloads.
   - **Auto-Relaunch:** Relaunch automatically upon update completion (`AppPackageReplacedReceiver`).
   - **Notification Cleanup & Suppression:** Automatically dismiss update notifications after update completes; suppress and never show update notifications or banners until the next newer version is released (`remoteVersionCode > currentVersionCode`).
   - **App Update Screen:** Keep the high-contrast "Update Check" button and "Share App" feature; do not include standalone raw APK download links on the update page.

3. **Music & Storage Integrity:**
   - **Zero Fake Songs:** Strictly never substitute trending or dummy songs into YouTube Liked songs or user playlists.
   - **Persistent Artwork:** Always embed ID3/MP4 album art and cache companion thumbnails in persistent internal storage so offline/downloaded songs never display blank thumbnails.
