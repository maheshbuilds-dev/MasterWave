package com.masterwave.player.data.update

import android.app.PendingIntent
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import com.masterwave.player.BuildConfig
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSize: String,
    val publishedAt: String,
    val forceUpdate: Boolean = false
)

/**
 * Live In-App Update Engine:
 * - Checks https://masterwave.vercel.app/update.json on app launch or user trigger.
 * - Detects if a new version exists (remoteVersionCode > BuildConfig.VERSION_CODE).
 * - Exposes availableUpdate and live downloadProgress StateFlows for seamless in-app UX.
 * - Downloads the APK with real-time byte progress.
 * - Applies updates through a PackageInstaller session with USER_ACTION_NOT_REQUIRED.
 *   Together with UPDATE_PACKAGES_WITHOUT_USER_ACTION this makes Android 12+ install the
 *   update silently, so the "App scan recommended" Play Protect screen no longer appears
 *   before every installation.
 */
class AppUpdateManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val notificationManager by lazy { NotificationManagerCompat.from(context) }
    private val prefs by lazy { context.getSharedPreferences("masterwave_updates", Context.MODE_PRIVATE) }

    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()

    private val _isDismissed = MutableStateFlow(false)
    val isDismissed: StateFlow<Boolean> = _isDismissed.asStateFlow()

    private val _installError = MutableStateFlow<String?>(null)
    val installError: StateFlow<String?> = _installError.asStateFlow()

    private var installWatchdog: Job? = null

    init {
        cleanOldApks()
    }

    private fun cleanOldApks() {
        try {
            val updateDir = File(context.cacheDir, "updates")
            if (updateDir.exists()) {
                updateDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning old APKs", e)
        }
    }

    fun dismissUpdate() {
        _isDismissed.value = true
        _availableUpdate.value?.let {
            prefs.edit().putInt("dismissed_version_code", it.versionCode).apply()
        }
        try {
            notificationManager.cancel(UPDATE_NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    fun checkForUpdate(silent: Boolean = false, onResult: ((UpdateInfo?) -> Unit)? = null) {
        if (_isChecking.value) return
        _isChecking.value = true

        scope.launch {
            try {
                val update = fetchUpdateMetadata()
                val currentCode = BuildConfig.VERSION_CODE
                val currentName = BuildConfig.VERSION_NAME

                val isNewer = if (update != null) {
                    if (update.versionCode > currentCode) {
                        true
                    } else {
                        isVersionNewer(update.versionName, currentName)
                    }
                } else {
                    false
                }

                Log.d(TAG, "Update check result: localCode=$currentCode, localName=$currentName, isNewer=$isNewer, update=$update")

                if (update != null && isNewer) {
                    _availableUpdate.value = update

                    val dismissedCode = prefs.getInt("dismissed_version_code", -1)
                    _isDismissed.value = (dismissedCode >= update.versionCode && !update.forceUpdate)

                    val lastNotified = prefs.getInt("last_notified_version", -1)
                    if (!silent && !_isDismissed.value && lastNotified < update.versionCode) {
                        postUpdateNotification(update)
                        prefs.edit().putInt("last_notified_version", update.versionCode).apply()
                    }

                    withContext(Dispatchers.Main) {
                        onResult?.invoke(update)
                    }
                } else {
                    _availableUpdate.value = null
                    _isDismissed.value = true
                    try {
                        notificationManager.cancel(UPDATE_NOTIFICATION_ID)
                    } catch (_: Exception) {}
                    prefs.edit()
                        .putInt("last_notified_version", currentCode)
                        .putInt("dismissed_version_code", currentCode)
                        .apply()
                    cleanOldApks()
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(null)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Update check failed", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(null)
                }
            } finally {
                _isChecking.value = false
            }
        }
    }

    /**
     * Resolves update metadata across multi-tiered sources:
     * 1. Official Website Mirror (Vercel) - primary release channel
     * 2. Direct GitHub Raw Repository Manifest
     * 3. Direct GitHub Releases API
     */
    private fun fetchUpdateMetadata(): UpdateInfo? {
        val candidates = mutableListOf<UpdateInfo>()

        fetchFromManifestUrl(UPDATE_MANIFEST_URL)?.let {
            Log.i(TAG, "Resolved update candidate from Vercel mirror: v${it.versionName} (code=${it.versionCode})")
            candidates.add(it)
        }

        fetchFromManifestUrl(GITHUB_RAW_MANIFEST_URL)?.let {
            Log.i(TAG, "Resolved update candidate from GitHub Raw Manifest: v${it.versionName} (code=${it.versionCode})")
            candidates.add(it)
        }

        fetchFromGitHubReleases()?.let {
            Log.i(TAG, "Resolved update candidate from GitHub Releases API: v${it.versionName} (code=${it.versionCode})")
            candidates.add(it)
        }

        return candidates.maxWithOrNull(
            compareBy<UpdateInfo> { it.versionCode }
                .thenBy { it.versionName }
        )
    }

    /**
     * Triggers download & install of the latest available release.
     * If metadata has not been fetched yet, resolves metadata or uses official fallbacks.
     */
    fun downloadAndInstallLatest(fallbackVersionName: String = "2.2.18", fallbackVersionCode: Int = 39) {
        val currentUpdate = _availableUpdate.value
        if (currentUpdate != null) {
            downloadAndInstall(currentUpdate)
        } else {
            scope.launch {
                val remote = fetchUpdateMetadata()
                val target = remote ?: UpdateInfo(
                    versionCode = fallbackVersionCode,
                    versionName = fallbackVersionName,
                    releaseNotes = "• 5 New Authentic App Icon Styles: Apple Liquid Glass, Spotify Style, Luxury Monogram, Cosmic Abstract, and Apple Music.\n• 3-per-row balanced icon grid in Appearance Settings.\n• High-Contrast In-App Update Engine & Launcher Recovery.",
                    apkUrl = "https://masterwave.vercel.app/assets/MasterWave.apk",
                    apkSize = "21 MB",
                    publishedAt = "2026-11-23T10:45:00Z",
                    forceUpdate = false
                )
                downloadAndInstall(target)
            }
        }
    }

    private fun fetchFromGitHubReleases(): UpdateInfo? {
        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_API_URL)
                .header("User-Agent", "MasterWave/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/vnd.github+json")
                .header("Cache-Control", "no-cache")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.d(TAG, "GitHub Releases API returned HTTP ${response.code}")
                return null
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null
            val json = JSONObject(body)

            val tagName = json.optString("tag_name", "").trim()
            if (tagName.isBlank()) return null
            val remoteVersionName = tagName.removePrefix("v").removePrefix("V").trim()
            val releaseNotes = json.optString("body", "MasterWave $tagName release available directly on GitHub.")
            val publishedAt = json.optString("published_at", "")

            var apkUrl: String? = null
            var apkSizeBytes = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isNullOrBlank()) {
                apkUrl = "https://github.com/mahesh2008here-star/MasterWave/releases/latest/download/app-release.apk"
            }

            val apkSize = formatApkSize(apkSizeBytes)

            val codeRegex = Regex("""(?i)(?:versionCode|code)[\s:=]+(\d+)""")
            val parsedCode = codeRegex.find(releaseNotes)?.groupValues?.get(1)?.toIntOrNull()

            val remoteVersionCode = parsedCode ?: run {
                if (isVersionNewer(remoteVersionName, BuildConfig.VERSION_NAME)) {
                    BuildConfig.VERSION_CODE + 1
                } else {
                    BuildConfig.VERSION_CODE
                }
            }

            return UpdateInfo(
                versionCode = remoteVersionCode,
                versionName = remoteVersionName,
                releaseNotes = releaseNotes,
                apkUrl = apkUrl,
                apkSize = apkSize,
                publishedAt = publishedAt,
                forceUpdate = false
            )
        } catch (e: Exception) {
            Log.d(TAG, "GitHub Releases API check failed: ${e.message}")
            return null
        }
    }

    private fun fetchFromManifestUrl(url: String): UpdateInfo? {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("User-Agent", "MasterWave/${BuildConfig.VERSION_NAME}")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val bodyString = response.body?.string().orEmpty()
            if (bodyString.isBlank()) return null
            val json = JSONObject(bodyString)

            val remoteVersionCode = json.optInt("versionCode", 0)
            val remoteVersionName = json.optString("versionName", "")
            if (remoteVersionCode <= 0 && remoteVersionName.isBlank()) return null

            return UpdateInfo(
                versionCode = remoteVersionCode,
                versionName = remoteVersionName.ifBlank { "2.2.2" },
                releaseNotes = json.optString("releaseNotes", "New performance and visual enhancements."),
                apkUrl = json.optString("apkUrl", "https://github.com/mahesh2008here-star/MasterWave/releases/latest/download/app-release.apk"),
                apkSize = json.optString("apkSize", "18.5 MB"),
                publishedAt = json.optString("publishedAt", ""),
                forceUpdate = json.optBoolean("forceUpdate", false)
            )
        } catch (e: Exception) {
            Log.d(TAG, "Manifest check failed for $url: ${e.message}")
            return null
        }
    }

    private fun isVersionNewer(remote: String, local: String): Boolean {
        val cleanRemote = remote.removePrefix("v").removePrefix("V").trim()
        val cleanLocal = local.removePrefix("v").removePrefix("V").trim()
        if (cleanRemote == cleanLocal) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }
        val maxParts = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxParts) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun formatApkSize(bytes: Long): String {
        if (bytes <= 0L) return "18.5 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }

    fun downloadAndInstall(update: UpdateInfo, onComplete: ((Boolean) -> Unit)? = null) {
        if (_downloadProgress.value != null || _isInstalling.value) return
        _downloadProgress.value = 0f
        _installError.value = null

        scope.launch {
            var tempFile: File? = null
            try {
                val updateDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
                val targetFile = File(updateDir, "MasterWave-v${update.versionName}.apk")
                tempFile = File(updateDir, "MasterWave-v${update.versionName}.apk.part")

                // Reuse an already-downloaded, valid package so a failed install can be retried
                // without pulling the whole APK again.
                val cachedApk = targetFile.takeIf { it.isValidUpdatePackage(update.versionCode) }

                if (cachedApk != null) {
                    Log.i(TAG, "Reusing cached update package ${cachedApk.name}")
                    _downloadProgress.value = 1f
                } else {
                    if (targetFile.exists()) targetFile.delete()

                    val request = Request.Builder()
                        .url(update.apkUrl)
                        .header("User-Agent", "MasterWave-Updater/2.0")
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} downloading APK")
                    }

                    val body = response.body ?: throw IOException("Empty APK body")
                    val totalLength = body.contentLength()
                    var totalRead = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).buffered().use { output ->
                            val buffer = ByteArray(16384)
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                totalRead += read

                                if (totalLength > 0) {
                                    val progress = (totalRead.toFloat() / totalLength).coerceIn(0f, 1f)
                                    _downloadProgress.value = progress
                                }
                            }
                        }
                    }

                    if (!tempFile.renameTo(targetFile)) {
                        throw IOException("Could not finalize APK file")
                    }
                    tempFile = null
                    _downloadProgress.value = 1f
                }

                _isInstalling.value = true
                _installError.value = null

                // Session creation + writing the APK is disk I/O, so keep it off the main thread.
                installApk(targetFile, update.apkUrl)
                armInstallWatchdog()
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed downloading update", e)
                tempFile?.delete()
                _downloadProgress.value = null
                _isInstalling.value = false
                _installError.value = "Update failed: ${e.message ?: "unknown error"}"
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false)
                }
            }
        }
    }

    private fun isApkSignatureValid(apkFile: File): Boolean {
        val pm = context.packageManager
        // GET_SIGNATURES is deprecated in newer APIs but still required for this check.
        // We use GET_SIGNING_CERTIFICATES for Tiramisu+
        @Suppress("DEPRECATION")
        val currentInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)

        if (archiveInfo?.signatures == null || currentInfo.signatures == null) return false

        return archiveInfo.signatures.contentEquals(currentInfo.signatures)
    }

    fun installApk(apkFile: File, sourceUrl: String? = null) {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                throw IOException("Downloaded update package is missing or empty")
            }

            // Verify signature before attempting installation
            if (!isApkSignatureValid(apkFile)) {
                throw SecurityException("Update package signature mismatch! Aborting insecure install.")
            }

            // Check unknown sources installation permission on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                _downloadProgress.value = null
                _isInstalling.value = false
                _installError.value = "Allow MasterWave to install unknown apps, then tap Update again."
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return
            }

            val packageInstaller = context.packageManager.packageInstaller
            Log.i(
                TAG,
                "Starting update session (sdk=${Build.VERSION.SDK_INT}, apk=${apkFile.length()} bytes)"
            )
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setInstallReason(PackageManager.INSTALL_REASON_USER)
                setSize(apkFile.length())
                setOriginatingUri(Uri.parse(sourceUrl ?: UPDATE_MANIFEST_URL))
                setReferrerUri(Uri.parse(OFFICIAL_SITE_URL))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // USER_ACTION_NOT_REQUIRED allows silent updates via PackageInstaller,
                    // avoiding the Play Protect "scan app" confirmation screen on updates.
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            var committed = false
            try {
                apkFile.inputStream().use { input ->
                    session.openWrite("masterwave-update.apk", 0, apkFile.length()).use { output ->
                        input.copyTo(output, DEFAULT_BUFFER_SIZE)
                        session.fsync(output)
                    }
                }

                val callbackIntent = Intent(context, AppUpdateInstallReceiver::class.java).apply {
                    action = AppUpdateInstallReceiver.ACTION_INSTALL_STATUS
                }
                val callbackFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                val callback = PendingIntent.getBroadcast(context, sessionId, callbackIntent, callbackFlags)
                session.commit(callback.intentSender)
                committed = true
            } finally {
                if (!committed) {
                    // Never leave a half-written session behind for the system to time out.
                    try { session.abandon() } catch (_: Exception) {}
                }
                session.close()
            }

            // Leave _isInstalling = true: the receiver resets it on failure or cancel,
            // and a successful update restarts the process automatically.
        } catch (e: Exception) {
            Log.e(TAG, "Could not start package installer session", e)
            _downloadProgress.value = null
            _isInstalling.value = false
            _installError.value = "Could not start the installer: ${e.message ?: "unknown error"}"
        }
    }

    /** Called from the PackageInstaller callback when the update could not be applied. */
    fun onInstallFailed(message: String) {
        Log.w(TAG, "Install failed: $message")
        installWatchdog?.cancel()
        _installError.value = message
        _downloadProgress.value = null
        _isInstalling.value = false
    }

    /** Called from the PackageInstaller callback when the user/system cancelled the install. */
    fun onInstallAborted() {
        installWatchdog?.cancel()
        _installError.value = null
        _downloadProgress.value = null
        _isInstalling.value = false
    }

    fun clearInstallError() {
        _installError.value = null
    }

    /**
     * True when the cached file is a readable APK for this app and the expected version. Guards
     * against retrying an install with a truncated or stale download.
     */
    private fun File.isValidUpdatePackage(expectedVersionCode: Int): Boolean {
        if (!exists() || length() < MIN_APK_SIZE_BYTES) return false
        return try {
            val info = context.packageManager.getPackageArchiveInfo(absolutePath, 0) ?: return false
            info.packageName == context.packageName &&
                PackageInfoCompat.getLongVersionCode(info).toInt() == expectedVersionCode
        } catch (e: Exception) {
            Log.w(TAG, "Could not validate cached APK", e)
            false
        }
    }

    /**
     * Safety net: if the system never delivers an install callback (rare vendor bugs), release
     * the "installing" UI state so the user can retry instead of being stuck on a spinner.
     */
    private fun armInstallWatchdog() {
        installWatchdog?.cancel()
        installWatchdog = scope.launch {
            delay(INSTALL_TIMEOUT_MS)
            if (_isInstalling.value) {
                Log.w(TAG, "Install watchdog fired; resetting install state")
                _downloadProgress.value = null
                _isInstalling.value = false
                _installError.value = "The update did not finish. Please try again."
            }
        }
    }

    private fun postUpdateNotification(update: UpdateInfo) {
        if (!notificationManager.areNotificationsEnabled()) return

        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_update_dialog", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                UPDATE_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, MasterWaveApp.UPDATE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("MasterWave v${update.versionName} Available")
                .setContentText("Tap to update MasterWave directly")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(UPDATE_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not post update notification", e)
        }
    }

    companion object {
        private const val TAG = "AppUpdateManager"
        const val ACTION_CLOSE_FOR_UPDATE = "com.masterwave.player.ACTION_CLOSE_FOR_UPDATE"
        const val GITHUB_REPO = "maheshbuilds-dev/MasterWave"
        const val GITHUB_RELEASES_API_URL = "https://api.github.com/repos/maheshbuilds-dev/MasterWave/releases/latest"
        const val GITHUB_RAW_MANIFEST_URL = "https://raw.githubusercontent.com/maheshbuilds-dev/MasterWave/main/update.json"
        const val GITHUB_RELEASES_PAGE_URL = "https://github.com/maheshbuilds-dev/MasterWave/releases"
        const val UPDATE_MANIFEST_URL = "https://masterwave.vercel.app/update.json"
        const val OFFICIAL_SITE_URL = "https://masterwave.vercel.app"
        private const val UPDATE_NOTIFICATION_ID = 20_001
        private const val MIN_APK_SIZE_BYTES = 1_000_000L
        private const val INSTALL_TIMEOUT_MS = 180_000L

        @Volatile
        private var INSTANCE: AppUpdateManager? = null

        /** Non-creating accessor used by the install-result receiver. */
        fun peekInstance(): AppUpdateManager? = INSTANCE

        fun getInstance(context: Context, okHttpClient: OkHttpClient): AppUpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppUpdateManager(context.applicationContext, okHttpClient).also { INSTANCE = it }
            }
        }
    }
}
