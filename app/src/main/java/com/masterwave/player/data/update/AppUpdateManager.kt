package com.masterwave.player.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.masterwave.player.BuildConfig
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * - Uses Android PackageInstaller.Session to trigger clean system updates without Play Protect test warnings.
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
    }

    fun checkForUpdate(silent: Boolean = false, onResult: ((UpdateInfo?) -> Unit)? = null) {
        if (_isChecking.value) return
        _isChecking.value = true

        scope.launch {
            try {
                val request = Request.Builder()
                    .url(UPDATE_MANIFEST_URL)
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .header("Pragma", "no-cache")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} from update manifest")
                }

                val bodyString = response.body?.string().orEmpty()
                val json = JSONObject(bodyString)

                val remoteVersionCode = json.optInt("versionCode", 0)
                val remoteVersionName = json.optString("versionName", "2.0.2")
                val releaseNotes = json.optString("releaseNotes", "New performance and visual enhancements.")
                val apkUrl = json.optString("apkUrl", "https://masterwave.vercel.app/assets/MasterWave.apk")
                val apkSize = json.optString("apkSize", "25 MB")
                val publishedAt = json.optString("publishedAt", "")
                val forceUpdate = json.optBoolean("forceUpdate", false)

                val currentCode = BuildConfig.VERSION_CODE
                Log.d(TAG, "Update check: local=$currentCode, remote=$remoteVersionCode ($remoteVersionName)")

                // Only trigger update if remote is strictly newer than installed build!
                if (remoteVersionCode > currentCode || forceUpdate) {
                    val update = UpdateInfo(
                        versionCode = remoteVersionCode,
                        versionName = remoteVersionName,
                        releaseNotes = releaseNotes,
                        apkUrl = apkUrl,
                        apkSize = apkSize,
                        publishedAt = publishedAt,
                        forceUpdate = forceUpdate
                    )
                    _availableUpdate.value = update

                    val dismissedCode = prefs.getInt("dismissed_version_code", -1)
                    _isDismissed.value = (dismissedCode == remoteVersionCode && !forceUpdate)

                    val lastNotified = prefs.getInt("last_notified_version", -1)
                    if (!silent && lastNotified < remoteVersionCode) {
                        postUpdateNotification(update)
                        prefs.edit().putInt("last_notified_version", remoteVersionCode).apply()
                    }

                    withContext(Dispatchers.Main) {
                        onResult?.invoke(update)
                    }
                } else {
                    _availableUpdate.value = null
                    _isDismissed.value = false
                    try {
                        notificationManager.cancel(UPDATE_NOTIFICATION_ID)
                    } catch (_: Exception) {}
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

    fun downloadAndInstall(update: UpdateInfo, onComplete: ((Boolean) -> Unit)? = null) {
        if (_downloadProgress.value != null) return
        _downloadProgress.value = 0f

        scope.launch {
            var tempFile: File? = null
            try {
                val updateDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
                val targetFile = File(updateDir, "MasterWave-v${update.versionName}.apk")
                tempFile = File(updateDir, "MasterWave-v${update.versionName}.apk.part")

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

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    throw IOException("Could not finalize APK file")
                }
                tempFile = null

                _downloadProgress.value = 1f
                _isInstalling.value = true

                withContext(Dispatchers.Main) {
                    installApk(targetFile)
                    onComplete?.invoke(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed downloading update", e)
                tempFile?.delete()
                _downloadProgress.value = null
                _isInstalling.value = false
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false)
                }
            }
        }
    }

    fun installApk(apkFile: File) {
        try {
            // Check unknown sources installation permission on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return
            }

            // Method 1: Android PackageInstaller.Session (Official direct system update API)
            // Displays the clean system "Update MasterWave? [Cancel] [Update]" prompt without Play Protect scan intercept
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val packageInstaller = context.packageManager.packageInstaller
                    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                        setAppPackageName(context.packageName)
                    }
                    val sessionId = packageInstaller.createSession(params)
                    val session = packageInstaller.openSession(sessionId)

                    apkFile.inputStream().use { input ->
                        session.openWrite("masterwave_update", 0, apkFile.length()).use { output ->
                            input.copyTo(output)
                            session.fsync(output)
                        }
                    }

                    val callbackIntent = Intent(context, AppUpdateInstallReceiver::class.java).apply {
                        action = AppUpdateInstallReceiver.ACTION_INSTALL_STATUS
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        sessionId,
                        callbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                    )
                    session.commit(pendingIntent.intentSender)
                    session.close()
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "PackageInstaller session failed; using FileProvider fallback", e)
                }
            }

            // Method 2: Standard Fallback via Intent.ACTION_VIEW
            val authority = "${context.packageName}.provider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch package installer", e)
        } finally {
            _downloadProgress.value = null
            _isInstalling.value = false
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
        const val UPDATE_MANIFEST_URL = "https://masterwave.vercel.app/update.json"
        private const val UPDATE_NOTIFICATION_ID = 20_001

        @Volatile
        private var INSTANCE: AppUpdateManager? = null

        fun getInstance(context: Context, okHttpClient: OkHttpClient): AppUpdateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppUpdateManager(context.applicationContext, okHttpClient).also { INSTANCE = it }
            }
        }
    }
}
