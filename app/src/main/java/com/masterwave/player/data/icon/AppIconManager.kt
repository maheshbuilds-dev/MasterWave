package com.masterwave.player.data.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppIcon(
    val id: String,
    val title: String,
    val subtitle: String,
    val aliasName: String,
    val previewResId: Int
) {
    DEFAULT(
        id = "default",
        title = "Default",
        subtitle = "MasterWave signature crimson wave",
        aliasName = "com.masterwave.player.MainActivityDefault",
        previewResId = R.drawable.ic_app_icon_default
    ),
    LIQUID_GLASS(
        id = "liquid_glass",
        title = "Liquid Glass",
        subtitle = "Apple transparent liquid glass refraction",
        aliasName = "com.masterwave.player.MainActivityLiquidGlass",
        previewResId = R.drawable.ic_app_icon_liquid_glass
    ),
    SPOTIFY(
        id = "spotify",
        title = "Spotify Style",
        subtitle = "Electric neon green on matte obsidian",
        aliasName = "com.masterwave.player.MainActivitySpotify",
        previewResId = R.drawable.ic_app_icon_spotify
    ),
    MONOGRAM(
        id = "monogram",
        title = "Monogram",
        subtitle = "Brushed champagne gold luxury emblem",
        aliasName = "com.masterwave.player.MainActivityMonogram",
        previewResId = R.drawable.ic_app_icon_monogram
    ),
    ABSTRACT(
        id = "abstract",
        title = "Abstract",
        subtitle = "Cosmic iridescent wave frequency",
        aliasName = "com.masterwave.player.MainActivityAbstract",
        previewResId = R.drawable.ic_app_icon_abstract
    ),
    APPLE_MUSIC(
        id = "apple_music",
        title = "Apple Music",
        subtitle = "Crisp white glyph on radiant rose gradient",
        aliasName = "com.masterwave.player.MainActivityAppleMusic",
        previewResId = R.drawable.ic_app_icon_apple_music
    );

    companion object {
        fun fromId(id: String?): AppIcon {
            return when (id?.lowercase()) {
                "liquid_glass", "ios" -> LIQUID_GLASS
                "spotify" -> SPOTIFY
                "monogram", "gold" -> MONOGRAM
                "abstract", "cyber" -> ABSTRACT
                "apple_music" -> APPLE_MUSIC
                else -> values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
            }
        }
    }
}

object AppIconManager {
    private const val PREFS_NAME = "masterwave_app_icon_prefs"
    private const val KEY_SELECTED_ICON = "selected_app_icon"

    private val prefs by lazy {
        MasterWaveApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _currentIcon = MutableStateFlow(
        AppIcon.fromId(prefs.getString(KEY_SELECTED_ICON, AppIcon.DEFAULT.id))
    )
    val currentIcon: StateFlow<AppIcon> = _currentIcon.asStateFlow()

    /**
     * Safely switches the launcher icon using Android activity-alias without crashing the app.
     * Enforces that the target alias is enabled BEFORE disabling others.
     */
    fun setAppIcon(context: Context, newIcon: AppIcon): Boolean {
        if (_currentIcon.value == newIcon) return true

        return try {
            val pm = context.packageManager
            val pkg = context.packageName

            // 1. Enable new target alias first (DONT_KILL_APP prevents launcher-triggered process kill)
            val targetComponent = ComponentName(pkg, newIcon.aliasName)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Disable all other aliases safely
            AppIcon.values().filter { it != newIcon }.forEach { other ->
                val otherComponent = ComponentName(pkg, other.aliasName)
                pm.setComponentEnabledSetting(
                    otherComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // 3. Persist selection
            prefs.edit().putString(KEY_SELECTED_ICON, newIcon.id).apply()
            _currentIcon.value = newIcon
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Ensures that at least one valid launcher alias is enabled on the device.
     * Prevents cases where an update or prior state left launcher components disabled.
     */
    fun ensureValidLauncherAlias(context: Context) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            val savedId = prefs.getString(KEY_SELECTED_ICON, AppIcon.DEFAULT.id)
            val current = AppIcon.fromId(savedId)

            val currentComp = ComponentName(pkg, current.aliasName)
            val state = pm.getComponentEnabledSetting(currentComp)
            if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    currentComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            // If current is Default, guarantee Default is enabled
            if (current == AppIcon.DEFAULT) {
                val defaultComp = ComponentName(pkg, AppIcon.DEFAULT.aliasName)
                pm.setComponentEnabledSetting(
                    defaultComp,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
