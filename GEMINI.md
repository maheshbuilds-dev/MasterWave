# MasterWave Lifetime App Release & Update Rules

These rules apply permanently to all MasterWave releases, builds, and updates without exception:

1. **Release Deployment Sequence:**
   - Step 1: Compile release APK (`./gradlew assembleRelease`).
   - Step 2: Deploy to website first ([masterwave.vercel.app](https://masterwave.vercel.app)) with updated `assets/MasterWave.apk`.
   - Step 3: Install on the user's phone via ADB (`adb install -r ...`) when connected.
   - Step 4: Wait for user review and explicit confirmation before broadcasting the update.
   - Step 5: Upon user confirmation, update `update.json` (`versionCode`, `versionName`, `releaseNotes`) on the website to trigger the update for all installed apps.

2. **App Update Lifecycle & Play Protect Optimization:**
   - **Silent Self-Update (permanent, no scan prompt):** Updates MUST be applied through `PackageInstaller.Session` with `SessionParams.setRequireUserAction(USER_ACTION_NOT_REQUIRED)`, and `AndroidManifest.xml` MUST declare `android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION`. On Android 12+ this makes the system apply the update itself, so the "Play Protect hasn't seen this app before / App scan recommended" screen never appears before installation. Never regress to `Intent.ACTION_VIEW` + FileProvider for installs.
   - **No Trampoline Activity:** Do not add a translucent/blank helper Activity to bridge the install. That pattern is flagged as tapjacking by Play Protect and was deliberately removed. The install result is delivered to the non-exported `AppUpdateInstallReceiver` broadcast receiver instead.
   - **Dialog Integrity:** Do NOT close the app process before committing the session. The app must stay alive while the download completes and the session commits.
   - **Fallback Confirmation Only:** If the system still returns `STATUS_PENDING_USER_ACTION`, the receiver may launch the provided confirmation intent as a last resort, but this is a fallback, not the normal path.
   - **Cancel Safety:** If the user or system cancels the install (`STATUS_FAILURE_ABORTED`), the app must remain open normally, reset install state, and allow retrying without a re-download loop.
   - **Data Preservation:** Replace only the app binary; never delete user playlists, favorites, preferences, or offline downloads.
   - **Auto-Relaunch:** Relaunch automatically upon update completion (`AppPackageReplacedReceiver`).
   - **Notification Cleanup & Suppression:** Automatically dismiss update notifications after update completes; suppress and never show update notifications or banners until the next newer version is released (`remoteVersionCode > currentVersionCode`).
   - **App Update Screen:** Keep the high-contrast "Update Check" button and "Share App" feature; do not include standalone raw APK download links on the update page.

3. **Music & Storage Integrity:**
   - **Zero Fake Songs:** Strictly never substitute trending or dummy songs into YouTube Liked songs or user playlists.
   - **Persistent Artwork:** Always embed ID3/MP4 album art and cache companion thumbnails in persistent internal storage so offline/downloaded songs never display blank thumbnails.
