package com.masterwave.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

// ==========================================
// MASTERWAVE MUSIC DESIGN SYSTEM TOKENS
// Sonic Craft Architecture: Deep Obsidian, Tactile Glass & High-Contrast Audio Tones
// ==========================================

// ==========================================
// SWISS EDITORIAL DESIGN SYSTEM TOKENS
// Mathematical Grid, Obsidian Contrast, Hairline Rules & Swiss Red Focal Accent
// ==========================================
val SwissRed = Color(0xFFE53935)                  // International Typographic Style vermilion red
val SwissRedGlow = Color(0x4DE53935)
val SwissHairline = Color(0x1AFFFFFF)              // 10% white architectural rule
val SwissHairlineStrong = Color(0x33FFFFFF)        // 20% white structural section divider
val SwissObsidian = Color(0xFF07080A)              // Deep obsidian canvas
val SwissSurface = Color(0xFF0F1015)               // Layered dark card surface
val SwissSurfaceElevated = Color(0xFF161820)       // Subtle elevated panel
val SwissBorder = Color(0x14FFFFFF)                // Crisp hairline boundary
val SwissTextPrimary = Color(0xFFFBFBFD)           // Stark neo-grotesque white
val SwissTextSecondary = Color(0xFF8E8E93)         // Neutral typographic slate
val SwissTextTertiary = Color(0xFF5C5E69)          // Muted metadata

// --- MasterWave Core Brand Identity Tokens ---
val MasterWaveTurquoise = Color(0xFF00D4B2)      // Signature MasterWave vibrant turquoise
val MasterWaveCyan = Color(0xFF00B4D8)           // Electric cyan secondary accent
val MasterWaveTurquoiseDark = Color(0xFF00A88F)
val MasterWaveTurquoiseGlow = Color(0x6600D4B2)
val MasterWaveTurquoiseTint = Color(0x2000D4B2)  // Translucent chip / pill highlight

// --- Sage & Ivory Reference Brand Identity Tokens (from Reference Design) ---
val BrandSageGreen = Color(0xFF236B43)          // Signature deep sage green in the reference image
val BrandSageGreenLight = Color(0xFF388E5E)     // Slightly lighter sage for interactive states
val BrandSageGreenTint = Color(0x1F236B43)      // Translucent sage tint for badges and pills
val BrandSageSurface = Color(0xFFD6E7DC)        // Pale sage gradient start for "Infinite Radio" hero card
val BrandSageSurfaceEnd = Color(0xFFCBE0D1)     // Pale sage gradient end

// --- Light Theme Tokens (Calibrated from Reference Screenshots) ---
val LightBackground = Color(0xFFEFF2EC)         // Calming sage-ivory studio background matching reference
val LightPrimaryGreen = BrandSageGreen          // Signature sage green primary
val LightSoftTint = Color(0xFFE4EDE5)           // Soft green-tinted card surface
val LightSurface = Color(0xFFFFFFFF)            // Crisp white card surface
val LightSurfaceVariant = Color(0xFFE2EAE3)     // Subtle neutral sage card
val LightBorder = Color(0xFFD3DDD4)             // Soft border
val LightTextPrimary = Color(0xFF161A17)        // Deep charcoal green-black
val LightTextSecondary = Color(0xFF566057)      // Muted slate sage
val LightTextTertiary = Color(0xFF8B958C)

// --- Dark Theme Tokens ---
val DarkBackground = Color(0xFF090A0F)  // Rich obsidian dark background
val DarkSurface = Color(0xFF11131A)     // Layered dark surface
val DarkSurfaceElevated = Color(0xFF181B26) // Elevated card surface
val DarkSurfaceVariant = Color(0xFF1E2230)
val DarkBorder = Color(0xFF262A3B)
val DarkTextPrimary = Color(0xFFF5F6FA) // Warm crisp white
val DarkTextSecondary = Color(0xFF989EAF) // High-contrast muted grey
val DarkTextTertiary = Color(0xFF6B7184)
val DarkPrimaryGreen = MasterWaveTurquoise // Signature vibrant turquoise

