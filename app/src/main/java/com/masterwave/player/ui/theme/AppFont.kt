package com.masterwave.player.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.masterwave.player.R

/**
 * MASTERWAVE CURATED TYPOGRAPHY SYSTEM
 * High-craft typefaces engineered for modern audio streaming.
 * Includes geometric display, Swiss neo-grotesque, monospace metadata, and system default.
 */
enum class AppFont(
    val id: String,
    val displayName: String,
    val previewSubtitle: String,
    val category: String,
    val sampleText: String = "Aa Modern Sound • 120 BPM"
) {
    FUECK(
        id = "FUECK",
        displayName = "Fueck",
        previewSubtitle = "Modern Geometric Display • Swiss Architecture",
        category = "Display"
    ),
    PLUS_JAKARTA(
        id = "PLUS_JAKARTA",
        displayName = "Plus Jakarta Sans",
        previewSubtitle = "Contemporary Neo-Grotesque • International Standard",
        category = "Sans-Serif"
    ),
    OUTFIT(
        id = "OUTFIT",
        displayName = "Outfit",
        previewSubtitle = "Balanced Geometric • Pure Apple Aesthetics",
        category = "Sans-Serif"
    ),
    INTER(
        id = "INTER",
        displayName = "Inter",
        previewSubtitle = "High-Precision • Ultra-Legible Interface Scale",
        category = "Sans-Serif"
    ),
    SYNE(
        id = "SYNE",
        displayName = "Syne",
        previewSubtitle = "Avant-Garde • Expressive Sound Culture",
        category = "Artistic"
    ),
    SPACE_GROTESK(
        id = "SPACE_GROTESK",
        displayName = "Space Grotesk",
        previewSubtitle = "Futuristic Tech • Studio Monospace Roots",
        category = "Geometric"
    ),
    JETBRAINS_MONO(
        id = "JETBRAINS_MONO",
        displayName = "JetBrains Mono",
        previewSubtitle = "Technical Code • Audio Metadata & Timing",
        category = "Monospace"
    ),
    SYSTEM(
        id = "SYSTEM",
        displayName = "System Default",
        previewSubtitle = "Device OS Native Font",
        category = "System"
    );

    companion object {
        val DEFAULT = FUECK

        fun fromId(id: String?): AppFont = when (id) {
            "FUECK" -> FUECK
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

val FontFueckFamily = FontFamily(
    Font(R.font.font_fueck, FontWeight.Normal),
    Font(R.font.font_fueck, FontWeight.Medium),
    Font(R.font.font_fueck, FontWeight.SemiBold),
    Font(R.font.font_fueck, FontWeight.Bold),
    Font(R.font.font_fueck, FontWeight.ExtraBold)
)

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
    AppFont.FUECK -> FontFueckFamily
    AppFont.PLUS_JAKARTA -> FontPlusJakartaFamily
    AppFont.OUTFIT -> FontOutfitFamily
    AppFont.INTER -> FontInterFamily
    AppFont.SYNE -> FontSyneFamily
    AppFont.SPACE_GROTESK -> FontSpaceGroteskFamily
    AppFont.JETBRAINS_MONO -> FontJetBrainsMonoFamily
    AppFont.SYSTEM -> FontFamily.SansSerif
}
