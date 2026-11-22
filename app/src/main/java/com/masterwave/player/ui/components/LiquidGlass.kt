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
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid

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
    liquidState: LiquidState? = null,
    isGlassEnabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val opticalState = liquidState.takeIf {
        isGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) {
            if (opticalState != null) 0.98f else 0.95f
        } else 1f,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = 500f
        ),
        label = "liquid_glass_scale"
    )

    // LAYER A: Optical Base Fill (Calibrated density to prevent uncontrolled bleed)
    val glassFillBrush = remember(style, isDark, tintColor, isGlassEnabled) {
        if (!isGlassEnabled) {
            Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(Color(0xFF1B1D25), Color(0xFF111319))
                } else {
                    listOf(Color(0xFFF8F9FC), Color(0xFFE9ECF2))
                }
            )
        } else when (style) {
            GlassyStyle.DOCK -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xAA1E2333), // Luminous smoky obsidian crown (translucent)
                            Color(0x75131622), // Translucent optical core revealing underlying content
                            Color(0x8A0B0D15)  // Grounded base
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xC8FFFFFF), // Polished high-clarity porcelain crown (78% opacity)
                            Color(0x85F6F8FC), // Luminous translucent optical core (52% opacity)
                            Color(0xA5E8EEF8)  // Ground bounce with pure crystal tint (65% opacity)
                        )
                    )
                }
            }
            GlassyStyle.HEADER -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE810121C),
                            Color(0xE00D0F18),
                            Color(0xD80A0B12)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE8FFFFFF),
                            Color(0xE5FFFFFF),
                            Color(0xDDF8F6F4)
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
                        if (isDark) Color(0x40FFFFFF) else Color(0x90FFFFFF), // Crisp top specular light
                        if (isDark) Color(0x15FFFFFF) else Color(0x10000000),
                        if (isDark) Color(0x28FFFFFF) else Color(0x15000000)  // Defined bottom rim
                    )
                )
            }
            GlassyStyle.DOCK -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD9FFFFFF), // Fine diamond specular rim
                            Color(0x40FFFFFF), // Mid transmission
                            Color(0x14FFFFFF), // Floor alpha
                            Color(0x33FFFFFF)  // Bottom ground reflection
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF2FFFFFF), // Fine diamond specular rim
                            Color(0x66FFFFFF), // High sheen
                            Color(0x0F000000), // Shadow bevel
                            Color(0x40FFFFFF)  // Bottom ground reflection
                        )
                    )
                }
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

    val surfaceModifier = opticalState?.let { state ->
        Modifier.liquid(state) {
            this.shape = shape
            frost = when (style) {
                GlassyStyle.DOCK -> 24.dp
                GlassyStyle.PROMINENT -> 14.dp
                else -> 10.dp
            }
            refraction = when (style) {
                GlassyStyle.DOCK -> if (isPressed) 0.25f else 0.18f
                GlassyStyle.PROMINENT -> if (isPressed) 0.22f else 0.16f
                else -> if (isPressed) 0.16f else 0.10f
            }
            curve = if (style == GlassyStyle.DOCK) 0.24f else 0.16f
            edge = if (style == GlassyStyle.DOCK) 0.18f else 0.11f
            saturation = 1.15f
            contrast = 1.08f
            dispersion = if (style == GlassyStyle.DOCK) 0.035f else 0.015f
            tint = when {
                // Artwork-derived accent lens: a whisper of the current cover colour.
                tintColor != null && style == GlassyStyle.DOCK ->
                    tintColor.copy(alpha = if (isDark) 0.12f else 0.08f)
                // Neutral dock glass: cool optical white on light paper, smoky graphite in the dark.
                style == GlassyStyle.DOCK ->
                    if (isDark) Color(0xFF262B3A).copy(alpha = 0.20f)
                    else Color(0xFFF7FAFF).copy(alpha = 0.15f)
                tintColor != null -> tintColor.copy(alpha = 0.12f)
                else -> Color.White.copy(alpha = 0.10f)
            }
        }
    } ?: Modifier.background(glassFillBrush)

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
                Color.Black.copy(alpha = if (isDark) 0.50f else 0.14f)
            },
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.30f else 0.05f)
        )
        .clip(shape)
        .then(surfaceModifier)
        // LAYER B: Specular Reflex Arc (Restrained overhead radial highlight)
        .then(
            if (isGlassEnabled && hasSheen &&
                (style == GlassyStyle.DOCK ||
                    style == GlassyStyle.PROMINENT ||
                    style == GlassyStyle.SLAB ||
                    style == GlassyStyle.HEADER)
            ) {
                Modifier.drawWithContent {
                    drawContent()

                    // 1. Polished overhead light pool (centered, radiant flare)
                    val poolRadius = size.width * 0.60f
                    val poolCenter = Offset(size.width * 0.50f, -size.height * 0.12f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.24f else 0.28f),
                                Color.White.copy(alpha = if (isDark) 0.08f else 0.10f),
                                Color.Transparent
                            ),
                            center = poolCenter,
                            radius = poolRadius
                        ),
                        radius = poolRadius,
                        center = poolCenter
                    )

                    // 2. High-polish specular rim reflex along the top 22%
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.26f else 0.34f),
                                Color.White.copy(alpha = if (isDark) 0.08f else 0.12f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.22f
                        ),
                        size = Size(size.width, size.height * 0.22f)
                    )

                    // 3. Diagonal polished light sweep (30-degree gloss beam across the lens)
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (isDark) 0.04f else 0.08f),
                                Color.White.copy(alpha = if (isDark) 0.10f else 0.14f),
                                Color.White.copy(alpha = if (isDark) 0.03f else 0.05f),
                                Color.Transparent
                            ),
                            start = Offset(size.width * 0.12f, 0f),
                            end = Offset(size.width * 0.60f, size.height)
                        ),
                        size = size
                    )

                    // 4. Ground bounce: a faint counter-light at the bottom lip gives the slab thickness
                    val baseTop = size.height * 0.70f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                (if (isDark) Color.White else Color(0xFF1A2333))
                                    .copy(alpha = if (isDark) 0.10f else 0.07f)
                            ),
                            startY = baseTop,
                            endY = size.height
                        ),
                        topLeft = Offset(0f, baseTop),
                        size = Size(size.width, size.height - baseTop)
                    )
                }
            } else Modifier
        )
        .then(
            if (isGlassEnabled) {
                Modifier.border(width = borderWidth, brush = specularBorderBrush, shape = shape)
            } else {
                Modifier
            }
        )
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
    isGlassEnabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = glassySurface(
    shape = shape,
    style = if (prominent) GlassyStyle.PROMINENT else GlassyStyle.SLAB,
    isDark = isDark,
    tintColor = tintColor,
    isGlassEnabled = isGlassEnabled,
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
        animationSpec = if (isPressed) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            spring(
                dampingRatio = 0.72f,
                stiffness = 850f
            )
        },
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
