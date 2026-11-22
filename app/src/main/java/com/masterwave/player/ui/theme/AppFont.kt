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
    val sampleText: String = "Fear is the enemy of innovation."
) {
    FUCEK(
        id = "FUCEK",
        displayName = "Fucek",
        previewSubtitle = "Default Typography • Geometric Android Display",
        sampleText = "Fear is the enemy of innovation."
    );

    companion object {
        val DEFAULT = FUCEK
        val FUECK = FUCEK

        fun fromId(id: String?): AppFont = FUCEK
    }
}

fun AppFont.toFontFamily(): FontFamily = FontFucekFamily
