package com.masterwave.player.data.webrtc

import android.content.Context
import android.util.Log
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
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.SecureRandom
import java.util.UUID

data class SyncPacket(
    val roomCode: String,
    val hostName: String,
    val track: Track?,
    val positionMs: Long,
    val isPlaying: Boolean,
    val timestamp: Long
)

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
    private val port = 8899

    companion object {
        private const val TAG = "ListenTogetherManager"
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
            val codeBuilder = StringBuilder("MW-")
            for (i in 0 until 4) {
                codeBuilder.append(chars[random.nextInt(chars.length)])
            }
            return codeBuilder.toString()
        }
    }

    private fun loadOrGenerateRoomCode(): String {
        val saved = prefs.getString("room_code", null)
        if (!saved.isNullOrBlank()) return saved
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

    fun startBroadcasting() {
        stopBroadcasting()
        _isBroadcasting.value = true
        broadcastJob = scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                val broadcastAddress = InetAddress.getByName("255.255.255.255")

                while (isActive && _isBroadcasting.value) {
                    val currentTrack = player.currentTrack.value
                    val isPlaying = player.isPlaying.value
                    val pos = player.positionMs.value
                    val now = System.currentTimeMillis()

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

                    // Save local broadcast state for cross-room discovery on device
                    prefs.edit()
                        .putString("room_state_${_currentRoomCode.value}", packetJson.toString())
                        .putLong("room_time_${_currentRoomCode.value}", now)
                        .apply()

                    val bytes = packetJson.toString().toByteArray(Charsets.UTF_8)
                    val datagram = DatagramPacket(bytes, bytes.size, broadcastAddress, port)
                    try {
                        socket.send(datagram)
                    } catch (e: Exception) {
                        Log.w(TAG, "Broadcast send error: ${e.message}")
                    }

                    delay(800)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Broadcast loop error", e)
            } finally {
                socket?.close()
            }
        }
    }

    fun stopBroadcasting() {
        _isBroadcasting.value = false
        broadcastJob?.cancel()
        broadcastJob = null
    }

    fun joinRoom(targetCode: String, onConnected: (Boolean) -> Unit) {
        val cleanCode = targetCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            onConnected(false)
            return
        }

        leaveRoom()
        _connectedRoomCode.value = cleanCode
        _isJoinedAsListener.value = true

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
                    } catch (_: Exception) {
                        // Socket read timeout is expected when waiting
                    }

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

                    // 3. Process Sync Packet if found
                    if (receivedPacket != null) {
                        applyHostSync(receivedPacket)
                    }

                    delay(500)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Listener error", e)
            } finally {
                socket?.close()
            }
        }

        onConnected(true)
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

    private suspend fun applyHostSync(packet: SyncPacket) {
        val now = System.currentTimeMillis()
        val latencyCompensation = (now - packet.timestamp).coerceIn(0, 3000)
        val targetPos = packet.positionMs + if (packet.isPlaying) latencyCompensation else 0

        _hostTrack.value = packet.track
        _isHostPlaying.value = packet.isPlaying

        val currentLocalTrack = player.currentTrack.value
        val isLocalPlaying = player.isPlaying.value
        val currentLocalPos = player.positionMs.value

        val drift = kotlin.math.abs(currentLocalPos - targetPos)
        _syncDriftMs.value = drift

        // Track mismatch: automatically sync to host track
        if (packet.track != null && (currentLocalTrack == null || currentLocalTrack.id != packet.track.id)) {
            player.playTrack(packet.track)
            delay(300)
            player.seekTo(targetPos)
            return
        }

        // Play/Pause sync
        if (packet.isPlaying && !isLocalPlaying) {
            player.resume()
        } else if (!packet.isPlaying && isLocalPlaying) {
            player.pause()
        }

        // Clock drift correction: seek if drift is > 800ms
        if (drift > 800L) {
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
    }
}
