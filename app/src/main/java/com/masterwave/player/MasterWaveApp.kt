package com.masterwave.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class MasterWaveApp : Application(), ImageLoaderFactory {

    companion object {
        const val PLAYBACK_CHANNEL_ID = "masterwave_playback_v1"
        const val DOWNLOAD_CHANNEL_ID = "masterwave_downloads_v1"
        const val UPDATE_CHANNEL_ID = "masterwave_updates_v1"
        lateinit var instance: MasterWaveApp
            private set
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val requestWithUserAgent = if (request.header("User-Agent") == null) {
                    request.newBuilder()
                        .header("User-Agent", "MasterWave/2.0 (Android music player)")
                        .build()
                } else {
                    request
                }
                var response = chain.proceed(requestWithUserAgent)
                val urlStr = request.url.toString()
                // Automatic 404 recovery: YouTube maxresdefault.jpg / hq720.jpg fallback to hqdefault.jpg
                if (!response.isSuccessful && response.code == 404) {
                    if (urlStr.contains("maxresdefault.jpg")) {
                        response.close()
                        val fallbackUrl = urlStr.replace("maxresdefault.jpg", "hqdefault.jpg")
                        val fallbackRequest = requestWithUserAgent.newBuilder().url(fallbackUrl).build()
                        response = chain.proceed(fallbackRequest)
                    } else if (urlStr.contains("hq720.jpg")) {
                        response.close()
                        val fallbackUrl = urlStr.replace("hq720.jpg", "hqdefault.jpg")
                        val fallbackRequest = requestWithUserAgent.newBuilder().url(fallbackUrl).build()
                        response = chain.proceed(fallbackRequest)
                    }
                }
                response
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.masterwave.player.data.icon.AppIconManager.ensureValidLauncherAlias(this)
        createNotificationChannels()
        // Automated cache clean engine (every 1 hour)
        com.masterwave.player.data.cache.CacheCleanManager.getInstance(this).startAutoCleanup()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(PLAYBACK_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val downloadChannel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                getString(R.string.download_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_notification_channel_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
            val updateChannel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "MasterWave App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new MasterWave stable releases and features"
                setShowBadge(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            notificationManager?.createNotificationChannel(downloadChannel)
            notificationManager?.createNotificationChannel(updateChannel)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024) // 150 MB fast image cache
                    .build()
            }
            .okHttpClient(okHttpClient)
            .crossfade(150)
            .respectCacheHeaders(false)
            .build()
    }
}
