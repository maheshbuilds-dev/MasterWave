package com.masterwave.player.data.webrtc

import android.content.Context
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Log
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.model.Track
import com.masterwave.player.playback.MasterWavePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.SecureRandom
import kotlin.math.abs

data class SyncPacket(
    val roomCode: String,
    val hostName: String,
    val track: Track?,
    val positionMs: Long,
    val isPlaying: Boolean,
    val timestamp: Long
)

/**
 * Universal Listen Together Manager:
 * Combines high-speed WebSocket cloud synchronization (wss://jam.clashprojects.qd.je/ws)
 * with low-latency local UDP mesh broadcast fallback.
 *
 * Implements anti-oscillation buffering protection with a 3500ms grace period on track changes
 * and a 1800ms drift threshold to ensure flawless, non-stuttering synchronized playback.
 */
class ListenTogetherManager private constructor(
    private val context: Context,
    private val player: MasterWavePlayer
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences("masterwave_listen_together", Context.MODE_PRIVATE)

    private val _currentRoomCode = MutableStateFlow(loadOrGenerateRoomCode())
    val currentRoomCode: StateFlow<String> = _currentRoomCode.asStateFlow()

    private val _isBroadcasting = MutableStateFlow(false)
    val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    private val _isJoinedAsListener = MutableStateFlow(false)
    val isJoinedAsListener: StateFlow<Boolean> = _isJoinedAsListener.asStateFlow()

    private val _connectedRoomCode = MutableStateFlow<String?>(null)
    val connectedRoomCode: StateFlow<String?> = _connectedRoomCode.asStateFlow()

    private val _hostTrack = MutableStateFlow<Track?>(null)
    val hostTrack: StateFlow<Track?> = _hostTrack.asStateFlow()

    private val _isHostPlaying = MutableStateFlow(false)
    val isHostPlaying: StateFlow<Boolean> = _isHostPlaying.asStateFlow()

    private val _syncDriftMs = MutableStateFlow(0L)
    val syncDriftMs: StateFlow<Long> = _syncDriftMs.asStateFlow()

    private var broadcastJob: Job? = null
    private var listenerJob: Job? = null
    private var webSocket: WebSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    // Anti-oscillation guard
    private var quietUntilMs = 0L
    private var lastBroadcastTrackId: String? = null
    private var lastBroadcastIsPlaying: Boolean? = null
    private var lastBroadcastPosMs = 0L

    private val port = 8899

    companion object {
        private const val TAG = "ListenTogetherManager"
        const val WS_JAM_SERVER_URL = "wss://jam.clashprojects.qd.je/ws"
        private const val DRIFT_SEEK_THRESHOLD_MS = 1800L
        private const val TRACK_CHANGE_GRACE_PERIOD_MS = 3500L

        @Volatile
        private var INSTANCE: ListenTogetherManager? = null

        fun getInstance(context: Context, player: MasterWavePlayer): ListenTogetherManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ListenTogetherManager(context.applicationContext, player).also { INSTANCE = it }
            }
        }

        fun generateRandomRoomCode(): String {
            val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
            val random = SecureRandom()
            val codeBuilder = StringBuilder("MW")
            for (i in 0 until 4) {
                codeBuilder.append(chars[random.nextInt(chars.length)])
            }
            return codeBuilder.toString()
        }
    }

    private fun acquireMulticastLock() {
        try {
            if (multicastLock == null) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wifiManager?.createMulticastLock("MasterWaveListenTogether")?.apply {
                    setReferenceCounted(true)
                    acquire()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MulticastLock acquire failed: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
        } catch (e: Exception) {
            Log.w(TAG, "MulticastLock release failed: ${e.message}")
        }
    }

    private fun loadOrGenerateRoomCode(): String {
        val saved = prefs.getString("room_code", null)?.replace("-", "")?.trim()
        if (!saved.isNullOrBlank()) {
            prefs.edit().putString("room_code", saved).apply()
            return saved
        }
        val newCode = generateRandomRoomCode()
        prefs.edit().putString("room_code", newCode).apply()
        return newCode
    }

    fun regenerateRoomCode(): String {
        val newCode = generateRandomRoomCode()
        _currentRoomCode.value = newCode
        prefs.edit().putString("room_code", newCode).apply()
        if (_isBroadcasting.value) {
            startBroadcasting()
        }
        return newCode
    }

    /**
     * Start broadcasting local playback to peers over WebSocket and UDP mesh.
     */
    fun startBroadcasting() {
        stopBroadcasting()
        _isBroadcasting.value = true
        acquireMulticastLock()

        connectHostWebSocket()

        broadcastJob = scope.launch {
            var udpSocket: DatagramSocket? = null
            try {
                udpSocket = DatagramSocket()
                udpSocket.broadcast = true
                val broadcastAddress = InetAddress.getByName("255.255.255.255")

                while (isActive && _isBroadcasting.value) {
                    val currentTrack = player.currentTrack.value
                    val isPlaying = player.isPlaying.value
                    val pos = player.positionMs.value
                    val now = System.currentTimeMillis()

                    val trackChanged = currentTrack?.id != lastBroadcastTrackId
                    val playStateChanged = isPlaying != lastBroadcastIsPlaying
                    val posDrift = abs(pos - lastBroadcastPosMs)

                    // 1. WebSocket sync dispatch
                    if (trackChanged || playStateChanged || posDrift > 2500L) {
                        lastBroadcastTrackId = currentTrack?.id
                        lastBroadcastIsPlaying = isPlaying
                        lastBroadcastPosMs = pos

                        val actionType = if (trackChanged) "change_track" else if (isPlaying) "play" else "pause"
                        sendWebSocketPlaybackAction(actionType, currentTrack, pos, isPlaying, now)
                    }

                    // 2. UDP mesh packet dispatch (local network fallback)
                    val packetJson = JSONObject().apply {
                        put("roomCode", _currentRoomCode.value)
                        put("hostName", "MasterWave Host")
                        put("isPlaying", isPlaying)
                        put("positionMs", pos)
                        put("timestamp", now)
                        if (currentTrack != null) {
                            put("track", JSONObject().apply {
                                put("id", currentTrack.id)
                                put("title", currentTrack.title)
                                put("artist", currentTrack.artist)
                                put("album", currentTrack.album)
                                put("artworkUrl", currentTrack.artworkUrl ?: "")
                                put("durationSeconds", currentTrack.durationSeconds)
                                put("streamUrl", currentTrack.streamUrl ?: "")
                            })
                        }
                    }

                    prefs.edit()
                        .putString("room_state_${_currentRoomCode.value}", packetJson.toString())
                        .putLong("room_time_${_currentRoomCode.value}", now)
                        .apply()

                    val bytes = packetJson.toString().toByteArray(Charsets.UTF_8)
                    val datagram = DatagramPacket(bytes, bytes.size, broadcastAddress, port)
                    try {
                        udpSocket.send(datagram)
                    } catch (e: Exception) {
                        Log.w(TAG, "Broadcast UDP send error: ${e.message}")
                    }

                    delay(1000L)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Broadcast loop error", e)
            } finally {
                udpSocket?.close()
            }
        }
    }

    private fun connectHostWebSocket() {
        try {
            webSocket?.close(1000, "Reconnecting")
            val request = Request.Builder()
                .url(WS_JAM_SERVER_URL)
                .build()

            webSocket = MasterWaveApp.instance.okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "Host WebSocket connected to $WS_JAM_SERVER_URL")
                    val createRoomMsg = JSONObject().apply {
                        put("type", "create_room")
                        put("payload", JSONObject().apply {
                            put("name", "MasterWave Host")
                            put("room_mode", "collaborative")
                        })
                    }
                    ws.send(createRoomMsg.toString())
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val envelope = JSONObject(text)
                        val type = envelope.optString("type")
                        val payload = envelope.optJSONObject("payload") ?: return

                        if (type == "room_created") {
                            val serverRoomCode = payload.optString("room_code")
                            if (serverRoomCode.isNotBlank()) {
                                _currentRoomCode.value = serverRoomCode
                                prefs.edit().putString("room_code", serverRoomCode).apply()
                                Log.d(TAG, "Room created on Jam server: $serverRoomCode")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Host error parsing WS message: ${e.message}")
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Host WebSocket failure: ${t.message}. Retrying in 3s...")
                    scope.launch {
                        delay(3000L)
                        if (_isBroadcasting.value) connectHostWebSocket()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Host WebSocket connect error", e)
        }
    }

    private fun sendWebSocketPlaybackAction(
        action: String,
        track: Track?,
        positionMs: Long,
        isPlaying: Boolean,
        clientTimeMs: Long
    ) {
        val ws = webSocket ?: return
        try {
            val payload = JSONObject().apply {
                put("action", action)
                put("position_ms", positionMs)
                put("is_playing", isPlaying)
                put("client_time_ms", clientTimeMs)
                if (track != null) {
                    put("track", JSONObject().apply {
                        put("id", track.id)
                        put("title", track.title)
                        put("artist", track.artist)
                        put("album", track.album)
                        put("artwork_url", track.artworkUrl ?: "")
                        put("duration_ms", track.durationSeconds * 1000L)
                        put("source", "youtube")
                    })
                }
            }
            val envelope = JSONObject().apply {
                put("type", "playback_action")
                put("payload", payload)
            }
            ws.send(envelope.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send WS playback action: ${e.message}")
        }
    }

    fun stopBroadcasting() {
        _isBroadcasting.value = false
        broadcastJob?.cancel()
        broadcastJob = null
        try {
            webSocket?.close(1000, "Broadcast stopped")
        } catch (_: Exception) {}
        webSocket = null
        releaseMulticastLock()
    }

    /**
     * Join an existing room via room code.
     */
    fun joinRoom(targetCode: String, onConnected: (Boolean) -> Unit) {
        val cleanCode = targetCode.replace("-", "").replace(" ", "").trim().uppercase()
        if (cleanCode.isBlank()) {
            onConnected(false)
            return
        }

        leaveRoom()
        _connectedRoomCode.value = cleanCode
        _isJoinedAsListener.value = true
        acquireMulticastLock()

        connectListenerWebSocket(cleanCode)

        // Local network listener fallback
        listenerJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(port)
                socket.soTimeout = 2000
                val buffer = ByteArray(4096)

                while (isActive && _isJoinedAsListener.value) {
                    var receivedPacket: SyncPacket? = null

                    // 1. Check UDP network packet
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val jsonStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val obj = JSONObject(jsonStr)
                        if (obj.optString("roomCode").equals(cleanCode, ignoreCase = true)) {
                            receivedPacket = parsePacket(obj)
                        }
                    } catch (_: Exception) {}

                    // 2. Check local shared preferences fallback
                    if (receivedPacket == null) {
                        val localState = prefs.getString("room_state_$cleanCode", null)
                        val localTime = prefs.getLong("room_time_$cleanCode", 0L)
                        if (localState != null && System.currentTimeMillis() - localTime < 15000) {
                            try {
                                val obj = JSONObject(localState)
                                receivedPacket = parsePacket(obj)
                            } catch (_: Exception) {}
                        }
                    }

                    if (receivedPacket != null) {
                        applyHostSync(receivedPacket)
                    }

                    delay(500)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Listener UDP loop error", e)
            } finally {
                socket?.close()
            }
        }

        onConnected(true)
    }

    private fun connectListenerWebSocket(roomCode: String) {
        try {
            webSocket?.close(1000, "Rejoining")
            val request = Request.Builder()
                .url(WS_JAM_SERVER_URL)
                .build()

            webSocket = MasterWaveApp.instance.okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "Listener connected to Jam server. Joining room $roomCode...")
                    val joinMsg = JSONObject().apply {
                        put("type", "join_room")
                        put("payload", JSONObject().apply {
                            put("room_code", roomCode)
                            put("name", "MasterWave Listener")
                        })
                    }
                    ws.send(joinMsg.toString())
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val envelope = JSONObject(text)
                        val type = envelope.optString("type")
                        val payload = envelope.optJSONObject("payload") ?: return

                        when (type) {
                            "sync_state" -> {
                                val playback = payload.optJSONObject("playback")
                                if (playback != null) {
                                    handleRemotePlayback(playback)
                                }
                            }
                            "sync_playback" -> {
                                handleRemotePlayback(payload)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error handling listener WS msg: ${e.message}")
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Listener WebSocket error: ${t.message}. Reconnecting in 3s...")
                    scope.launch {
                        delay(3000L)
                        if (_isJoinedAsListener.value && _connectedRoomCode.value == roomCode) {
                            connectListenerWebSocket(roomCode)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Listener WebSocket connect error", e)
        }
    }

    private fun handleRemotePlayback(payload: JSONObject) {
        val isPlaying = payload.optBoolean("is_playing", false)
        val posMs = payload.optLong("position_ms", 0L)
        val serverTimeMs = payload.optLong("server_time_ms", System.currentTimeMillis())

        val trackObj = payload.optJSONObject("track") ?: payload.optJSONObject("current_track")
        var remoteTrack: Track? = null
        if (trackObj != null) {
            val trackId = trackObj.optString("id")
            val title = trackObj.optString("title")
            val artist = trackObj.optString("artist")
            val album = trackObj.optString("album")
            val artworkUrl = trackObj.optString("artwork_url").takeIf { it.isNotBlank() }
                ?: trackObj.optString("artworkUrl").takeIf { it.isNotBlank() }
            val durationMs = trackObj.optLong("duration_ms", 0L)
            val durationSec = if (durationMs > 0) durationMs / 1000L else trackObj.optLong("durationSeconds", 0L)

            remoteTrack = Track(
                id = trackId,
                title = title,
                artist = artist,
                album = album,
                artworkUrl = artworkUrl,
                durationSeconds = durationSec,
                streamUrl = trackObj.optString("streamUrl").takeIf { it.isNotBlank() },
                isLocal = false
            )
        }

        val packet = SyncPacket(
            roomCode = _connectedRoomCode.value.orEmpty(),
            hostName = "Jam Host",
            track = remoteTrack,
            positionMs = posMs,
            isPlaying = isPlaying,
            timestamp = serverTimeMs
        )

        scope.launch(Dispatchers.Main) {
            applyHostSync(packet)
        }
    }

    private fun parsePacket(obj: JSONObject): SyncPacket {
        val roomCode = obj.optString("roomCode")
        val hostName = obj.optString("hostName", "Host")
        val isPlaying = obj.optBoolean("isPlaying", false)
        val pos = obj.optLong("positionMs", 0L)
        val time = obj.optLong("timestamp", System.currentTimeMillis())

        var track: Track? = null
        val trackObj = obj.optJSONObject("track")
        if (trackObj != null) {
            track = Track(
                id = trackObj.optString("id"),
                title = trackObj.optString("title"),
                artist = trackObj.optString("artist"),
                album = trackObj.optString("album"),
                artworkUrl = trackObj.optString("artworkUrl").takeIf { it.isNotBlank() },
                durationSeconds = trackObj.optLong("durationSeconds", 0L),
                streamUrl = trackObj.optString("streamUrl").takeIf { it.isNotBlank() },
                isLocal = false
            )
        }

        return SyncPacket(
            roomCode = roomCode,
            hostName = hostName,
            track = track,
            positionMs = pos,
            isPlaying = isPlaying,
            timestamp = time
        )
    }

    /**
     * Applies remote sync with anti-oscillation buffering protection:
     * - Suppresses seeks during stream resolving or buffering.
     * - Implements a 3500ms grace period after track switches.
     * - Applies soft drift correction only when exceeding DRIFT_SEEK_THRESHOLD_MS (1800ms).
     */
    private suspend fun applyHostSync(packet: SyncPacket) {
        val now = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()

        val latencyCompensation = (now - packet.timestamp).coerceIn(0, 3000)
        val targetPos = packet.positionMs + if (packet.isPlaying) latencyCompensation else 0L

        _hostTrack.value = packet.track
        _isHostPlaying.value = packet.isPlaying

        val currentLocalTrack = player.currentTrack.value
        val isLocalPlaying = player.isPlaying.value
        val currentLocalPos = player.positionMs.value

        val drift = abs(currentLocalPos - targetPos)
        _syncDriftMs.value = drift

        // 1. Track Mismatch: Switch track and enter grace period
        if (packet.track != null && (currentLocalTrack == null || currentLocalTrack.id != packet.track.id)) {
            Log.d(TAG, "Syncing track switch to: ${packet.track.title}")
            quietUntilMs = nowElapsed + TRACK_CHANGE_GRACE_PERIOD_MS
            player.playTrack(packet.track)
            return
        }

        // 2. Protect buffering state - do not seek while resolving or loading audio chunks
        if (player.isLoading.value || nowElapsed < quietUntilMs) {
            return
        }

        // 3. Play/Pause state sync
        if (packet.isPlaying && !isLocalPlaying) {
            player.resume()
        } else if (!packet.isPlaying && isLocalPlaying) {
            player.pause()
        }

        // 4. Clock drift correction: apply only on substantial drift (>1800ms)
        if (drift > DRIFT_SEEK_THRESHOLD_MS) {
            Log.d(TAG, "Correcting drift: ${drift}ms -> seeking to ${targetPos}ms")
            quietUntilMs = nowElapsed + 800L
            player.seekTo(targetPos)
        }
    }

    fun leaveRoom() {
        _isJoinedAsListener.value = false
        _connectedRoomCode.value = null
        _hostTrack.value = null
        _syncDriftMs.value = 0L
        listenerJob?.cancel()
        listenerJob = null
        try {
            webSocket?.close(1000, "Left room")
        } catch (_: Exception) {}
        webSocket = null
        releaseMulticastLock()
    }
}
