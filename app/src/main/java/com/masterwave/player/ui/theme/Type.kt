package com.masterwave.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MASTERWAVE MUSIC DESIGN SYSTEM TYPOGRAPHY
 * High-precision typographic scale engineered for high-end audio streaming.
 * Features balanced line-heights, disciplined negative tracking on headlines,
 * and high-legibility body styles.
 */
var DisplayFontFamily: FontFamily = FontFueckFamily
val MonoFontFamily: FontFamily = FontJetBrainsMonoFamily

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ==========================================
// SWISS EDITORIAL TYPOGRAPHY TOKENS
// International Typographic Neo-Grotesque & Monospaced Metadata Architecture
// Set color to Color.Unspecified so Compose's LocalContentColor or MaterialTheme handles theme contrast dynamically
// ==========================================
val SwissDisplayLarge = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 38.sp,
    lineHeight = 44.sp,
    letterSpacing = (-0.8).sp,
    color = Color.Unspecified
)

val SwissDisplayMedium = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.6).sp,
    color = Color.Unspecified
)

val SwissHeadline = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.4).sp,
    color = Color.Unspecified
)

val SwissSectionEyebrow = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 1.3.sp,
    color = Color.Unspecified
)

val SwissSectionHeader = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 21.sp,
    lineHeight = 27.sp,
    letterSpacing = (-0.3).sp,
    color = Color.Unspecified
)

val SwissMono = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.5.sp,
    color = Color.Unspecified
)

val SwissBody = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 21.sp,
    letterSpacing = (-0.1).sp,
    color = Color.Unspecified
)

val SwissMeta = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.2.sp,
    color = Color.Unspecified
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 54.sp,
        letterSpacing = (-0.8).sp,
        color = TextPrimary
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.6).sp,
        color = TextPrimary
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
        color = TextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        color = TextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp,
        color = TextPrimary
    ),
    titleSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        color = TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        color = TextSecondary
    ),
    bodyMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
        color = TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
        color = TextPrimary
    ),
    labelMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        color = TextPrimary
    ),
    labelSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
        color = TextTertiary
    )
)

val SleekSmallFont = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.1.sp
)

val SleekBadgeFont = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.3.sp
)

val SleekTimestampFont = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.2.sp
)

// Clean Geometric Display & Monospace Accents
val NothingFontFamily = DisplayFontFamily
val NothingMonoFamily = MonoFontFamily

val NothingHeadlineLarge = SwissDisplayMedium

val NothingHeadlineMedium = SwissHeadline

val NothingHeadlineSmall = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.2).sp
)

val NothingSectionHeader = SwissSectionHeader

val NothingTechMono = SwissMono

val NothingDotBadge = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 9.5.sp,
    lineHeight = 13.sp,
    letterSpacing = 0.5.sp
)

// Aliases mapped cleanly to maintain complete compatibility across all screens
val EditorialSerifFont = DisplayFontFamily
val EditorialHeadlineLarge = SwissDisplayLarge
val EditorialHeadlineMedium = SwissDisplayMedium
val EditorialHeadlineSmall = SwissHeadline
val ExpressiveDisplayFont = SwissDisplayLarge
val ExpressiveSectionHeader = SwissSectionHeader
