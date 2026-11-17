package com.masterwave.player.ui.components

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
// REFINED APPLE-GRADE LIQUID GLASS ENGINE
// Reference: Apple HIG Materials & WWDC25 Adopting Liquid Glass
// - Calibrated density: No uncontrolled bleed-through or plastic transparency.
// - Restrained specular curvature: Soft radial light pool, no harsh stripes.
// - Fine optical dual-bevel edges: 1dp directional rim with top light entry.
// - Subtle artwork chromatic infusion.
// - Tactile spring compression on interaction.
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
 * - Layer A: Calibrated optical base fill preventing content scramble underneath.
 * - Layer B: Soft overhead specular sheen arc.
 * - Layer C: 1dp directional dual-bevel rim (crisp top specular, subtle ground bounce).
 * - Layer D: Restrained ambient occlusion elevation.
 * - Layer E: Tactile spring compression on touch.
 */
@Composable
fun Modifier.glassySurface(
    shape: Shape = RoundedCornerShape(24.dp),
    style: GlassyStyle = GlassyStyle.SURFACE,
    isDark: Boolean = true,
    tintColor: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = if (style == GlassyStyle.DOCK) 10.dp else 6.dp,
    accentGlow: Color = Color.Transparent,
    hasSheen: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile spring slab compression physics
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = 500f
        ),
        label = "liquid_glass_scale"
    )

    // LAYER A: Optical Base Fill (Calibrated density to prevent uncontrolled bleed)
    val glassFillBrush = remember(style, isDark, tintColor) {
        when (style) {
            GlassyStyle.DOCK -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDF171A27), // 87% density: rich smoky quartz
                            Color(0xD2121420), // 82% density: translucent core
                            Color(0xDA0E1018)  // 85% density: grounded base
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF2FFFFFF), // 95% density: crisp liquid crystal
                            Color(0xE4F4F6FB), // 89% density: translucent quartz body
                            Color(0xEBEDF1F8)  // 92% density: ground light bounce
                        )
                    )
                }
            }
            GlassyStyle.HEADER -> {
                // High protective density: completely isolates status bar and category chips
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF610121C), // 96% density over status bar
                            Color(0xF00D0F18), // 94% density
                            Color(0xEA0A0B12)  // 92% density at curved bottom edge
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF8FAF9F7), // 97% density over status bar
                            Color(0xF4F2F0EB), // 95% density
                            Color(0xEEEDE9E3)  // 93% density at curved bottom edge
                        )
                    )
                }
            }
            GlassyStyle.PROMINENT -> {
                val base = tintColor ?: GlassyProminentBlue
                Brush.linearGradient(
                    colors = listOf(
                        base.copy(alpha = 0.88f),
                        base.copy(alpha = 0.75f),
                        base.copy(alpha = 0.62f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
            GlassyStyle.SLAB -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x38FFFFFF),
                            Color(0x15FFFFFF),
                            Color(0x22FFFFFF)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88FFFFFF),
                            Color(0x48FFFFFF),
                            Color(0x65FFFFFF)
                        )
                    )
                }
            }
            GlassyStyle.CARD, GlassyStyle.SURFACE -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE0151826),
                            Color(0xD010121C),
                            Color(0xD80D0E16)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF4FFFFFF),
                            Color(0xECF6F5F8),
                            Color(0xE4EDEFF6)
                        )
                    )
                }
            }
        }
    }

    // LAYER C: Directional Dual-Bevel Edge Highlight (1dp fine stroke)
    val specularBorderBrush = remember(style, isDark) {
        when (style) {
            GlassyStyle.PROMINENT -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.80f), // Brilliant top-left light entry
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.40f)  // Ground light bounce
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
            GlassyStyle.HEADER -> {
                Brush.verticalGradient(
                    colors = listOf(
                        if (isDark) Color(0x35FFFFFF) else Color(0x70FFFFFF), // Subtle top light
                        if (isDark) Color(0x10FFFFFF) else Color(0x12000000),
                        if (isDark) Color(0x25FFFFFF) else Color(0x0E000000)  // Bottom edge definition
                    )
                )
            }
            else -> {
                if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x75FFFFFF), // Peak entry specular highlight (top-left)
                            Color(0x28FFFFFF), // Edge attenuation
                            Color(0x10FFFFFF), // Floor alpha
                            Color(0x30FFFFFF)  // Softer counter-highlight (bottom-right)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xB5FFFFFF), // Crisp diamond rim
                            Color(0x20000000),
                            Color(0x0E000000),
                            Color(0x40FFFFFF)
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
                accentGlow.copy(alpha = 0.25f)
            } else {
                Color.Black.copy(alpha = if (isDark) 0.40f else 0.08f)
            },
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.25f else 0.03f)
        )
        .clip(shape)
        .background(glassFillBrush)
        // LAYER B: Specular Reflex Arc (Restrained overhead radial highlight)
        .then(
            if (hasSheen && (style == GlassyStyle.DOCK || style == GlassyStyle.PROMINENT || style == GlassyStyle.SLAB)) {
                Modifier.drawWithContent {
                    drawContent()

                    // Soft radial overhead specular highlight pool
                    val poolRadius = size.width * 0.65f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.18f else 0.30f),
                                Color.White.copy(alpha = if (isDark) 0.05f else 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.38f, 0f),
                            radius = poolRadius
                        ),
                        radius = poolRadius,
                        center = Offset(size.width * 0.38f, 0f)
                    )

                    // Subtle top-rim light reflex across top 22%
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.12f else 0.20f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.22f
                        ),
                        size = Size(size.width, size.height * 0.22f)
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
    elevation = if (prominent) 6.dp else 3.dp,
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
                elevation = if (prominent) 6.dp else 3.dp,
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
    elevation: Dp = 10.dp,
    onClick: (() -> Unit)? = null
): Modifier = glassySurface(
    shape = shape,
    style = GlassyStyle.DOCK,
    isDark = isAmoledDark,
    borderWidth = 1.dp,
    elevation = elevation,
    accentGlow = accentGlow,
    onClick = onClick
)

@Composable
fun Modifier.bouncyClickable(
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = 500f
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