// --- AMOLED Theme Tokens ---
val AmoledBackground = Color(0xFF000000) // Pure Pitch Black
val AmoledSurface = Color(0xFF08090C)
val AmoledSurfaceElevated = Color(0xFF101217)
val AmoledBorder = Color(0xFF181B22)

// --- Expressive Accent Colors ---
val AccentEmerald = Color(0xFF10B981)
val AccentLime = Color(0xFF22C55E)
val AccentCrimson = Color(0xFFF43F5E)
val AccentBurgundy = Color(0xFF881337)
val DarkPrimaryCrimson = AccentCrimson
val AccentMaroon = Color(0xFF9F1239)
val AccentRose = Color(0xFFFB7185)
val AccentViolet = Color(0xFF8B5CF6)
val AccentSapphire = Color(0xFF3B82F6)
val AccentAmber = Color(0xFFF59E0B)

// --- Translucent Glass & Overlays ---
val TranslucentGlassLight = Color(0xF2FFFFFF)
val TranslucentGlassDark = Color(0xE012141C)
val TranslucentGlassAmoled = Color(0xD4000000)
val WaveformGlow = Color(0x6610B981)
val WaveformTrack = Color(0x3310B981)

// --- Aliases for compatibility ---
val CanvasObsidian = DarkBackground
val SurfaceCharcoal = DarkSurface
val SurfaceElevated = DarkSurfaceElevated
val SurfaceCard = DarkSurfaceElevated
val BorderSubtle = DarkBorder
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary
val TextTertiary = DarkTextTertiary
val TranslucentGlass = TranslucentGlassDark
val TranslucentCrimson = Color(0x26F43F5E)
val AccentCrimsonGlow = WaveformGlow

// ==========================================
// GLASSY UI DESIGN SYSTEM TOKENS
// ==========================================
val GlassyProminentBlue = Color(0xFF2563EB)
val GlassyProminentFill = Color(0xFF2563EB)
val GlassySiteBgDark = Color(0xFF12141C)
val GlassySiteBgLight = Color(0xFFF8F9FA)
val GlassyPanelDark = Color(0x18FFFFFF)
val GlassyPanelLight = Color(0x80FFFFFF)
val GlassySlabTintDark = Color(0x4D181B26)
val GlassyHeaderTintDark = Color(0x6612141C)

// Glassy UI Status & Badge Tones (4.5:1 contrast calibrated)
val GlassyJade = Color(0xFF10B981)   // Success / Active
val GlassyGold = Color(0xFFF59E0B)   // Warning / Hi-Res
val GlassyRose = Color(0xFFF43F5E)   // Danger / Error
val GlassyViolet = Color(0xFF8B5CF6) // AI / Special
val GlassySlate = Color(0xFF9499AD)  // Neutral Gray

// Glassy UI Specular Rim Highlights
val GlassyRimSpecularPeak = Color(0x60FFFFFF)
val GlassyRimSpecularCounter = Color(0x28FFFFFF)
val GlassyRimFloor = Color(0x10FFFFFF)

// ==========================================
// APPLE MUSIC × MATERIAL 3 EXPRESSIVE TOKENS
// Neutral, gallery-like surfaces with a signature rose-red accent.
// ==========================================
val AppleMusicRed = Color(0xFFFA2D48)
val AppleMusicRose = Color(0xFFFF6482)
val AppleMusicCoral = Color(0xFFFF5A5F)
val AppleMusicRedDeep = Color(0xFFD70036)
val AppleMusicIndigo = Color(0xFF5E5CE6)

// Light surfaces (pure white canvas, soft grouped greys)
val AMLightBackground = Color(0xFFFFFFFF)
val AMLightSurface = Color(0xFFF2F2F7)
val AMLightSurfaceLow = Color(0xFFF7F7FA)
val AMLightSurfaceHigh = Color(0xFFECECF1)
val AMLightSurfaceHighest = Color(0xFFE5E5EA)
val AMLightTextPrimary = Color(0xFF1C1C1E)
val AMLightTextSecondary = Color(0xFF8E8E93)
val AMLightOutline = Color(0xFFC7C7CC)
val AMLightSeparator = Color(0xFFE5E5EA)

