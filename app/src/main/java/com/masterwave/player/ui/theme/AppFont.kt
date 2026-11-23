package com.masterwave.player.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.masterwave.player.R

/**
 * MASTERWAVE TYPOGRAPHY SYSTEM
 * Exclusively powered by the authentic Fucek display typeface across all screens.
 */
val FontFucekFamily = FontFamily(
    Font(R.font.font_fucek, FontWeight.Normal),
    Font(R.font.font_fucek, FontWeight.Medium),
    Font(R.font.font_fucek, FontWeight.SemiBold),
    Font(R.font.font_fucek, FontWeight.Bold),
    Font(R.font.font_fucek, FontWeight.ExtraBold)
)

val FontFueckFamily = FontFucekFamily

enum class AppFont(
    val id: String,
    val displayName: String,
    val previewSubtitle: String,
    val sampleText: String = "Music sounds better with you."
) {
    FUCEK(
        id = "FUCEK",
        displayName = "Fucek (Default)",
        previewSubtitle = "Default Typography • Geometric Android Display",
        sampleText = "Music sounds better with you."
    ),
    SYSTEM_DEFAULT(
        id = "SYSTEM_DEFAULT",
        displayName = "System Default",
        previewSubtitle = "Native Device Font • Matches Phone Settings",
        sampleText = "Music sounds better with you."
    );

    companion object {
        val DEFAULT = FUCEK
        val FUECK = FUCEK

        fun fromId(id: String?): AppFont = when (id) {
            "SYSTEM_DEFAULT", "SYSTEM", "DEFAULT" -> SYSTEM_DEFAULT
            else -> FUCEK
        }
    }
}

fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.FUCEK -> FontFucekFamily
    AppFont.SYSTEM_DEFAULT -> FontFamily.Default
}
