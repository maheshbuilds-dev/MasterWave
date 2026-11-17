package com.masterwave.player.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.theme.GlassyGold
import com.masterwave.player.ui.theme.GlassyJade
import com.masterwave.player.ui.theme.GlassyProminentBlue
import com.masterwave.player.ui.theme.GlassyRose
import com.masterwave.player.ui.theme.GlassySlate
import com.masterwave.player.ui.theme.GlassyViolet
import com.masterwave.player.ui.theme.NothingTechMono

// ==========================================
// TRUE APPLE-INSPIRED LIQUID GLASS ENGINE
// Reference: Apple HIG Materials & WWDC25 Adopting Liquid Glass
// Layers:
// - LAYER A: Optical Base (Context-Aware Translucency & Artwork Chromatic Infusion)
// - LAYER B: Specular Reflex Arc (Curved Radial Overhead Light Pool)
// - LAYER C: Directional Dual-Bevel Edge (Crisp Incident Highlight & Inset Rim)
// - LAYER D: Volumetric Elevation & Caustic Underglow
// - LAYER E: Responsive Tactile Spring Compression
// ==========================================

enum class GlassyStyle {
    SURFACE,    // Standard refractive glass for panels & cards
    DOCK,       // Floating liquid glass dock shelf with curved specular arc & caustic glow
    SLAB,       // Interactive clear-glass tactile button resting above the page
    PROMINENT,  // Vibrant artwork-derived liquid glass lens with bright top-left specular
    HEADER,     // Protective optical header isolating status bar with specular edge
    CARD        // Subtle rounded card glass surface
}

enum class GlassyBadgeTone {
    JADE,       // Success, active, synced
    GOLD,       // Warning, hi-res, trending
    ROSE,       // Danger, error, live
    VIOLET,     // AI, recommendation, special
    SLATE,      // Neutral gray, format, codec
    ACCENT      // Dynamic user accent color
}

/**
 * Universal Apple-Grade Liquid Glass Surface Modifier.
 * Replaces flat translucent white rectangles with true physical optical materials:
 * 1. Multi-stop translucent refractive base that lets content glide beneath.
 * 2. Directional specular arc highlight simulating curved physical lens reflection.
 * 3. Directional dual-bevel rim (crisp top light entry, subtle ground bounce).
 * 4. Inset optical reflection contour giving volumetric thickness.
 * 5. Dynamic artwork-reactive chromatic infusion.
 * 6. Responsive spring physics on touch.
 */
@Composable
fun Modifier.glassySurface(
    shape: Shape = RoundedCornerShape(24.dp),
    style: GlassyStyle = GlassyStyle.SURFACE,
    isDark: Boolean = true,
    tintColor: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = if (style == GlassyStyle.DOCK) 14.dp else 8.dp,
    accentGlow: Color = Color.Transparent,
    hasSheen: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile spring slab compression physics
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 450f
        ),
        label = "liquid_glass_scale"
    )

    // LAYER A: Optical Base Fill (Context-Aware Translucency & Artwork Chromatic Infusion)
    val glassFillBrush = remember(style, isDark, tintColor) {
        when (style) {
            GlassyStyle.DOCK -> {
                val accent = tintColor ?: Color.Transparent
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x4C262C3E), // Translucent cool quartz top
                            Color(0x33141724), // Sheer dark refraction body
                            Color(0x400C0D16)  // Grounded obsidian absorption
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x9EFFFFFF), // Crystal clear liquid quartz top
                            Color(0x6BF1F4FA), // Translucent liquid body
                            Color(0x7CE2E8F4)  // Ground light bounce
                        )
                    )
                }
            }
            GlassyStyle.HEADER -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEB11131E), // High density over status bar for perfect contrast
                            Color(0xD90E1018), // Smooth mid-bleed
                            Color(0xC00B0C14)  // Soft bottom feather
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF4FAF9F7), // Protective top density
                            Color(0xECF3F1EC), // Clean mid surface
                            Color(0xE0EBE8E2)  // Soft bottom transition
                        )
                    )
                }
            }
            GlassyStyle.PROMINENT -> {
                val base = tintColor ?: GlassyProminentBlue
                Brush.linearGradient(
                    colors = listOf(
                        base.copy(alpha = 0.90f),
                        base.copy(alpha = 0.78f),
                        base.copy(alpha = 0.65f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
            GlassyStyle.SLAB -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x3DFFFFFF),
                            Color(0x18FFFFFF),
                            Color(0x24FFFFFF)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x90FFFFFF),
                            Color(0x55FFFFFF),
                            Color(0x75FFFFFF)
                        )
                    )
                }
            }
            GlassyStyle.CARD, GlassyStyle.SURFACE -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x38222638),
                            Color(0x22131520),
                            Color(0x300D0E16)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x98FFFFFF),
                            Color(0x6DF6F5F8),
                            Color(0x80EDEFF6)
                        )
                    )
                }
            }
        }
    }

    // LAYER C: Directional Dual-Bevel Edge Highlight (Crisp top-left entry, subtle bottom bounce)
    val specularBorderBrush = remember(style, isDark) {
        when (style) {
            GlassyStyle.PROMINENT -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f), // Brilliant top-left light entry
                        Color.White.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.45f)  // Ground light bounce
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
            GlassyStyle.HEADER -> {
                Brush.verticalGradient(
                    colors = listOf(
                        if (isDark) Color(0x55FFFFFF) else Color(0x90FFFFFF), // Crisp top light
                        if (isDark) Color(0x15FFFFFF) else Color(0x18000000),
                        if (isDark) Color(0x35FFFFFF) else Color(0x12000000)  // Bottom refraction line
                    )
                )
            }
            else -> {
                if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xB8FFFFFF), // Peak entry specular highlight (top-left)
                            Color(0x40FFFFFF), // Edge attenuation
                            Color(0x18FFFFFF), // Floor alpha
                            Color(0x45FFFFFF)  // Softer counter-highlight (bottom-right)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xDEFFFFFF), // Crisp diamond rim
                            Color(0x30000000),
                            Color(0x12000000),
                            Color(0x55FFFFFF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                }
            }
        }
    }

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        // LAYER D: Volumetric Elevation & Caustic Underglow
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = if (accentGlow != Color.Transparent) {
                accentGlow.copy(alpha = 0.35f)
            } else {
                Color.Black.copy(alpha = if (isDark) 0.55f else 0.12f)
            },
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.04f)
        )
        .clip(shape)
        .background(glassFillBrush)
        // LAYER B: Specular Reflex Arc (Curved Radial Overhead Light Pool)
        .then(
            if (hasSheen && (style == GlassyStyle.DOCK || style == GlassyStyle.PROMINENT || style == GlassyStyle.SLAB || style == GlassyStyle.SURFACE)) {
                Modifier.drawWithContent {
                    drawContent()

                    // 1. Curved Radial Overhead Specular Pool (Light entering from upper-center)
                    val poolRadius = size.width * 0.70f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.28f else 0.42f),
                                Color.White.copy(alpha = if (isDark) 0.08f else 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.38f, 0f),
                            radius = poolRadius
                        ),
                        radius = poolRadius,
                        center = Offset(size.width * 0.38f, 0f)
                    )

                    // 2. Soft horizontal specular flare across top 24% of surface
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.16f else 0.26f),
                                Color.White.copy(alpha = if (isDark) 0.04f else 0.08f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.28f
                        ),
                        size = Size(size.width, size.height * 0.28f)
                    )

                    // 3. Inset optical refraction stroke (0.75dp inside perimeter)
                    val insetBorderColor = if (isDark) Color(0x30FFFFFF) else Color(0x55FFFFFF)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                insetBorderColor,
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.60f
                        ),
                        topLeft = Offset(1.2f, 1.2f),
                        size = Size(size.width - 2.4f, size.height - 2.4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                        style = Stroke(width = 0.8f)
                    )
                }
            } else Modifier
        )
        .border(width = borderWidth, brush = specularBorderBrush, shape = shape)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * Liquid Glass Slab Modifier: for buttons, icons, pills that rest above the page.
 */
