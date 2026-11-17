package com.masterwave.player.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// MASTERWAVE MUSIC DESIGN SYSTEM TOKENS
// Sonic Craft Architecture: Deep Obsidian, Tactile Glass & High-Contrast Audio Tones
// ==========================================

// --- Light Theme Tokens ---
val LightBackground = Color(0xFFF8F9FA) // Clean studio ivory background
val LightPrimaryGreen = Color(0xFF059669) // Modern refined emerald
val LightSoftTint = Color(0xFFD1FAE5) // Soft tint
val LightSurface = Color(0xFFFFFFFF) // Crisp surface
val LightSurfaceVariant = Color(0xFFF1F3F6) // Subtle card neutral
val LightBorder = Color(0xFFE2E4E9) // Subtle border
val LightTextPrimary = Color(0xFF11141A) // Deep charcoal
val LightTextSecondary = Color(0xFF5B6170) // Muted slate
val LightTextTertiary = Color(0xFF8A90A0)

// --- Dark Theme Tokens ---
val DarkBackground = Color(0xFF0A0B10) // Rich obsidian dark background
val DarkSurface = Color(0xFF12141C) // Layered dark surface
val DarkSurfaceElevated = Color(0xFF181B26) // Elevated card surface
val DarkSurfaceVariant = Color(0xFF202432)
val DarkBorder = Color(0xFF282C3D)
val DarkTextPrimary = Color(0xFFF5F6FA) // Warm crisp white
val DarkTextSecondary = Color(0xFF989EAF) // High-contrast muted grey
val DarkTextTertiary = Color(0xFF6B7184)
val DarkPrimaryGreen = Color(0xFF10B981) // Vibrant emerald green

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