// Dark surfaces (near-black, layered charcoal)
val AMDarkBackground = Color(0xFF0E0E10)
val AMDarkSurface = Color(0xFF141416)
val AMDarkSurfaceHigh = Color(0xFF222224)
val AMDarkSurfaceHighest = Color(0xFF2C2C2E)
val AMDarkTextPrimary = Color(0xFFFFFFFF)
val AMDarkTextSecondary = Color(0xFF98989F)
val AMDarkOutline = Color(0xFF48484A)
val AMDarkSeparator = Color(0xFF2C2C2E)

// ==========================================
// MASTERWAVE WEBSITE DESIGN SYSTEM TOKENS
// Warm floral white, tactile bento surfaces, neo-brutalist ink outlines & pastel badges
// ==========================================
val WebCanvas = Color(0xFFFFFAF0)            // Warm floral white canvas (#fffaf0)
val WebSurfaceSoft = Color(0xFFFAF5E8)       // Soft cream surface (#faf5e8)
val WebSurfaceCard = Color(0xFFF5F0E0)       // Tactile bento card surface (#f5f0e0)
val WebInk = Color(0xFF0A0A0A)               // Crisp ink black (#0a0a0a)
val WebBody = Color(0xFF3A3A3A)              // Crisp body charcoal (#3a3a3a)
val WebMuted = Color(0xFF6A6A6A)             // Subtle metadata grey (#6a6a6a)
val WebHairline = Color(0xFFE5E5E5)          // Clean hairline rule (#e5e5e5)

// Website Pastel Accent Badges
val WebAccentViolet = Color(0xFFB8A4ED)
val WebAccentPink = Color(0xFFFF4D8B)
val WebAccentSky = Color(0xFFA7C7E7)
val WebAccentPeach = Color(0xFFFFB084)
val WebAccentOchre = Color(0xFFE8B94A)
val WebAccentMint = Color(0xFFA4D4C5)
val WebAccentGreen = Color(0xFF00E676)
val WebAccentEmerald = Color(0xFFBBF7D0)
val WebAccentRose = Color(0xFFFBCFE8)
val WebAccentIce = Color(0xFFBFDBFE)

// ==========================================
// DYNAMIC THEME-AWARE DESIGN SYSTEM PALETTE
// Harmonizes Light Warm Website Aesthetic with Dark/AMOLED Obsidian Aesthetic
// ==========================================
data class AppThemeColors(
    val canvas: Color,
    val surfaceSoft: Color,
    val surfaceCard: Color,
    val ink: Color,
    val body: Color,
    val muted: Color,
    val border: Color,
    val hairline: Color,
    val isDark: Boolean
)

@Composable
fun rememberAppThemeColors(): AppThemeColors {
    val themeMode by ThemeState.themeMode.collectAsState()
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemInDark
    }

    return remember(isDark) {
        if (isDark) {
            AppThemeColors(
                canvas = Color(0xFF000000),             // Pure pitch obsidian black
                surfaceSoft = Color(0xFF0D0D12),        // Soft obsidian panel
                surfaceCard = Color(0xFF14151C),        // Layered dark tactile card
                ink = Color(0xFFF5F5F7),                // Crisp bright white text
                body = Color(0xFFD1D1D6),               // Crisp secondary text
                muted = Color(0xFF8E8E93),              // Muted metadata
                border = Color(0xFF262833),             // Sleek dark tactile border
                hairline = Color(0xFF1C1E26),           // Subtle divider
                isDark = true
            )
        } else {
            AppThemeColors(
                canvas = WebCanvas,                     // Warm floral white (#fffaf0)
                surfaceSoft = WebSurfaceSoft,           // Soft cream (#faf5e8)
                surfaceCard = WebSurfaceCard,           // Bento card surface (#f5f0e0)
                ink = WebInk,                           // Crisp ink black (#0a0a0a)
                body = WebBody,                         // Crisp charcoal body (#3a3a3a)
                muted = WebMuted,                       // Muted metadata (#6a6a6a)
                border = Color(0xFF0A0A0A),             // Bold 2dp ink border
                hairline = WebHairline,                 // Clean hairline rule (#e5e5e5)
                isDark = false
            )
        }
    }
}