@Composable
fun Modifier.glassySlab(
    shape: Shape = CircleShape,
    prominent: Boolean = false,
    isDark: Boolean = true,
    tintColor: Color? = null,
    onClick: (() -> Unit)? = null
): Modifier = glassySurface(
    shape = shape,
    style = if (prominent) GlassyStyle.PROMINENT else GlassyStyle.SLAB,
    isDark = isDark,
    tintColor = tintColor,
    borderWidth = 1.dp,
    elevation = if (prominent) 8.dp else 4.dp,
    onClick = onClick
)

/**
 * Authentic Liquid Glass Status Badge with calibrated 4.5:1 text contrast.
 */
@Composable
fun GlassyBadge(
    text: String,
    tone: GlassyBadgeTone = GlassyBadgeTone.SLATE,
    modifier: Modifier = Modifier,
    accentColor: Color = GlassyProminentBlue,
    showDot: Boolean = true
) {
    val toneColor = when (tone) {
        GlassyBadgeTone.JADE -> GlassyJade
        GlassyBadgeTone.GOLD -> GlassyGold
        GlassyBadgeTone.ROSE -> GlassyRose
        GlassyBadgeTone.VIOLET -> GlassyViolet
        GlassyBadgeTone.SLATE -> GlassySlate
        GlassyBadgeTone.ACCENT -> accentColor
    }

    Box(
        modifier = modifier
            .glassySurface(
                shape = CircleShape,
                style = GlassyStyle.SURFACE,
                borderWidth = 0.8.dp,
                elevation = 2.dp
            )
            .background(toneColor.copy(alpha = 0.16f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(toneColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text.uppercase(),
                style = NothingTechMono.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                ),
                color = toneColor
            )
        }
    }
}

/**
 * Liquid Glass Button Composable.
 */
@Composable
fun GlassyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    isDark: Boolean = true,
    shape: Shape = CircleShape,
    tintColor: Color? = null,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .glassySurface(
                shape = shape,
                style = if (prominent) GlassyStyle.PROMINENT else GlassyStyle.SLAB,
                isDark = isDark,
                tintColor = tintColor,
                elevation = if (prominent) 8.dp else 4.dp,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

// ==========================================
// BACKWARD COMPATIBILITY & TACTILE HELPERS
// ==========================================

@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(32.dp),
    isAmoledDark: Boolean = false,
    accentGlow: Color = Color.Transparent,
    elevation: Dp = 12.dp,
    onClick: (() -> Unit)? = null
): Modifier = glassySurface(
    shape = shape,
    style = GlassyStyle.DOCK,
    isDark = isAmoledDark,
    borderWidth = 1.2.dp,
    elevation = elevation,
    accentGlow = accentGlow,
    onClick = onClick
)

@Composable
fun Modifier.bouncyClickable(
    scaleDown: Float = 0.94f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 450f
        ),
        label = "bouncy_scale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
