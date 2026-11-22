package com.masterwave.player.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.masterwave.player.R

/**
 * MASTERWAVE CURATED TYPOGRAPHY SYSTEM
 * High-craft typefaces engineered for modern audio streaming.
 * Includes authentic Fucek display typeface, Swiss neo-grotesque, monospace metadata, and system default.
 */
enum class AppFont(
    val id: String,
    val displayName: String,
    val previewSubtitle: String,
    val category: String,
    val sampleText: String = "Fear is the enemy of innovation."
) {
    FUCEK(
        id = "FUCEK",
        displayName = "Fucek",
        previewSubtitle = "Signature Clean Geometric Display • Modern Android",
        category = "Display",
        sampleText = "Fear is the enemy of innovation."
    ),
    PLUS_JAKARTA(
        id = "PLUS_JAKARTA",
        displayName = "Plus Jakarta Sans",
        previewSubtitle = "Contemporary Neo-Grotesque • International Standard",
        category = "Sans-Serif",
        sampleText = "Fear is the enemy of innovation."
    ),
    OUTFIT(
        id = "OUTFIT",
        displayName = "Outfit",
        previewSubtitle = "Balanced Geometric • Pure Apple Aesthetics",
        category = "Sans-Serif",
        sampleText = "Fear is the enemy of innovation."
    ),
    INTER(
        id = "INTER",
        displayName = "Inter",
        previewSubtitle = "High-Precision • Ultra-Legible Interface Scale",
        category = "Sans-Serif",
        sampleText = "Fear is the enemy of innovation."
    ),
    SYNE(
        id = "SYNE",
        displayName = "Syne",
        previewSubtitle = "Avant-Garde • Expressive Sound Culture",
        category = "Artistic",
        sampleText = "Fear is the enemy of innovation."
    ),
    SPACE_GROTESK(
        id = "SPACE_GROTESK",
        displayName = "Space Grotesk",
        previewSubtitle = "Futuristic Tech • Studio Monospace Roots",
        category = "Geometric",
        sampleText = "Fear is the enemy of innovation."
    ),
    JETBRAINS_MONO(
        id = "JETBRAINS_MONO",
        displayName = "JetBrains Mono",
        previewSubtitle = "Technical Code • Audio Metadata & Timing",
        category = "Monospace",
        sampleText = "1234567890!@#%&*()_+-="
    ),
    SYSTEM(
        id = "SYSTEM",
        displayName = "System Default",
        previewSubtitle = "Device OS Native Font",
        category = "System",
        sampleText = "Fear is the enemy of innovation."
    );

    companion object {
        val DEFAULT = FUCEK
        val FUECK = FUCEK // Alias for backwards compatibility

        fun fromId(id: String?): AppFont = when (id) {
            "FUCEK", "FUECK" -> FUCEK
            "PLUS_JAKARTA" -> PLUS_JAKARTA
            "OUTFIT" -> OUTFIT
            "INTER" -> INTER
            "SYNE" -> SYNE
            "SPACE_GROTESK" -> SPACE_GROTESK
            "JETBRAINS_MONO" -> JETBRAINS_MONO
            "SYSTEM" -> SYSTEM
            else -> DEFAULT
        }
    }
}

val FontFucekFamily = FontFamily(
    Font(R.font.font_fucek, FontWeight.Normal),
    Font(R.font.font_fucek, FontWeight.Medium),
    Font(R.font.font_fucek, FontWeight.SemiBold),
    Font(R.font.font_fucek, FontWeight.Bold),
    Font(R.font.font_fucek, FontWeight.ExtraBold)
)

val FontFueckFamily = FontFucekFamily

val FontPlusJakartaFamily = FontFamily(
    Font(R.font.font_plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.font_plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.font_plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.font_plus_jakarta_sans, FontWeight.Bold)
)

val FontOutfitFamily = FontFamily(
    Font(R.font.font_outfit, FontWeight.Normal),
    Font(R.font.font_outfit, FontWeight.Medium),
    Font(R.font.font_outfit, FontWeight.SemiBold),
    Font(R.font.font_outfit, FontWeight.Bold)
)

val FontInterFamily = FontFamily(
    Font(R.font.font_inter, FontWeight.Normal),
    Font(R.font.font_inter, FontWeight.Medium),
    Font(R.font.font_inter, FontWeight.SemiBold),
    Font(R.font.font_inter, FontWeight.Bold)
)

val FontSyneFamily = FontFamily(
    Font(R.font.font_syne, FontWeight.Normal),
    Font(R.font.font_syne, FontWeight.Medium),
    Font(R.font.font_syne, FontWeight.SemiBold),
    Font(R.font.font_syne, FontWeight.Bold)
)

val FontSpaceGroteskFamily = FontFamily(
    Font(R.font.font_space_grotesk, FontWeight.Normal),
    Font(R.font.font_space_grotesk, FontWeight.Medium),
    Font(R.font.font_space_grotesk, FontWeight.SemiBold),
    Font(R.font.font_space_grotesk, FontWeight.Bold)
)

val FontJetBrainsMonoFamily = FontFamily(
    Font(R.font.font_jetbrains_mono, FontWeight.Normal),
    Font(R.font.font_jetbrains_mono, FontWeight.Medium),
    Font(R.font.font_jetbrains_mono, FontWeight.Bold)
)

fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.FUCEK -> FontFucekFamily
    AppFont.PLUS_JAKARTA -> FontPlusJakartaFamily
    AppFont.OUTFIT -> FontOutfitFamily
    AppFont.INTER -> FontInterFamily
    AppFont.SYNE -> FontSyneFamily
    AppFont.SPACE_GROTESK -> FontSpaceGroteskFamily
    AppFont.JETBRAINS_MONO -> FontJetBrainsMonoFamily
    AppFont.SYSTEM -> FontFamily.SansSerif
}
