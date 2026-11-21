package com.masterwave.player.playback

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MasterWavePlaybackService : MediaBrowserServiceCompat() {

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var player: MasterWavePlayer
        private set

    lateinit var mediaSession: MediaSessionCompat
        private set

    private lateinit var notificationManager: MasterWaveNotificationManager
    private var isForegroundService = false

    inner class LocalBinder : Binder() {
        fun getService(): MasterWavePlaybackService = this@MasterWavePlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        val repository = MusicRepository.getInstance(this)
        player = MasterWavePlayer(this, repository)

        mediaSession = MediaSessionCompat(this, "MasterWaveMediaSession").apply {
            setCallback(MediaSessionCallback())
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        notificationManager = MasterWaveNotificationManager(this, mediaSession)

        observePlayerState()
    }

    private fun observePlayerState() {
        scope.launch {
            player.currentTrack.collectLatest { track ->
                if (track != null) {
                    updateMediaSessionMetadata(track)
                    updateNotification(track, player.isPlaying.value)
                }
            }
        }

        scope.launch {
            player.isPlaying.collectLatest { isPlaying ->
                updatePlaybackState(isPlaying)
                val track = player.currentTrack.value
                if (track != null) {
                    updateNotification(track, isPlaying)
                }
            }
        }
    }

    private fun updateMediaSessionMetadata(track: Track) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationSeconds * 1000L)
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
            .setState(state, player.positionMs.value, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun updateNotification(track: Track, isPlaying: Boolean) {
        notificationManager.updateNotification(track, isPlaying) { notification ->
            if (isPlaying) {
                if (!isForegroundService) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            MasterWaveNotificationManager.NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                    } else {
                        startForeground(MasterWaveNotificationManager.NOTIFICATION_ID, notification)
                    }
                    isForegroundService = true
                } else {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(MasterWaveNotificationManager.NOTIFICATION_ID, notification)
                }
            } else {
                if (isForegroundService) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(false)
                    }
                    isForegroundService = false
                }
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(MasterWaveNotificationManager.NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            MasterWaveNotificationManager.ACTION_PLAY_PAUSE -> player.togglePlayPause()
            MasterWaveNotificationManager.ACTION_PREV -> player.playPrevious()
            MasterWaveNotificationManager.ACTION_NEXT -> player.playNext()
            com.masterwave.player.data.update.AppUpdateInstallActivity.ACTION_CLOSE_FOR_UPDATE -> {
                player.pause()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return if (SERVICE_INTERFACE == intent?.action) {
            super.onBind(intent) ?: binder
        } else {
            binder
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("masterwave_root", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            player.togglePlayPause()
        }

        override fun onPause() {
            player.togglePlayPause()
        }

        override fun onSkipToNext() {
            player.playNext()
        }

        override fun onSkipToPrevious() {
            player.playPrevious()
        }

        override fun onSeekTo(pos: Long) {
            player.seekTo(pos)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val prefs = getSharedPreferences("masterwave_audio_prefs", Context.MODE_PRIVATE)
        val stopWhenCleared = prefs.getBoolean("stop_when_app_cleared", false)
        if (stopWhenCleared) {
            player.pause()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        mediaSession.release()
        player.release()
    }
}
