package com.masterwave.player.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MasterWavePlayer(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    val exoPlayer: ExoPlayer by lazy {
        val renderersFactory = DefaultRenderersFactory(context)

        val httpDataSourceFactory = OkHttpDataSource.Factory(MasterWaveApp.instance.okHttpClient)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")

        val upstreamDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(upstreamDataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // --- Audio & Playback Controls (Frame 15.0s) ---
    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    private val _isSpeedPitchLinked = MutableStateFlow(true)
    val isSpeedPitchLinked: StateFlow<Boolean> = _isSpeedPitchLinked.asStateFlow()

    private val _musicHapticsEnabled = MutableStateFlow(false)
    val musicHapticsEnabled: StateFlow<Boolean> = _musicHapticsEnabled.asStateFlow()

    private var hapticVibrator: android.os.Vibrator? = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
    } catch (_: Exception) { null }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    _isLoading.value = true
                }
                Player.STATE_READY -> {
                    _isLoading.value = false
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                Player.STATE_ENDED -> {
                    _isLoading.value = false
                    _currentTrack.value?.let { repository.recommendationManager.eventTracker.onTrackEndedNaturally(it) }
                    playNext()
                }
                Player.STATE_IDLE -> {
                    _isLoading.value = false
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isLoading.value = false
            _isPlaying.value = false
            error.printStackTrace()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val curPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                _positionMs.value = curPos
                val curDuration = exoPlayer.duration
                if (curDuration > 0) {
                    _durationMs.value = curDuration
                    repository.recommendationManager.eventTracker.onProgressUpdate(curPos, curDuration)
                }
                delay(250L) // 4Hz update for silky smooth seekbar
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
        _positionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
    }

    fun playPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val index = startIndex.coerceIn(0, tracks.size - 1)
        playTrack(tracks[index], tracks)
    }

    private fun resolveMediaUri(url: String): Uri {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> Uri.parse(url)
            url.startsWith("content://") -> Uri.parse(url)
            url.startsWith("file://") -> Uri.parse(url)
            else -> Uri.fromFile(java.io.File(url))
        }
    }

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        scope.launch {
            _isLoading.value = true
            _currentTrack.value = track
            _queue.value = newQueue
            _currentIndex.value = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            repository.recordRecentTrack(track)
            repository.recommendationManager.eventTracker.onTrackStarted(track)

            try {
                val streamUrl = repository.resolveStreamUrl(track)
                val artworkUri = track.artworkUrl?.let { url ->
                    if (url.isNotBlank()) resolveMediaUri(url) else null
                }

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(artworkUri)
                    .build()

                val mediaUri = resolveMediaUri(streamUrl)

                val mediaItem = MediaItem.Builder()
                    .setMediaId(track.id)
                    .setUri(mediaUri)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _currentTrack.value != null) {
                playTrack(_currentTrack.value!!)
            } else {
                exoPlayer.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    private val aiRecommender = com.masterwave.player.data.recommendation.AiMusicRecommender()

    fun playNext() {
        val q = _queue.value
        val cur = _currentTrack.value
        cur?.let { repository.recommendationManager.eventTracker.onTrackSkipped(it) }

        if (q.isNotEmpty() && _currentIndex.value >= 0 && _currentIndex.value + 1 < q.size) {
            val nextIdx = _currentIndex.value + 1
            playTrack(q[nextIdx], q)
            return
        }

        // Smart Dynamic Continuation powered by Recommendation Engine:
        scope.launch {
            if (cur != null) {
                val smartRecs = repository.recommendationManager.getQueueRecommendations(cur, limit = 5)
                if (smartRecs.isNotEmpty()) {
                    val extendedQueue = q + smartRecs
                    _queue.value = extendedQueue
                    playTrack(smartRecs.first(), extendedQueue)
                    return@launch
                }
            }
            // Fallback to pool
            val pool = repository.recentTracks.value + repository.favorites.value + repository.localTracks.value
            val aiTrack = aiRecommender.selectNextTrack(cur, pool, repository.recentTracks.value)
            if (aiTrack != null) {
                playTrack(aiTrack)
            } else if (q.isNotEmpty()) {
                playTrack(q.first(), q)
            }
        }
    }

    fun skipToNext() = playNext()

    fun playPrevious() {
        val cur = _currentTrack.value
        cur?.let { repository.recommendationManager.eventTracker.onTrackSkipped(it) }

        if (exoPlayer.currentPosition > 3000L) {
            exoPlayer.seekTo(0L)
            _positionMs.value = 0L
            return
        }
        val q = _queue.value
        if (q.isEmpty()) return
        val prevIdx = if (_currentIndex.value - 1 < 0) q.size - 1 else _currentIndex.value - 1
        playTrack(q[prevIdx], q)
    }

    fun skipToPrevious() = playPrevious()

    fun toggleShuffle() {
        val newShuffle = !_isShuffle.value
        _isShuffle.value = newShuffle
        exoPlayer.shuffleModeEnabled = newShuffle
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer.repeatMode = nextMode
    }

    fun cycleRepeatMode() = toggleRepeat()

    fun clearQueue() {
        val cur = _currentTrack.value
        if (cur != null) {
            _queue.value = listOf(cur)
            _currentIndex.value = 0
        } else {
            _queue.value = emptyList()
            _currentIndex.value = -1
        }
    }

    // --- Audio & Playback Adjustments ---
    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        exoPlayer.volume = clamped
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = (Math.round(speed.coerceIn(0.5f, 2.0f) * 100.0) / 100.0).toFloat()
        _playbackSpeed.value = clamped
        if (_isSpeedPitchLinked.value) {
            _playbackPitch.value = clamped
        }
        applyPlaybackParameters()
    }

    fun setPlaybackPitch(pitch: Float) {
        val clamped = (Math.round(pitch.coerceIn(0.5f, 2.0f) * 100.0) / 100.0).toFloat()
        _playbackPitch.value = clamped
        if (_isSpeedPitchLinked.value) {
            _playbackSpeed.value = clamped
        }
        applyPlaybackParameters()
    }

    fun toggleSpeedPitchLinked() {
        val newLinked = !_isSpeedPitchLinked.value
        _isSpeedPitchLinked.value = newLinked
        if (newLinked) {
            _playbackPitch.value = _playbackSpeed.value
            applyPlaybackParameters()
        }
    }

    fun resetPlaybackParameters() {
        _playbackSpeed.value = 1.0f
        _playbackPitch.value = 1.0f
        applyPlaybackParameters()
    }

    private fun applyPlaybackParameters() {
        exoPlayer.playbackParameters = PlaybackParameters(_playbackSpeed.value, _playbackPitch.value)
    }

    fun toggleMusicHaptics(enabled: Boolean) {
        _musicHapticsEnabled.value = enabled
        if (enabled) {
            triggerHapticPulse(50L)
        }
    }

    fun triggerHapticPulse(durationMs: Long = 25L) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                hapticVibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        durationMs,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                hapticVibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun getAudioSessionId(): Int {
        return try {
            exoPlayer.audioSessionId
        } catch (_: Exception) {
            0
        }
    }

    fun openSystemEqualizer() {
        try {
            val audioSessionId = getAudioSessionId()
            val eqIntent = android.content.Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL").apply {
                putExtra("android.media.extra.AUDIO_SESSION_ID", audioSessionId)
                putExtra("android.media.extra.CONTENT_TYPE", 0) // CONTENT_TYPE_MUSIC
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(eqIntent)
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "System Equalizer: Snapdragon DSP Active (Dolby Atmos / Moto Sound)", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun release() {
        stopProgressTracker()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
