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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
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

data class PeerListener(
    val id: String,
    val name: String,
    val driftMs: Long,
    val lastSeenMs: Long
)

data class LiveReaction(
    val id: String,
    val emoji: String,
    val senderName: String,
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

    private val _connectedPeers = MutableStateFlow<List<PeerListener>>(emptyList())
    val connectedPeers: StateFlow<List<PeerListener>> = _connectedPeers.asStateFlow()

    private val _liveReactions = MutableStateFlow<List<LiveReaction>>(emptyList())
    val liveReactions: StateFlow<List<LiveReaction>> = _liveReactions.asStateFlow()

    private val _discoveredNearbyRooms = MutableStateFlow<List<String>>(emptyList())
    val discoveredNearbyRooms: StateFlow<List<String>> = _discoveredNearbyRooms.asStateFlow()

    val deviceId: String by lazy {
        val existing = prefs.getString("unique_device_sync_id", null)?.trim()
        if (!existing.isNullOrBlank()) {
            existing
        } else {
            val newId = SecureRandom().nextInt(900000 + 100000).toString()
            prefs.edit().putString("unique_device_sync_id", newId).apply()
            newId
        }
    }

    private var broadcastJob: Job? = null
    private var listenerJob: Job? = null
    private var webSocket: WebSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    // Anti-oscillation guard
    private var quietUntilMs = 0L
    private var lastBroadcastTrackId: String? = null
    private var lastBroadcastIsPlaying: Boolean? = null
    private var lastBroadcastPosMs = 0L

    private val broadcastPort = 8899 // Host -> Listeners sync clock
    private val feedbackPort = 8898  // Listeners -> Host feedback/presence

    companion object {
        private const val TAG = "ListenTogetherManager"
        const val RELAY_HTTP_BASE = "https://ntfy.sh"
        const val RELAY_WS_BASE = "wss://ntfy.sh"
        private const val DRIFT_SEEK_THRESHOLD_MS = 1800L
        private const val TRACK_CHANGE_GRACE_PERIOD_MS = 3500L

        fun getRoomTopic(code: String): String =
            "mw_jam_" + code.replace("-", "").replace(" ", "").trim().lowercase()

        fun getFeedbackTopic(code: String): String =
            getRoomTopic(code) + "_fb"

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
        // Enforce user requirement: Only ONE permanent code for ONE user.
        // Do not change or refresh it. New user gets a new unique code once and keeps it for lifetime.
        val existing = prefs.getString("persistent_user_room_code", null)?.trim()
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val legacy = prefs.getString("room_code", null)?.replace("-", "")?.trim()
        val finalCode = if (!legacy.isNullOrBlank()) legacy else generateRandomRoomCode()
        prefs.edit()
            .putString("persistent_user_room_code", finalCode)
            .putString("room_code", finalCode)
            .apply()
        return finalCode
    }

    fun regenerateRoomCode(): String {
        // Enforce invariant: Always preserve the user's single permanent room code (no refresh)
        val permanentCode = loadOrGenerateRoomCode()
        _currentRoomCode.value = permanentCode
        return permanentCode
    }

    fun registerOrUpdatePeer(id: String, name: String, driftMs: Long) {
        if (id == deviceId) return // Never add self as peer
        val now = System.currentTimeMillis()
        val list = _connectedPeers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(name = name, driftMs = driftMs, lastSeenMs = now)
        } else {
            list.add(PeerListener(id = id, name = name, driftMs = driftMs, lastSeenMs = now))
        }
        _connectedPeers.value = list
    }

    fun removePeer(id: String) {
        _connectedPeers.value = _connectedPeers.value.filter { it.id != id }
    }

    fun pruneStalePeers() {
        val now = System.currentTimeMillis()
        _connectedPeers.value = _connectedPeers.value.filter { now - it.lastSeenMs < 10000L }
    }

    fun addLiveReaction(emoji: String, senderName: String) {
        val rx = LiveReaction(
            id = java.util.UUID.randomUUID().toString(),
            emoji = emoji,
            senderName = senderName,
            timestamp = System.currentTimeMillis()
        )
        val list = _liveReactions.value.takeLast(19).toMutableList()
        list.add(rx)
        _liveReactions.value = list
    }

    private fun publishRelayMessage(topic: String, envelope: JSONObject) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = "$RELAY_HTTP_BASE/$topic"
                val body = envelope.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val req = Request.Builder().url(url).post(body).build()
                MasterWaveApp.instance.okHttpClient.newCall(req).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "Relay publish error: ${e.message}")
            }
        }
    }

    fun sendReaction(emoji: String) {
        if (emoji.isBlank()) return
        val senderName = if (_isBroadcasting.value) "You (Host)" else "You"
        addLiveReaction(emoji, senderName)

        val activeRoom = if (_isBroadcasting.value) _currentRoomCode.value else _connectedRoomCode.value ?: return

        // 1. Send via Relay Cloud HTTP pub/sub
        try {
            val payload = JSONObject().apply {
                put("room_code", activeRoom)
                put("emoji", emoji)
                put("sender_name", senderName)
                put("sender_id", deviceId)
            }
            val env = JSONObject().apply {
                put("type", "reaction")
                put("payload", payload)
            }
            val targetTopic = if (_isBroadcasting.value) getRoomTopic(activeRoom) else getFeedbackTopic(activeRoom)
            publishRelayMessage(targetTopic, env)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send relay reaction: ${e.message}")
        }

        // 2. Broadcast via UDP mesh
        scope.launch(Dispatchers.IO) {
            try {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val packetJson = JSONObject().apply {
                        put("type", "reaction")
                        put("roomCode", activeRoom)
                        put("emoji", emoji)
                        put("senderName", senderName)
                        put("senderId", deviceId)
                        put("timestamp", System.currentTimeMillis())
                    }
                    val bytes = packetJson.toString().toByteArray(Charsets.UTF_8)
                    val targetAddr = InetAddress.getByName("255.255.255.255")
                    socket.send(DatagramPacket(bytes, bytes.size, targetAddr, broadcastPort))
                    socket.send(DatagramPacket(bytes, bytes.size, targetAddr, feedbackPort))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send UDP reaction: ${e.message}")
            }
        }
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
            var receiverSocket: DatagramSocket? = null
            try {
                udpSocket = DatagramSocket()
                udpSocket.broadcast = true
                val broadcastAddress = InetAddress.getByName("255.255.255.255")

                // Host UDP receiver for listener pings and reactions
                launch(Dispatchers.IO) {
                    try {
                        receiverSocket = DatagramSocket(null).apply {
                            reuseAddress = true
                            bind(InetSocketAddress(feedbackPort))
                            soTimeout = 1200
                        }
                        val buffer = ByteArray(4096)
                        while (isActive && _isBroadcasting.value) {
                            try {
                                val packet = DatagramPacket(buffer, buffer.size)
                                receiverSocket?.receive(packet)
                                val jsonStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                                val obj = JSONObject(jsonStr)
                                val pType = obj.optString("type")
                                val pRoom = obj.optString("roomCode")
                                val senderId = obj.optString("senderId")

                                if (pRoom.equals(_currentRoomCode.value, ignoreCase = true) && senderId != deviceId) {
                                    if (pType == "listener_ping") {
                                        val pName = obj.optString("senderName", "Listener ${senderId.take(3)}")
                                        val drift = obj.optLong("driftMs", 0L)
                                        registerOrUpdatePeer(senderId, pName, drift)
                                    } else if (pType == "reaction") {
                                        val emoji = obj.optString("emoji")
                                        val senderName = obj.optString("senderName", "Listener")
                                        if (emoji.isNotBlank()) {
                                            addLiveReaction(emoji, senderName)
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Host receiver socket run error: ${e.message}")
                    } finally {
                        try { receiverSocket?.close() } catch (_: Exception) {}
                    }
                }

                while (isActive && _isBroadcasting.value) {
                    pruneStalePeers()

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
                        put("type", "sync_packet")
                        put("roomCode", _currentRoomCode.value)
                        put("senderId", deviceId)
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
                    val datagram = DatagramPacket(bytes, bytes.size, broadcastAddress, broadcastPort)
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
                receiverSocket?.close()
            }
        }
    }

    private fun connectHostWebSocket() {
        try {
            webSocket?.close(1000, "Reconnecting")
            val feedbackTopic = getFeedbackTopic(_currentRoomCode.value)
            val request = Request.Builder()
                .url("$RELAY_WS_BASE/$feedbackTopic/ws")
                .build()

            webSocket = MasterWaveApp.instance.okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "Host connected to Relay topic $feedbackTopic")
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val outer = JSONObject(text)
                        if (outer.optString("event") != "message") return
                        val msgStr = outer.optString("message")
                        if (msgStr.isBlank()) return
                        val envelope = JSONObject(msgStr)
                        val type = envelope.optString("type")
                        val payload = envelope.optJSONObject("payload") ?: return

                        when (type) {
                            "reaction" -> {
                                val emoji = payload.optString("emoji")
                                val sender = payload.optString("sender_name", "Listener")
                                val senderId = payload.optString("sender_id")
                                if (emoji.isNotBlank() && senderId != deviceId) {
                                    addLiveReaction(emoji, sender)
                                }
                            }
                            "listener_ping", "peer_joined", "peer_ping" -> {
                                val peerId = payload.optString("user_id").ifBlank { payload.optString("sender_id") }
                                val peerName = payload.optString("name").ifBlank { payload.optString("sender_name", "Listener") }
                                val drift = payload.optLong("drift_ms", 0L)
                                if (peerId.isNotBlank() && peerId != deviceId) {
                                    registerOrUpdatePeer(peerId, peerName, drift)
                                }
                            }
                            "peer_left" -> {
                                val peerId = payload.optString("user_id").ifBlank { payload.optString("sender_id") }
                                if (peerId.isNotBlank()) {
                                    removePeer(peerId)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Host error parsing relay message: ${e.message}")
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Host relay WebSocket failure: ${t.message}. Retrying in 3s...")
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
            val roomTopic = getRoomTopic(_currentRoomCode.value)
            publishRelayMessage(roomTopic, envelope)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send relay playback action: ${e.message}")
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
        _connectedPeers.value = emptyList()
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

        // Local network listener fallback & mesh radar discovery
        listenerJob = scope.launch {
            var socket: DatagramSocket? = null
            var sendSocket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(broadcastPort))
                    soTimeout = 1500
                }
                sendSocket = DatagramSocket().apply { broadcast = true }
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val buffer = ByteArray(4096)
                var lastPingSentTime = 0L

                while (isActive && _isJoinedAsListener.value) {
                    var receivedPacket: SyncPacket? = null
                    val now = System.currentTimeMillis()

                    // Send periodic listener ping to notify host
                    if (now - lastPingSentTime > 2500L) {
                        lastPingSentTime = now
                        try {
                            val pingJson = JSONObject().apply {
                                put("type", "listener_ping")
                                put("roomCode", cleanCode)
                                put("senderId", deviceId)
                                put("senderName", "Listener ${deviceId.take(3)}")
                                put("driftMs", _syncDriftMs.value)
                            }
                            val env = JSONObject().apply {
                                put("type", "listener_ping")
                                put("payload", pingJson)
                            }
                            publishRelayMessage(getFeedbackTopic(cleanCode), env)
                            val pingBytes = pingJson.toString().toByteArray(Charsets.UTF_8)
                            val pingPacket = DatagramPacket(pingBytes, pingBytes.size, broadcastAddress, feedbackPort)
                            sendSocket?.send(pingPacket)
                        } catch (_: Exception) {}
                    }

                    // 1. Check UDP network packet
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val jsonStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val obj = JSONObject(jsonStr)
                        val pType = obj.optString("type")
                        val pRoom = obj.optString("roomCode").trim().uppercase()
                        val pSender = obj.optString("senderId")

                        // Discover nearby rooms (excluding self permanent room and currently connected room)
                        if (pRoom.isNotBlank() && pRoom != _currentRoomCode.value && pRoom != cleanCode && pSender != deviceId) {
                            if (!_discoveredNearbyRooms.value.contains(pRoom)) {
                                _discoveredNearbyRooms.value = _discoveredNearbyRooms.value + pRoom
                            }
                        }

                        if (pRoom.equals(cleanCode, ignoreCase = true)) {
                            if (pType == "reaction" && pSender != deviceId) {
                                val emoji = obj.optString("emoji")
                                val senderName = obj.optString("senderName", "Host")
                                if (emoji.isNotBlank()) {
                                    addLiveReaction(emoji, senderName)
                                }
                            } else if (pType == "sync_packet" || !obj.has("type")) {
                                receivedPacket = parsePacket(obj)
                            }
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

                    delay(400)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Listener UDP loop error", e)
            } finally {
                socket?.close()
                sendSocket?.close()
            }
        }

        onConnected(true)
    }

    private fun connectListenerWebSocket(roomCode: String) {
        val topic = getRoomTopic(roomCode)
        // 1. Immediate HTTP Fast Poll: Retrieve current playing state within <150ms
        scope.launch(Dispatchers.IO) {
            try {
                val pollUrl = "$RELAY_HTTP_BASE/$topic/json?poll=1&since=all"
                val req = Request.Builder().url(pollUrl).build()
                val res = MasterWaveApp.instance.okHttpClient.newCall(req).execute()
                val body = res.body?.string()
                res.close()
                if (!body.isNullOrBlank()) {
                    val lines = body.lines().filter { it.isNotBlank() }
                    for (line in lines.reversed()) {
                        try {
                            val obj = JSONObject(line)
                            if (obj.optString("event") == "message") {
                                val msgStr = obj.optString("message")
                                if (msgStr.isNotBlank()) {
                                    val env = JSONObject(msgStr)
                                    val p = env.optJSONObject("payload")
                                    if (p != null) {
                                        handleRemotePlayback(p)
                                        break
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Listener initial relay poll error: ${e.message}")
            }
        }

        // 2. Persistent Live WebSocket via Relay
        try {
            webSocket?.close(1000, "Rejoining")
            val request = Request.Builder()
                .url("$RELAY_WS_BASE/$topic/ws")
                .build()

            webSocket = MasterWaveApp.instance.okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "Listener connected to Relay topic $topic")
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val outer = JSONObject(text)
                        if (outer.optString("event") != "message") return
                        val msgStr = outer.optString("message")
                        if (msgStr.isBlank()) return
                        val envelope = JSONObject(msgStr)
                        val type = envelope.optString("type")
                        val payload = envelope.optJSONObject("payload") ?: return

                        when (type) {
                            "reaction" -> {
                                val emoji = payload.optString("emoji")
                                val sender = payload.optString("sender_name", "Host")
                                val senderId = payload.optString("sender_id")
                                if (emoji.isNotBlank() && senderId != deviceId) {
                                    addLiveReaction(emoji, sender)
                                }
                            }
                            "playback_action", "sync_packet", "sync_playback", "sync_state" -> {
                                handleRemotePlayback(payload)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error handling listener relay WS msg: ${e.message}")
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Listener relay WebSocket error: ${t.message}. Reconnecting in 3s...")
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
