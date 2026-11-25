package com.masterwave.player.data.appearance

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class NowPlayingTemplate(val title: String, val subtitle: String) {
    CLASSIC("Classic", "Balanced artwork, standard playback controls, clear metadata"),
    IMMERSIVE("Immersive", "Large artwork, ambient background, fluid transitions"),
    MINIMAL("Minimal", "Clean typography, discreet artwork, uncluttered layout"),
    COMPACT("Compact", "Smaller artwork, dense metadata, quick actions"),
    MODERN_GLASS("Modern Glass", "Translucent glass control surfaces and reflections")
}

enum class NowPlayingBgStyle(val title: String, val subtitle: String) {
    BLURRED_ARTWORK("Blurred Artwork", "Rich multi-layer blurred album background"),
    SOLID("Solid Canvas", "Clean background matching active app theme"),
    GRADIENT("Album Gradient", "Dual-tone ambient gradient extracted from cover"),
    CINEMATIC("Dark Cinematic", "Deep obsidian backdrop with focused vignette")
}

enum class AppIconStyle(val title: String, val subtitle: String) {
    MINIMAL_OUTLINE("Minimal Outline", "Clean hairline vector stroke"),
    ROUNDED_OUTLINE("Rounded Outline", "Smooth rounded corner outlines"),
    FILLED("Filled", "Bold solid filled icons"),
    DUOTONE("Duotone", "Two-tone tinted secondary accents"),
    SOFT_ROUNDED("Soft Rounded", "Soft organic curves with subtle pills"),
    MONOCHROME("Monochrome", "High-contrast slate grayscale")
}

enum class LibraryLayout(val title: String) {
    LIST("List"),
    GRID("Grid"),
    COMPACT("Compact")
}

enum class AnimationSpeed(val title: String, val multiplier: Float) {
    OFF("Off", 0f),
    SUBTLE("Subtle", 0.5f),
    NORMAL("Normal", 1f),
    EXPRESSIVE("Expressive", 1.25f)
}

enum class TypographyScale(val title: String, val scale: Float) {
    COMPACT("Compact (90%)", 0.90f),
    BALANCED("Balanced (100%)", 1.0f),
    LARGE("Large (110%)", 1.10f)
}

enum class ThemePreset(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val defaultMode: ThemeMode
) {
    MASTERWAVE_SIGNATURE("MasterWave Signature", "Iconic scarlet & warm coral", Color(0xFFFA2D48), Color(0xFFFF6482), ThemeMode.SYSTEM),
    PURE_WHITE("Pure White", "Crisp high-contrast studio white", Color(0xFF101828), Color(0xFF475467), ThemeMode.LIGHT),
    OBSIDIAN("Obsidian", "Pure pitch black AMOLED power saver", Color(0xFFFA2D48), Color(0xFF000000), ThemeMode.AMOLED),
    MINIMAL_SILVER("Minimal Silver", "Slate monochrome & brushed steel", Color(0xFF64748B), Color(0xFF94A3B8), ThemeMode.SYSTEM),
    OCEAN("Ocean", "Electric cyan & marine azure", Color(0xFF0091FF), Color(0xFF00D4B2), ThemeMode.SYSTEM),
    FOREST("Forest", "Cyber emerald & deep moss", Color(0xFF10B981), Color(0xFF00E599), ThemeMode.SYSTEM),
    BURGUNDY("Burgundy", "Rich velvet wine & deep ruby", Color(0xFF881337), Color(0xFFBE123C), ThemeMode.DARK),
    VIOLET("Violet", "Electric neon purple & ultraviolet", Color(0xFF8B5CF6), Color(0xFFA855F7), ThemeMode.SYSTEM),
    AMBER("Amber", "Golden sunrise glow & warm dusk", Color(0xFFF59E0B), Color(0xFFFBBF24), ThemeMode.SYSTEM),
    ROSE("Rose", "Pastel blush & soft magenta", Color(0xFFF43F5E), Color(0xFFFB7185), ThemeMode.LIGHT)
}

