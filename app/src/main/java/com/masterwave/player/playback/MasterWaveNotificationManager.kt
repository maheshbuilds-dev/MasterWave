package com.masterwave.player.playback

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.masterwave.player.MainActivity
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.R
import com.masterwave.player.data.model.Track

class MasterWaveNotificationManager(
    private val context: Context,
    private val mediaSession: MediaSessionCompat
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY_PAUSE = "com.masterwave.player.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.masterwave.player.ACTION_PREV"
        const val ACTION_NEXT = "com.masterwave.player.ACTION_NEXT"
    }

    fun buildNotification(
        track: Track,
        isPlaying: Boolean,
        artwork: Bitmap? = null
    ): Notification {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, MasterWavePlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePendingIntent = PendingIntent.getService(
            context,
            2,
            Intent(context, MasterWavePlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            context,
            3,
            Intent(context, MasterWavePlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val builder = NotificationCompat.Builder(context, MasterWaveApp.PLAYBACK_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(track.title)
            .setContentText(track.artist)
            .setSubText(track.album.ifBlank { "MasterWave" })
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        if (artwork != null) {
            builder.setLargeIcon(artwork)
        }

        return builder.build()
    }

    fun updateNotification(
        track: Track,
        isPlaying: Boolean,
        onNotificationReady: (Notification) -> Unit
    ) {
        val initialNotification = buildNotification(track, isPlaying, null)
        onNotificationReady(initialNotification)

        if (!track.artworkUrl.isNullOrBlank()) {
            val imageRequest = ImageRequest.Builder(context)
                .data(track.artworkUrl)
                .target { drawable ->
                    val bitmap = (drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val updated = buildNotification(track, isPlaying, bitmap)
                        notificationManager.notify(NOTIFICATION_ID, updated)
                    }
                }
                .build()
            ImageLoader(context).enqueue(imageRequest)
        }
    }
}
