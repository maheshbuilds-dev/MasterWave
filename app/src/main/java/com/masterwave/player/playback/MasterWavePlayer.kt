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
                /* minBufferMs = */ 25_000,
                /* maxBufferMs = */ 70_000,
                /* bufferForPlaybackMs = */ 1_200,
                /* bufferForPlaybackAfterRebufferMs = */ 2_400
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(15_000, true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
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

    private val _skipSilenceEnabled = MutableStateFlow(audioPrefs.getBoolean("skip_silence_enabled", false))
    val skipSilenceEnabled: StateFlow<Boolean> = _skipSilenceEnabled.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(audioPrefs.getInt("crossfade_seconds", 0))
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _automixEnabled = MutableStateFlow(audioPrefs.getBoolean("automix_enabled", false))
    val automixEnabled: StateFlow<Boolean> = _automixEnabled.asStateFlow()

    private val _stopWhenCleared = MutableStateFlow(audioPrefs.getBoolean("stop_when_app_cleared", false))
    val stopWhenCleared: StateFlow<Boolean> = _stopWhenCleared.asStateFlow()

    // 5-Band Studio Equalizer (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
    private var equalizer: Equalizer? = null
    private val _equalizerEnabled = MutableStateFlow(audioPrefs.getBoolean("equalizer_enabled", true))
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _equalizerPreset = MutableStateFlow(audioPrefs.getString("equalizer_preset", "Apple Music Master") ?: "Apple Music Master")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val _equalizerBands = MutableStateFlow(listOf(5, 2, 1, 2, 3))
    val equalizerBands: StateFlow<List<Int>> = _equalizerBands.asStateFlow()

    val equalizerPresetMap = mapOf(
        "Apple Music Master" to listOf(5, 2, 1, 2, 3),
        "Harman Target" to listOf(6, 3, 0, 3, 2),
        "AirPods Spatial" to listOf(4, 2, 2, 2, 4),
        "Studio Pure" to listOf(0, 0, 0, 0, 0),
        "Flat" to listOf(0, 0, 0, 0, 0),
        "Bass Boost" to listOf(7, 5, 2, 0, 0),
        "Vocal Booster" to listOf(-1, 1, 5, 4, 1),
        "Electronic" to listOf(5, 3, -1, 2, 4),
        "Rock" to listOf(5, 2, -2, 3, 5),
        "Pop" to listOf(2, 4, 5, 3, -1),
        "Classical" to listOf(4, 3, -1, 3, 4),
        "Hip Hop" to listOf(6, 5, 0, 2, 3),
        "Acoustic" to listOf(3, 2, 2, 4, 3)
    )

    // MasterWave Apple Music Tier DSP Tuning Pipeline (32-bit float, anti-clipping headroom & spatial audio)
    private val _appleMusicMasterMode = MutableStateFlow(audioPrefs.getBoolean("apple_music_master_mode", true))
    val appleMusicMasterMode: StateFlow<Boolean> = _appleMusicMasterMode.asStateFlow()

    private var isFadingOut = false
    private var trackRetryCount = 0

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

            val curTrack = _currentTrack.value
            if (curTrack != null && trackRetryCount < 2) {
                trackRetryCount++
                scope.launch {
                    delay(800L * trackRetryCount) // Exponential backoff with delay
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
                    // If retry fails:
                    _playbackError.value = "Playback error: ${curTrack.title}. Skipping..."
                    delay(1200L)
                    _playbackError.value = null
                    playNext()
                }
            } else if (curTrack != null) {
                trackRetryCount = 0
                _playbackError.value = "Track unavailable: ${curTrack.title}"
                scope.launch {
                    delay(1200L)
                    _playbackError.value = null
                    playNext()
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
                val curDuration = exoPlayer.duration
                if (curDuration > 0) {
                    _durationMs.value = curDuration
                    repository.recommendationManager.eventTracker.onProgressUpdate(curPos, curDuration)

                    // Crossfade & Automix auto-transition near end of track
                    val fadeSec = if (_crossfadeSeconds.value > 0) _crossfadeSeconds.value else if (_automixEnabled.value) 4 else 0
                    val fadeMs = fadeSec * 1000L
                    if (fadeMs > 0 && curDuration > fadeMs && (curDuration - curPos) <= fadeMs && !isFadingOut) {
                        isFadingOut = true
                        launch {
                            try {
                                val steps = 8
                                val stepTime = (fadeMs / steps).coerceAtLeast(20L)
                                for (s in steps downTo 1) {
                                    exoPlayer.volume = s.toFloat() / steps
                                    delay(stepTime)
                                }
                            } catch (_: Exception) {}
                            playNext()
                            isFadingOut = false
                            exoPlayer.volume = _volume.value
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

    fun playTrack(track: Track, newQueue: List<Track> = listOf(track)) {
        trackRetryCount = 0
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

    fun addToQueue(track: Track) {
        _queue.value = QueueOperations.add(_queue.value, track)
        if (_currentIndex.value < 0 && _queue.value.isNotEmpty()) {
            _currentIndex.value = 0
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
            val targetGain = if (_appleMusicMasterMode.value) 350 else 380
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                setTargetGain(targetGain)
                enabled = _acousticTuningEnabled.value
            }
            // 2. Punchy Warm Bass Acoustic Tuning (tight, non-muddy sub-bass for all phone DACs)
            val bassStrength = if (_appleMusicMasterMode.value) 350 else 380
            bassBoost = BassBoost(0, sessionId).apply {
                if (strengthSupported) {
                    setStrength(bassStrength.toShort())
                }
                enabled = _acousticTuningEnabled.value
            }
            // 3. Studio Acoustic Soundstage Widening & Spatial Audio (AirPods/Harman binaural stage)
            virtualizer = Virtualizer(0, sessionId).apply {
                val strength = if (_spatialAudioEnabled.value || _appleMusicMasterMode.value) 850.toShort() else 180.toShort()
                if (strengthSupported) {
                    setStrength(strength)
                }
                enabled = _acousticTuningEnabled.value || _spatialAudioEnabled.value || _appleMusicMasterMode.value
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
                virtualizer?.setStrength(1000.toShort())
                virtualizer?.enabled = true
            } else {
                virtualizer?.setStrength(180.toShort())
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
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (_: Exception) {}
    }

    fun toggleAppleMusicMasterMode(enabled: Boolean) {
        _appleMusicMasterMode.value = enabled
        audioPrefs.edit().putBoolean("apple_music_master_mode", enabled).apply()
        if (enabled) {
            setEqualizerPreset("Apple Music Master")
            toggleSpatialAudio(true)
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
        releaseAcousticTuning()
        stopProgressTracker()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
