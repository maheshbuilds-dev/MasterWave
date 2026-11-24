package com.masterwave.player.data.listentogether

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages user display name registration and identity for MasterWave Listen Together.
 * Registration requires only a display name — zero passwords, phone numbers, or emails collected.
 */
class UserIdentityManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _displayName = MutableStateFlow<String?>(prefs.getString(KEY_DISPLAY_NAME, null)?.takeIf { it.isNotBlank() })
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private val _isRegistered = MutableStateFlow(!_displayName.value.isNullOrBlank())
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    val userId: String by lazy {
        val existing = prefs.getString(KEY_USER_ID, null)?.trim()
        if (!existing.isNullOrBlank()) {
            existing
        } else {
            val newId = "usr_" + UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString(KEY_USER_ID, newId).apply()
            newId
        }
    }

    fun setDisplayName(rawName: String): Boolean {
        val cleaned = rawName.trim()
        if (cleaned.length < 2 || cleaned.length > 30) {
            return false
        }
        prefs.edit().putString(KEY_DISPLAY_NAME, cleaned).apply()
        _displayName.value = cleaned
        _isRegistered.value = true
        return true
    }

    fun getDisplayNameOrFallback(): String {
        return _displayName.value?.ifBlank { null } ?: "Listener ${userId.takeLast(4)}"
    }

    companion object {
        private const val PREFS_NAME = "masterwave_user_identity"
        private const val KEY_DISPLAY_NAME = "user_display_name"
        private const val KEY_USER_ID = "user_unique_id"

        @Volatile
        private var INSTANCE: UserIdentityManager? = null

        fun getInstance(context: Context): UserIdentityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserIdentityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
