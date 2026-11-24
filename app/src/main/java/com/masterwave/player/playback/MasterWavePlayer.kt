package com.masterwave.player.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
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
import androidx.media3.exoplayer.DefaultLoadControl
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serialisable snapshot of the listening session. Written continuously while a track
 * is loaded so a service restart (process death, OEM task killer, reboot) can resume
 * exactly where the listener left off instead of falling silent.
 */
@Serializable
private data class PlaybackSnapshot(
    val track: Track,
    val queue: List<Track> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0L,
    val wasPlaying: Boolean = false,
    val savedAt: Long = 0L
)

class MasterWavePlayer(
    private val context: Context,
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    val exoPlayer: ExoPlayer by lazy {
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableAudioTrackPlaybackParams(true)
            setEnableAudioFloatOutput(true)
        }

        val httpDataSourceFactory = OkHttpDataSource.Factory(MasterWaveApp.instance.okHttpClient)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")

        val upstreamDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(upstreamDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 350,
                /* bufferForPlaybackAfterRebufferMs = */ 800
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(10_000, true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            // Streaming needs the network radio awake too, otherwise Wi-Fi power-save
            // throttles the stream as soon as the screen turns off.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(playerListener)
            }
    }

    // --- Session snapshot (survives process death / lock-screen kills) ---
    private val snapshotPrefs by lazy {
        context.getSharedPreferences("masterwave_playback_snapshot", Context.MODE_PRIVATE)
    }
    private val snapshotJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    @Volatile
    private var isAdvancing = false

    private companion object {
        const val KEY_SNAPSHOT = "last_session"
        const val SNAPSHOT_MAX_AGE_MS = 24 * 60 * 60 * 1000L
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

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val audioPrefs by lazy {
        context.getSharedPreferences("masterwave_audio_prefs", Context.MODE_PRIVATE)
    }

    // --- Audio Enhancement & Tuning Suite ---
    private val _dataSaverEnabled = MutableStateFlow(audioPrefs.getBoolean("data_saver_enabled", false))
    val dataSaverEnabled: StateFlow<Boolean> = _dataSaverEnabled.asStateFlow()

    private val _spatialAudioEnabled = MutableStateFlow(audioPrefs.getBoolean("spatial_audio_enabled", false))
    val spatialAudioEnabled: StateFlow<Boolean> = _spatialAudioEnabled.asStateFlow()

    private val _skipSilenceEnabled = MutableStateFlow(audioPrefs.getBoolean("skip_silence_enabled", true))
    val skipSilenceEnabled: StateFlow<Boolean> = _skipSilenceEnabled.asStateFlow()

    private val _gaplessPlaybackEnabled = MutableStateFlow(audioPrefs.getBoolean("gapless_playback_enabled", true))
    val gaplessPlaybackEnabled: StateFlow<Boolean> = _gaplessPlaybackEnabled.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(audioPrefs.getInt("crossfade_seconds", 0))
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _automixEnabled = MutableStateFlow(audioPrefs.getBoolean("automix_enabled", false))
    val automixEnabled: StateFlow<Boolean> = _automixEnabled.asStateFlow()

    private val _stopWhenCleared = MutableStateFlow(audioPrefs.getBoolean("stop_when_app_cleared", false))
    val stopWhenCleared: StateFlow<Boolean> = _stopWhenCleared.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(audioPrefs.getInt("bass_boost_strength", 480))
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    // 5-Band Studio Equalizer (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
    private var equalizer: Equalizer? = null
    private val _equalizerEnabled = MutableStateFlow(audioPrefs.getBoolean("equalizer_enabled", true))
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _equalizerPreset = MutableStateFlow(audioPrefs.getString("equalizer_preset", "Musician Pro Master") ?: "Musician Pro Master")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val _equalizerBands = MutableStateFlow(listOf(6, 4, 1, 3, 4))
    val equalizerBands: StateFlow<List<Int>> = _equalizerBands.asStateFlow()

    val equalizerPresetMap = mapOf(
        "Musician Pro Master" to listOf(6, 4, 1, 3, 4),
        "Apple Music Master" to listOf(6, 3, 1, 3, 4),
        "Harman Target" to listOf(6, 3, 0, 3, 2),
        "AirPods Spatial" to listOf(5, 2, 2, 3, 4),
        "Studio Pure" to listOf(0, 0, 0, 0, 0),
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass Boost" to listOf(8, 5, 2, 1, 1),
        "Vocal Booster" to listOf(-1, 1, 5, 4, 1),
        "Electronic" to listOf(6, 4, -1, 3, 5),
        "Rock" to listOf(6, 3, -1, 4, 5),
        "Pop" to listOf(3, 4, 4, 3, 0),
        "Classical" to listOf(4, 3, 0, 3, 4),
        "Hip Hop" to listOf(7, 5, 0, 2, 3),
        "Acoustic" to listOf(4, 3, 2, 4, 4)
    )

    // MasterWave Apple Music Tier DSP Tuning Pipeline (32-bit float, anti-clipping headroom & spatial audio)
    private val _appleMusicMasterMode = MutableStateFlow(audioPrefs.getBoolean("apple_music_master_mode", true))
    val appleMusicMasterMode: StateFlow<Boolean> = _appleMusicMasterMode.asStateFlow()

    private var isFadingOut = false
    private var trackRetryCount = 0
    private var isPreparingTrack = false
    private var consecutiveErrorCount = 0
    private var lastAdvanceTimeMs = 0L
    private var playJob: Job? = null

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
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            setupAcousticTuning(audioSessionId)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
            saveSnapshot()
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    _isLoading.value = true
                }
                Player.STATE_READY -> {
                    _isLoading.value = false
                    isPreparingTrack = false
                    consecutiveErrorCount = 0
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                Player.STATE_ENDED -> {
                    _isLoading.value = false
                    val duration = exoPlayer.duration
                    val curPos = exoPlayer.currentPosition
                    // Only advance if playback actually reached the genuine end of a track!
                    if (!isPreparingTrack && duration > 5000L && curPos >= (duration - 2500L)) {
                        _currentTrack.value?.let { repository.recommendationManager.eventTracker.onTrackEndedNaturally(it) }
                        playNext(isUserInitiated = false)
                    }
                }
                Player.STATE_IDLE -> {
                    _isLoading.value = false
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _isLoading.value = false
            _isPlaying.value = false
            isPreparingTrack = false
            error.printStackTrace()

            consecutiveErrorCount++
            if (consecutiveErrorCount >= 3) {
                consecutiveErrorCount = 0
                _playbackError.value = "Playback stopped: tracks unavailable"
                return
            }

            val curTrack = _currentTrack.value
            if (curTrack != null && trackRetryCount < 1) {
                trackRetryCount++
                repository.evictStreamCache(curTrack)
                scope.launch {
                    delay(1000L)
                    try {
                        val refreshedUrl = repository.resolveStreamUrl(curTrack)
                        if (!refreshedUrl.isNullOrBlank()) {
                            val artworkUri = curTrack.artworkUrl?.let { url ->
                                if (url.isNotBlank()) resolveMediaUri(url) else null
                            }
                            val mediaMetadata = MediaMetadata.Builder()
                                .setTitle(curTrack.title)
                                .setArtist(curTrack.artist)
                                .setAlbumTitle(curTrack.album)
                                .setArtworkUri(artworkUri)
                                .build()
                            val mediaItem = MediaItem.Builder()
                                .setMediaId(curTrack.id)
                                .setUri(resolveMediaUri(refreshedUrl))
                                .setMediaMetadata(mediaMetadata)
                                .build()
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.prepare()
                            exoPlayer.play()
                            return@launch
                        }
                    } catch (_: Exception) {}
                    playNext(isUserInitiated = false)
                }
            } else {
                trackRetryCount = 0
                scope.launch {
                    delay(1200L)
                    playNext(isUserInitiated = false)
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val curPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                _positionMs.value = curPos
                // Cheap periodic snapshot (about once every 5 s) so a kill never
                // loses more than a few seconds of the listening position.
                if (curPos % 5_000L < 260L) saveSnapshot()
                val curDuration = exoPlayer.duration
                if (curDuration > 0) {
                    _durationMs.value = curDuration
                    repository.recommendationManager.eventTracker.onProgressUpdate(curPos, curDuration)

                    // Crossfade volume fade out near end of track (STATE_ENDED will advance the track naturally)
                    val fadeSec = if (_crossfadeSeconds.value > 0) _crossfadeSeconds.value else if (_automixEnabled.value) 4 else 0
                    val fadeMs = fadeSec * 1000L
                    if (fadeMs > 0 && curDuration > (fadeMs + 10_000L) && curPos > 10_000L && (curDuration - curPos) <= fadeMs && !isFadingOut) {
                        isFadingOut = true
                        launch {
                            try {
                                val steps = 8
                                val stepTime = (fadeMs / steps).coerceAtLeast(20L)
                                for (s in steps downTo 1) {
                                    exoPlayer.volume = (s.toFloat() / steps) * _volume.value
                                    delay(stepTime)
                                }
                            } catch (_: Exception) {}
                        }
                    }
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

    fun playTrack(
        track: Track,
        newQueue: List<Track> = listOf(track),
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true
    ) {
        trackRetryCount = 0
        isAdvancing = false
        isFadingOut = false
        isPreparingTrack = true
        exoPlayer.stop()
        exoPlayer.volume = _volume.value
        _positionMs.value = startPositionMs.coerceAtLeast(0L)
        _durationMs.value = 0L

        playJob?.cancel()
        playJob = scope.launch {
            _isLoading.value = true
            _currentTrack.value = track
            _queue.value = newQueue
            _currentIndex.value = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            repository.recordRecentTrack(track)
            repository.recommendationManager.eventTracker.onTrackStarted(track)

            try {
                // Ensure stream URL is resolved on IO dispatcher so Doze mode / screen off never stalls Main thread
                val streamUrl = withContext(Dispatchers.IO) {
                    repository.resolveStreamUrl(track)
                }
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

                exoPlayer.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
                exoPlayer.prepare()
                if (autoPlay) {
                    val fadeSec = if (_crossfadeSeconds.value > 0) _crossfadeSeconds.value else if (_automixEnabled.value) 4 else 0
                    if (fadeSec > 0 && startPositionMs == 0L) {
                        exoPlayer.volume = 0f
                        exoPlayer.play()
                        launch {
                            try {
                                val steps = 8
                                val stepTime = 150L
                                val targetVol = _volume.value
                                for (s in 1..steps) {
                                    delay(stepTime)
                                    exoPlayer.volume = (s.toFloat() / steps) * targetVol
                                }
                                exoPlayer.volume = targetVol
                            } catch (_: Exception) {}
                        }
                    } else {
                        exoPlayer.volume = _volume.value
                        exoPlayer.play()
                    }
                } else {
                    exoPlayer.pause()
                }
                isFadingOut = false
                saveSnapshot()

                // If newQueue has only 1 track, populate smart recommendations into queue in background
                if (newQueue.size <= 1) {
                    launch(Dispatchers.IO) {
                        try {
                            val smartRecs = repository.recommendationManager.getQueueRecommendations(track, limit = 10)
                            val fallbackPool = (repository.recentTracks.value + repository.favorites.value + repository.localTracks.value).filter { it.id != track.id }
                            val additional = smartRecs.ifEmpty { fallbackPool.take(10) }
                            if (additional.isNotEmpty()) {
                                val fullQueue = listOf(track) + additional
                                withContext(Dispatchers.Main) {
                                    _queue.value = fullQueue
                                    preWarmNextTrack(1, fullQueue)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                } else {
                    // Pre-warm the upcoming track stream URL and queue it directly in ExoPlayer
                    preWarmNextTrack(_currentIndex.value + 1, newQueue)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
                isPreparingTrack = false
                consecutiveErrorCount++
                if (consecutiveErrorCount >= 3) {
                    consecutiveErrorCount = 0
                    _playbackError.value = "Playback stopped: tracks unavailable"
                    return@launch
                }
                delay(1200L)
                playNext(isUserInitiated = false)
            }
        }
    }

    private fun preWarmNextTrack(index: Int, queue: List<Track>) {
        if (index in queue.indices) {
            val upcoming = queue[index]
            scope.launch(Dispatchers.IO) {
                try {
                    repository.resolveStreamUrl(upcoming)
                } catch (_: Exception) {}
            }
        }
    }

    /** Persist the live session so a restarted service can pick it back up. */
    fun saveSnapshot() {
        val track = _currentTrack.value ?: return
        try {
            val snapshot = PlaybackSnapshot(
                track = track,
                queue = _queue.value,
                index = _currentIndex.value,
                positionMs = exoPlayer.currentPosition.coerceAtLeast(_positionMs.value),
                wasPlaying = _isPlaying.value,
                savedAt = System.currentTimeMillis()
            )
            snapshotPrefs.edit()
                .putString(KEY_SNAPSHOT, snapshotJson.encodeToString(snapshot))
                .apply()
        } catch (_: Exception) {
            // A failed snapshot must never interrupt playback.
        }
    }

    fun clearSnapshot() {
        try {
            snapshotPrefs.edit().remove(KEY_SNAPSHOT).apply()
        } catch (_: Exception) {}
    }

    /**
     * Rebuild the last session after the system recreated the service (START_STICKY).
     * Returns true when a session was found and queued for playback.
     */
    fun restoreLastSession(autoResume: Boolean = true): Boolean {
        val raw = try {
            snapshotPrefs.getString(KEY_SNAPSHOT, null)
        } catch (_: Exception) {
            null
        } ?: return false

        val snapshot = try {
            snapshotJson.decodeFromString<PlaybackSnapshot>(raw)
        } catch (_: Exception) {
            return false
        }

        if (System.currentTimeMillis() - snapshot.savedAt > SNAPSHOT_MAX_AGE_MS) return false

        val queue = snapshot.queue.ifEmpty { listOf(snapshot.track) }
        val index = snapshot.index.coerceIn(0, queue.size - 1)
        val track = queue.getOrNull(index) ?: snapshot.track
        playTrack(
            track = track,
            newQueue = queue,
            startPositionMs = snapshot.positionMs,
            autoPlay = snapshot.wasPlaying && autoResume
        )
        return true
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

    /** Explicit play / resume used by sync and remote controls. */
    fun play() {
        if (!exoPlayer.isPlaying) {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _currentTrack.value != null) {
                playTrack(_currentTrack.value!!)
            } else {
                exoPlayer.play()
            }
        }
    }

    fun resume() = play()

    /** Explicit pause used by the sleep timer and other UI affordances. */
    fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    private val aiRecommender = com.masterwave.player.data.recommendation.AiMusicRecommender()

    fun playNext(isUserInitiated: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!isUserInitiated && now - lastAdvanceTimeMs < 1200L) {
            return
        }
        if (isAdvancing) return
        isAdvancing = true
        lastAdvanceTimeMs = now

        val q = _queue.value
        val cur = _currentTrack.value
        cur?.let { repository.recommendationManager.eventTracker.onTrackSkipped(it) }

        // Fast path: the queue already knows the next track. No network, no coroutine,
        // so auto-advance still works instantly with the screen off.
        if (q.isNotEmpty() && _currentIndex.value >= 0 && _currentIndex.value + 1 < q.size) {
            val nextIdx = _currentIndex.value + 1
            isAdvancing = false
            playTrack(q[nextIdx], q)
            return
        }

        // Loop back to beginning when repeat mode is ALL
        if (q.isNotEmpty() && _repeatMode.value == Player.REPEAT_MODE_ALL) {
            isAdvancing = false
            playTrack(q[0], q)
            return
        }

        // Smart Dynamic Continuation powered by the Recommendation Engine.
        scope.launch {
            try {
                val smartRecs = if (cur != null) {
                    withTimeoutOrNull(6_000L) {
                        repository.recommendationManager.getQueueRecommendations(cur, limit = 5)
                    } ?: emptyList()
                } else {
                    emptyList()
                }

                if (smartRecs.isNotEmpty()) {
                    val extendedQueue = q + smartRecs
                    _queue.value = extendedQueue
                    isAdvancing = false
                    playTrack(smartRecs.first(), extendedQueue)
                    return@launch
                }

                // Fallback to the local pool.
                val pool = (repository.recentTracks.value + repository.favorites.value + repository.localTracks.value).filter { it.id != cur?.id }
                val aiTrack = aiRecommender.selectNextTrack(cur, pool, repository.recentTracks.value)
                isAdvancing = false
                when {
                    aiTrack != null -> playTrack(aiTrack)
                    q.isNotEmpty() -> playTrack(q.first(), q)
                    cur != null -> playTrack(cur, listOf(cur))
                }
            } catch (_: Exception) {
                isAdvancing = false
                val fallback = q.ifEmpty { listOfNotNull(cur) }
                if (fallback.isNotEmpty()) playTrack(fallback.first(), fallback)
            } finally {
                isAdvancing = false
            }
        }
    }

    fun skipToNext() = playNext(isUserInitiated = true)

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

    fun addToQueue(track: Track) {
        _queue.value = QueueOperations.add(_queue.value, track)
        if (_currentIndex.value < 0 && _queue.value.isNotEmpty()) {
            _currentIndex.value = 0
        }
        saveSnapshot()
    }

    fun playNext(track: Track) {
        val (updatedQueue, updatedIndex) = QueueOperations.playNext(
            _queue.value,
            _currentIndex.value,
            track
        )
        _queue.value = updatedQueue
        _currentIndex.value = updatedIndex
        saveSnapshot()
    }

    fun saveQueueSnapshot(name: String = "Saved Queue"): Boolean {
        return try {
            val q = _queue.value
            if (q.isEmpty()) return false
            val encoded = snapshotJson.encodeToString(q)
            audioPrefs.edit().putString("queue_snapshot_${name.trim().lowercase().replace(' ', '_')}", encoded).apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun restoreQueueSnapshot(name: String = "Saved Queue"): Boolean {
        return try {
            val key = "queue_snapshot_${name.trim().lowercase().replace(' ', '_')}"
            val encoded = audioPrefs.getString(key, null) ?: return false
            val restored = snapshotJson.decodeFromString<List<Track>>(encoded)
            if (restored.isNotEmpty()) {
                _queue.value = restored
                _currentIndex.value = 0
                _currentTrack.value = restored.first()
                playTrack(restored.first(), restored)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }

    fun removeFromQueue(trackId: String) {
        val (updatedQueue, updatedIndex) = QueueOperations.remove(
            _queue.value,
            _currentIndex.value,
            trackId
        )
        _queue.value = updatedQueue
        _currentIndex.value = updatedIndex
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val (updatedQueue, updatedIndex) = QueueOperations.move(
            _queue.value,
            _currentIndex.value,
            fromIndex,
            toIndex
        )
        _queue.value = updatedQueue
        _currentIndex.value = updatedIndex
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
            android.widget.Toast.makeText(context, "System Equalizer Active", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // MasterWave Studio Acoustic DSP Tuning & Equalizer Engine
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var virtualizer: Virtualizer? = null

    // Kept on permanently in player engine by default (+2.5dB loudness lift, warm punchy bass)
    private val _acousticTuningEnabled = MutableStateFlow(true)
    val acousticTuningEnabled: StateFlow<Boolean> = _acousticTuningEnabled.asStateFlow()

    fun setupAcousticTuning(sessionId: Int) {
        if (sessionId <= 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) return
        try {
            releaseAcousticTuning()
            // 1. Studio Loudness Enhancer (EBU R128 transparent headroom without clipping)
            val targetGain = if (_appleMusicMasterMode.value) 280 else 320
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                setTargetGain(targetGain)
                enabled = _acousticTuningEnabled.value
            }
            // 2. Punchy Warm Bass Acoustic Tuning (tight, non-muddy sub-bass for all phone DACs)
            val bassStrength = _bassBoostStrength.value
            bassBoost = BassBoost(0, sessionId).apply {
                if (strengthSupported) {
                    setStrength(bassStrength.toShort())
                }
                enabled = _acousticTuningEnabled.value && bassStrength > 0
            }
            // 3. Studio Acoustic Soundstage Widening & Spatial Audio (AirPods/Harman binaural stage)
            // Preserves center mono channel (vocals & kick punch) while expanding stereo air
            virtualizer = Virtualizer(0, sessionId).apply {
                val strength = when {
                    _spatialAudioEnabled.value -> 600.toShort()
                    _appleMusicMasterMode.value -> 200.toShort()
                    else -> 100.toShort()
                }
                if (strengthSupported) {
                    setStrength(strength)
                }
                enabled = _acousticTuningEnabled.value || _spatialAudioEnabled.value
            }
            // 4. Studio Graphic Equalizer
            equalizer = Equalizer(0, sessionId).apply {
                enabled = _equalizerEnabled.value
                applyBandsToHardware()
            }
            // 5. Skip silence setting
            exoPlayer.skipSilenceEnabled = _skipSilenceEnabled.value
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassBoostStrength.value = clamped
        audioPrefs.edit().putInt("bass_boost_strength", clamped).apply()
        try {
            bassBoost?.apply {
                if (strengthSupported) {
                    setStrength(clamped.toShort())
                }
                enabled = _acousticTuningEnabled.value && clamped > 0
            }
        } catch (_: Exception) {}
    }

    private fun applyBandsToHardware() {
        try {
            val eq = equalizer ?: return
            val numBands = eq.numberOfBands.toInt().coerceAtMost(5)
            val currentLevels = _equalizerBands.value
            val minEq = eq.bandLevelRange[0]
            val maxEq = eq.bandLevelRange[1]
            for (i in 0 until numBands) {
                val db = currentLevels.getOrElse(i) { 0 }
                val mb = (db * 100).coerceIn(minEq.toInt(), maxEq.toInt()).toShort()
                eq.setBandLevel(i.toShort(), mb)
            }
        } catch (_: Exception) {}
    }

    fun setEqualizerBand(bandIndex: Int, levelDb: Int) {
        val current = _equalizerBands.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = levelDb.coerceIn(-12, 12)
            _equalizerBands.value = current
            _equalizerPreset.value = "Custom"
            applyBandsToHardware()
        }
    }

    fun setEqualizerPreset(presetName: String) {
        val vals = equalizerPresetMap[presetName] ?: return
        _equalizerPreset.value = presetName
        _equalizerBands.value = vals
        audioPrefs.edit().putString("equalizer_preset", presetName).apply()
        applyBandsToHardware()
    }

    fun toggleEqualizer(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        audioPrefs.edit().putBoolean("equalizer_enabled", enabled).apply()
        try {
            equalizer?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun toggleSpatialAudio(enabled: Boolean) {
        _spatialAudioEnabled.value = enabled
        audioPrefs.edit().putBoolean("spatial_audio_enabled", enabled).apply()
        try {
            if (enabled) {
                virtualizer?.setStrength(600.toShort())
                virtualizer?.enabled = true
            } else {
                virtualizer?.setStrength(if (_appleMusicMasterMode.value) 200.toShort() else 100.toShort())
                virtualizer?.enabled = _acousticTuningEnabled.value
            }
        } catch (_: Exception) {}
    }

    fun toggleSkipSilence(enabled: Boolean) {
        _skipSilenceEnabled.value = enabled
        audioPrefs.edit().putBoolean("skip_silence_enabled", enabled).apply()
        try {
            exoPlayer.skipSilenceEnabled = enabled
        } catch (_: Exception) {}
    }

    fun toggleGaplessPlayback(enabled: Boolean) {
        _gaplessPlaybackEnabled.value = enabled
        audioPrefs.edit().putBoolean("gapless_playback_enabled", enabled).apply()
    }

    fun toggleDataSaver(enabled: Boolean) {
        _dataSaverEnabled.value = enabled
        audioPrefs.edit().putBoolean("data_saver_enabled", enabled).apply()
    }

    fun setCrossfadeSeconds(seconds: Int) {
        _crossfadeSeconds.value = seconds
        audioPrefs.edit().putInt("crossfade_seconds", seconds).apply()
    }

    fun toggleAutomix(enabled: Boolean) {
        _automixEnabled.value = enabled
        audioPrefs.edit().putBoolean("automix_enabled", enabled).apply()
    }

    fun toggleStopWhenCleared(enabled: Boolean) {
        _stopWhenCleared.value = enabled
        audioPrefs.edit().putBoolean("stop_when_app_cleared", enabled).apply()
    }

    fun toggleAcousticTuning(enabled: Boolean) {
        _acousticTuningEnabled.value = enabled
        try {
            loudnessEnhancer?.enabled = enabled
            bassBoost?.enabled = enabled && _bassBoostStrength.value > 0
            virtualizer?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun toggleAppleMusicMasterMode(enabled: Boolean) {
        _appleMusicMasterMode.value = enabled
        audioPrefs.edit().putBoolean("apple_music_master_mode", enabled).apply()
        if (enabled) {
            setEqualizerPreset("Musician Pro Master")
            _bassBoostStrength.value = 480
            audioPrefs.edit().putInt("bass_boost_strength", 480).apply()
            _acousticTuningEnabled.value = true
        }
        setupAcousticTuning(getAudioSessionId())
    }

    private fun releaseAcousticTuning() {
        try {
            equalizer?.release()
            equalizer = null
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            bassBoost?.release()
            bassBoost = null
            virtualizer?.release()
            virtualizer = null
        } catch (_: Exception) {}
    }

    fun release() {
        saveSnapshot()
        releaseAcousticTuning()
        stopProgressTracker()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
