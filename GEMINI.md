# MasterWave Lifetime App Release & Update Rules

These rules apply permanently to all MasterWave releases, builds, and updates without exception:

1. **Release Deployment Sequence:**
   - Step 1: Compile release APK (`./gradlew assembleRelease`).
   - Step 2: Deploy to website first ([masterwave.vercel.app](https://masterwave.vercel.app)) with updated `assets/MasterWave.apk`.
   - Step 3: Install on the user's phone via ADB (`adb install -r ...`) when connected.
   - Step 4: Wait for user review and explicit confirmation before broadcasting the update.
   - Step 5: Upon user confirmation, update `update.json` (`versionCode`, `versionName`, `releaseNotes`) on the website to trigger the update for all installed apps.

2. **App Update Lifecycle & Play Protect Optimization:**
   - **Immediate Close:** When the user taps update, immediately terminate the active app process so `PackageInstaller` executes an in-place update without file-lock collisions or Play Protect scan alerts.
   - **Data Preservation:** Replace only the app binary; never delete user playlists, favorites, preferences, or offline downloads.
   - **Auto-Relaunch:** Relaunch automatically upon update completion (`AppPackageReplacedReceiver`).
   - **Notification Cleanup:** Automatically dismiss update notifications after update; never show update prompts to users already on the latest version.
   - **App Update Screen:** Keep the high-contrast "Update Check" button and "Share App" feature; do not include standalone raw APK download links on the update page.

3. **Music & Storage Integrity:**
   - **Zero Fake Songs:** Strictly never substitute trending or dummy songs into YouTube Liked songs or user playlists.
   - **Persistent Artwork:** Always embed ID3/MP4 album art and cache companion thumbnails in persistent internal storage so offline/downloaded songs never display blank thumbnails.
