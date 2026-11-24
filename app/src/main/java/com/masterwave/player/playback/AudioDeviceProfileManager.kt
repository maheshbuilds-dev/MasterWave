package com.masterwave.player.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioDeviceRoute(val displayName: String, val category: String) {
    PHONE_SPEAKER("Phone Speaker", "Built-in Hal"),
    WIRED_HEADPHONES("Wired Headphones", "3.5mm Analog / IEM"),
    BLUETOOTH_HEADPHONES("Bluetooth Headphones", "A2DP HD Wireless"),
    BLUETOOTH_SPEAKER("Bluetooth Speaker", "Wireless Speaker"),
    USB_DAC("USB-C DAC", "High-Res Digital Audio");

    val prefKeyPrefix: String get() = "device_profile_${name.lowercase()}"
}

data class DeviceAudioProfile(
    val route: AudioDeviceRoute,
    val eqPreset: String = "Balanced",
    val bassBoost: Int = 300,
    val spatialAudio: Boolean = false,
    val preampDb: Int = 0
)

class AudioDeviceProfileManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val prefs: SharedPreferences = context.getSharedPreferences("masterwave_audio_profiles", Context.MODE_PRIVATE)

    private val _currentRoute = MutableStateFlow(detectCurrentRoute())
    val currentRoute: StateFlow<AudioDeviceRoute> = _currentRoute.asStateFlow()

    private val _isSpatializerAvailable = MutableStateFlow(checkSpatializerSupport())
    val isSpatializerAvailable: StateFlow<Boolean> = _isSpatializerAvailable.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateRoute()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateRoute()
        }
    }

    init {
        try {
            audioManager?.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {}
        updateRoute()
    }

    fun updateRoute() {
        val detected = detectCurrentRoute()
        _currentRoute.value = detected
        _isSpatializerAvailable.value = checkSpatializerSupport()
    }

    private fun detectCurrentRoute(): AudioDeviceRoute {
        val am = audioManager ?: return AudioDeviceRoute.PHONE_SPEAKER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            // Priority 1: USB DAC
            if (outputs.any { it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET }) {
                return AudioDeviceRoute.USB_DAC
            }
            // Priority 2: Wired Headset/Headphones
            if (outputs.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }) {
                return AudioDeviceRoute.WIRED_HEADPHONES
            }
            // Priority 3: Bluetooth
            val btDevice = outputs.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_HEARING_AID ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }
            if (btDevice != null) {
                return AudioDeviceRoute.BLUETOOTH_HEADPHONES
            }
            if (outputs.any { it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }) {
                return AudioDeviceRoute.BLUETOOTH_SPEAKER
            }
        } else {
            @Suppress("DEPRECATION")
            if (am.isWiredHeadsetOn) return AudioDeviceRoute.WIRED_HEADPHONES
            @Suppress("DEPRECATION")
            if (am.isBluetoothA2dpOn) return AudioDeviceRoute.BLUETOOTH_HEADPHONES
        }
        return AudioDeviceRoute.PHONE_SPEAKER
    }

    private fun checkSpatializerSupport(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return try {
                val spatializer = audioManager?.spatializer
                spatializer?.isAvailable == true
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    fun saveProfile(route: AudioDeviceRoute, eqPreset: String, bassBoost: Int, spatialAudio: Boolean, preampDb: Int) {
        val prefix = route.prefKeyPrefix
        prefs.edit()
            .putString("${prefix}_preset", eqPreset)
            .putInt("${prefix}_bass", bassBoost)
            .putBoolean("${prefix}_spatial", spatialAudio)
            .putInt("${prefix}_preamp", preampDb)
            .apply()
    }

    fun loadProfile(route: AudioDeviceRoute): DeviceAudioProfile {
        val prefix = route.prefKeyPrefix
        val defaultPreset = when (route) {
            AudioDeviceRoute.PHONE_SPEAKER -> "Vocal"
            AudioDeviceRoute.WIRED_HEADPHONES -> "Musician Pro Master"
            AudioDeviceRoute.BLUETOOTH_HEADPHONES -> "Balanced"
            AudioDeviceRoute.BLUETOOTH_SPEAKER -> "Bass Boost"
            AudioDeviceRoute.USB_DAC -> "Studio Pure"
        }
        val defaultBass = when (route) {
            AudioDeviceRoute.PHONE_SPEAKER -> 150
            AudioDeviceRoute.WIRED_HEADPHONES -> 350
            AudioDeviceRoute.BLUETOOTH_HEADPHONES -> 400
            AudioDeviceRoute.BLUETOOTH_SPEAKER -> 500
            AudioDeviceRoute.USB_DAC -> 0
        }
        return DeviceAudioProfile(
            route = route,
            eqPreset = prefs.getString("${prefix}_preset", defaultPreset) ?: defaultPreset,
            bassBoost = prefs.getInt("${prefix}_bass", defaultBass),
            spatialAudio = prefs.getBoolean("${prefix}_spatial", route != AudioDeviceRoute.PHONE_SPEAKER),
            preampDb = prefs.getInt("${prefix}_preamp", 0)
        )
    }

    fun release() {
        try {
            audioManager?.unregisterAudioDeviceCallback(deviceCallback)
        } catch (_: Exception) {}
    }
}