@Serializable
data class AppearanceProfileData(
    val profileName: String,
    val themeMode: String = "SYSTEM",
    val themePreset: String = "MASTERWAVE_SIGNATURE",
    val customAccentArgb: Int? = null,
    val nowPlayingTemplate: String = "CLASSIC",
    val nowPlayingBgStyle: String = "BLURRED_ARTWORK",
    val artworkCornerRadiusDp: Int = 20,
    val iconStyle: String = "FILLED",
    val libraryLayout: String = "LIST",
    val animationSpeed: String = "NORMAL",
    val typographyScale: String = "BALANCED",
    val dynamicColorEnabled: Boolean = true
)

object AppearanceStudioManager {
    private const val PREFS_NAME = "masterwave_appearance_studio_v1"

    private val prefs: SharedPreferences by lazy {
        MasterWaveApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    // Active Profile Name
    private val _activeProfileName = MutableStateFlow(
        prefs.getString("active_profile_name", "Clean Daily") ?: "Clean Daily"
    )
    val activeProfileName: StateFlow<String> = _activeProfileName.asStateFlow()

    // 1. Now Playing Template (Locked strictly to IMMERSIVE per user experience specification)
    private val _nowPlayingTemplate = MutableStateFlow(NowPlayingTemplate.IMMERSIVE)
    val nowPlayingTemplate: StateFlow<NowPlayingTemplate> = _nowPlayingTemplate.asStateFlow()

    // 2. Now Playing Background Style
    private val _nowPlayingBgStyle = MutableStateFlow(
        try {
            NowPlayingBgStyle.valueOf(prefs.getString("now_playing_bg_style", NowPlayingBgStyle.BLURRED_ARTWORK.name) ?: NowPlayingBgStyle.BLURRED_ARTWORK.name)
        } catch (_: Exception) {
            NowPlayingBgStyle.BLURRED_ARTWORK
        }
    )
    val nowPlayingBgStyle: StateFlow<NowPlayingBgStyle> = _nowPlayingBgStyle.asStateFlow()

    // 3. Artwork Corner Radius (8dp to 36dp)
    private val _artworkCornerRadiusDp = MutableStateFlow(prefs.getInt("artwork_corner_radius", 20))
    val artworkCornerRadiusDp: StateFlow<Int> = _artworkCornerRadiusDp.asStateFlow()

    // 4. Global Icon Style
    private val _iconStyle = MutableStateFlow(
        try {
            AppIconStyle.valueOf(prefs.getString("global_icon_style", AppIconStyle.FILLED.name) ?: AppIconStyle.FILLED.name)
        } catch (_: Exception) {
            AppIconStyle.FILLED
        }
    )
    val iconStyle: StateFlow<AppIconStyle> = _iconStyle.asStateFlow()

    // 5. Library Layout Style
    private val _libraryLayout = MutableStateFlow(
        try {
            LibraryLayout.valueOf(prefs.getString("library_layout", LibraryLayout.LIST.name) ?: LibraryLayout.LIST.name)
        } catch (_: Exception) {
            LibraryLayout.LIST
        }
    )
    val libraryLayout: StateFlow<LibraryLayout> = _libraryLayout.asStateFlow()

    // 6. Animation Speed
    private val _animationSpeed = MutableStateFlow(
        try {
            AnimationSpeed.valueOf(prefs.getString("animation_speed", AnimationSpeed.NORMAL.name) ?: AnimationSpeed.NORMAL.name)
        } catch (_: Exception) {
            AnimationSpeed.NORMAL
        }
    )
    val animationSpeed: StateFlow<AnimationSpeed> = _animationSpeed.asStateFlow()

    // 7. Typography Scale
    private val _typographyScale = MutableStateFlow(
        try {
            TypographyScale.valueOf(prefs.getString("typography_scale", TypographyScale.BALANCED.name) ?: TypographyScale.BALANCED.name)
        } catch (_: Exception) {
            TypographyScale.BALANCED
        }
    )
    val typographyScale: StateFlow<TypographyScale> = _typographyScale.asStateFlow()

    // 8. Active Theme Preset
    private val _themePreset = MutableStateFlow(
        try {
            ThemePreset.valueOf(prefs.getString("theme_preset", ThemePreset.MASTERWAVE_SIGNATURE.name) ?: ThemePreset.MASTERWAVE_SIGNATURE.name)
        } catch (_: Exception) {
            ThemePreset.MASTERWAVE_SIGNATURE
        }
    )
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    fun setNowPlayingTemplate(template: NowPlayingTemplate) {
        _nowPlayingTemplate.value = NowPlayingTemplate.IMMERSIVE
        prefs.edit().putString("now_playing_template", NowPlayingTemplate.IMMERSIVE.name).apply()
    }

    fun setNowPlayingBgStyle(style: NowPlayingBgStyle) {
        _nowPlayingBgStyle.value = style
        prefs.edit().putString("now_playing_bg_style", style.name).apply()
    }

    fun setArtworkCornerRadius(radiusDp: Int) {
        val clamped = radiusDp.coerceIn(4, 40)
        _artworkCornerRadiusDp.value = clamped
        prefs.edit().putInt("artwork_corner_radius", clamped).apply()
    }

    fun setIconStyle(style: AppIconStyle) {
        _iconStyle.value = style
        prefs.edit().putString("global_icon_style", style.name).apply()
    }

    fun setLibraryLayout(layout: LibraryLayout) {
        _libraryLayout.value = layout
        prefs.edit().putString("library_layout", layout.name).apply()
    }

    fun setAnimationSpeed(speed: AnimationSpeed) {
        _animationSpeed.value = speed
        prefs.edit().putString("animation_speed", speed.name).apply()
    }

    fun setTypographyScale(scale: TypographyScale) {
        _typographyScale.value = scale
        prefs.edit().putString("typography_scale", scale.name).apply()
    }

    fun setThemePreset(preset: ThemePreset) {
        _themePreset.value = preset
        prefs.edit().putString("theme_preset", preset.name).apply()
    }

    fun setActiveProfileName(name: String) {
        _activeProfileName.value = name
        prefs.edit().putString("active_profile_name", name).apply()
    }

    fun exportCurrentProfileJson(): String {
        val data = AppearanceProfileData(
            profileName = _activeProfileName.value,
            themePreset = _themePreset.value.name,
            nowPlayingTemplate = _nowPlayingTemplate.value.name,
            nowPlayingBgStyle = _nowPlayingBgStyle.value.name,
            artworkCornerRadiusDp = _artworkCornerRadiusDp.value,
            iconStyle = _iconStyle.value.name,
            libraryLayout = _libraryLayout.value.name,
            animationSpeed = _animationSpeed.value.name,
            typographyScale = _typographyScale.value.name
        )
        return json.encodeToString(data)
    }

    fun importProfileJson(rawJson: String): Boolean {
        return try {
            val data = json.decodeFromString<AppearanceProfileData>(rawJson)
            setActiveProfileName(data.profileName)
            try { setThemePreset(ThemePreset.valueOf(data.themePreset)) } catch (_: Exception) {}
            try { setNowPlayingTemplate(NowPlayingTemplate.valueOf(data.nowPlayingTemplate)) } catch (_: Exception) {}
            try { setNowPlayingBgStyle(NowPlayingBgStyle.valueOf(data.nowPlayingBgStyle)) } catch (_: Exception) {}
            setArtworkCornerRadius(data.artworkCornerRadiusDp)
            try { setIconStyle(AppIconStyle.valueOf(data.iconStyle)) } catch (_: Exception) {}
            try { setLibraryLayout(LibraryLayout.valueOf(data.libraryLayout)) } catch (_: Exception) {}
            try { setAnimationSpeed(AnimationSpeed.valueOf(data.animationSpeed)) } catch (_: Exception) {}
            try { setTypographyScale(TypographyScale.valueOf(data.typographyScale)) } catch (_: Exception) {}
            true
        } catch (_: Exception) {
            false
        }
    }

    fun resetToDefaults() {
        setNowPlayingTemplate(NowPlayingTemplate.CLASSIC)
        setNowPlayingBgStyle(NowPlayingBgStyle.BLURRED_ARTWORK)
        setArtworkCornerRadius(20)
        setIconStyle(AppIconStyle.FILLED)
        setLibraryLayout(LibraryLayout.LIST)
        setAnimationSpeed(AnimationSpeed.NORMAL)
        setTypographyScale(TypographyScale.BALANCED)
        setThemePreset(ThemePreset.MASTERWAVE_SIGNATURE)
        setActiveProfileName("Clean Daily")
    }
}
