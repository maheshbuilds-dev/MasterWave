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
    IOS(
        id = "ios",
        title = "iOS Glass",
        subtitle = "Glossy squircle with liquid refraction sheen",
        aliasName = "com.masterwave.player.MainActivityIos",
        previewResId = R.drawable.ic_app_icon_ios
    ),
    NOTHING(
        id = "nothing",
        title = "Nothing Matrix",
        subtitle = "Matte industrial with dot-matrix grid & red glyph",
        aliasName = "com.masterwave.player.MainActivityNothing",
        previewResId = R.drawable.ic_app_icon_nothing
    ),
    MATERIAL(
        id = "material",
        title = "Material You",
        subtitle = "Minimalist dual-tone adaptive aesthetic",
        aliasName = "com.masterwave.player.MainActivityMaterial",
        previewResId = R.drawable.ic_app_icon_material
    ),
    CYBER(
        id = "cyber",
        title = "Cyber Neon",
        subtitle = "Electric cyan & magenta neon synthwave glow",
        aliasName = "com.masterwave.player.MainActivityCyber",
        previewResId = R.drawable.ic_app_icon_cyber
    ),
    GOLD(
        id = "gold",
        title = "Obsidian Gold",
        subtitle = "Luxury champagne gold on polished black ceramic",
        aliasName = "com.masterwave.player.MainActivityGold",
        previewResId = R.drawable.ic_app_icon_gold
    );

    companion object {
        fun fromId(id: String?): AppIcon {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
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
}
