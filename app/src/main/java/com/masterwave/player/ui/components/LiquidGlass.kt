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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
// GLASSY UI DESIGN SYSTEM ARCHITECTURE
// https://github.com/alexburlis/glassyui
// Principles:
// 1. Glass bends; it doesn't frost. Refracts backdrop & catches light at rims.
// 2. Glass needs something to bend: layered over charcoal & ambient content.
// 3. Slabs rest above page; touch compresses without swelling.
// 4. Directional specular rim highlight: bright entry, subtle counter-light.
// 5. High-contrast restrained status palette: Jade, Gold, Rose, Violet, Slate.
// ==========================================

enum class GlassyStyle {
    SURFACE,    // Standard frosted refractive glass for panels & cards
    DOCK,       // Floating translucent glass shelf with top sheen & optical depth
    SLAB,       // Tactile glass slab button resting above the page
    PROMINENT,  // Tinted blue/accent glass with bright top-left rim highlight
    HEADER,     // Floating header slab letting content glide smoothly behind
    CARD        // Rounded card glass surface
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
 * Universal Glassy UI Surface Modifier.
 * Implements the optical physics of Glassy UI:
 * - Multi-stop refractive translucent glass fill.
 * - Directional angle-specular rim highlight (peak light at top-left, soft counter-highlight at bottom-right).
 * - Upper rim sheen reflection.
 * - Dual-stage soft ambient drop shadow.
 * - Tactile spring slab compression on touch.
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

    // Tactile slab compression physics: buttons and controls compress slightly on touch
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glassy_scale"
    )

    // Multi-stop glass refraction fill based on Glassy UI style
    val glassFillBrush = remember(style, isDark, tintColor) {
        when (style) {
            GlassyStyle.DOCK -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEB161826),
                            Color(0xCC11131E),
                            Color(0xD9141622)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF2FFFFFF),
                            Color(0xE0F7F6FA),
                            Color(0xEBFFFFFF)
                        )
                    )
                }
            }
            GlassyStyle.HEADER -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF0121420),
                            Color(0xDC0F111B),
                            Color(0xCC0D0E16)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF6FAF9F7),
                            Color(0xE8F4F2EE),
                            Color(0xDCF0EEE9)
                        )
                    )
                }
            }
            GlassyStyle.PROMINENT -> {
                val base = tintColor ?: GlassyProminentBlue
                Brush.linearGradient(
                    colors = listOf(
                        base.copy(alpha = 0.95f),
                        base.copy(alpha = 0.82f),
                        base.copy(alpha = 0.70f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
            GlassyStyle.SLAB -> {
                if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x44FFFFFF),
                            Color(0x1AFFFFFF),
                            Color(0x28FFFFFF)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xB0FFFFFF),
                            Color(0x70FFFFFF),
                            Color(0x90FFFFFF)
                        )
                    )
                }
            }
            GlassyStyle.CARD, GlassyStyle.SURFACE -> {
                if (isDark) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD9161826),
                            Color(0xB310121C),
                            Color(0xCC131522)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF0FFFFFF),
                            Color(0xE0F6F5F8),
                            Color(0xEBFFFFFF)
                        )
                    )
                }
            }
        }
    }

    // Directional specular rim highlight simulating light entering from top-left
    val specularBorderBrush = remember(style, isDark) {
        when (style) {
            GlassyStyle.PROMINENT -> {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xB3FFFFFF), // Crisp top-left specular highlight
                        Color(0x40FFFFFF),
                        Color(0x20FFFFFF),
                        Color(0x60FFFFFF)  // Counter highlight
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            }
            GlassyStyle.HEADER -> {
                Brush.verticalGradient(
                    colors = listOf(
                        if (isDark) Color(0x35FFFFFF) else Color(0x60FFFFFF),
                        if (isDark) Color(0x10FFFFFF) else Color(0x15000000),
                        if (isDark) Color(0x25FFFFFF) else Color(0x1A000000)
                    )
                )
            }
            else -> {
                if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x75FFFFFF), // Peak entry specular highlight (top-left)
                            Color(0x22FFFFFF), // Edge attenuation
                            Color(0x10FFFFFF), // Floor alpha
                            Color(0x3AFFFFFF)  // Softer counter-highlight (bottom-right)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xB3FFFFFF),
                            Color(0x30000000),
                            Color(0x12000000),
                            Color(0x45FFFFFF)
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
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = if (accentGlow != Color.Transparent) accentGlow else Color.Black.copy(alpha = if (isDark) 0.55f else 0.12f),
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.04f)
        )
        .clip(shape)
        .background(glassFillBrush)
        .then(
            if (hasSheen && (style == GlassyStyle.DOCK || style == GlassyStyle.PROMINENT || style == GlassyStyle.SLAB)) {
                Modifier.drawWithContent {
                    drawContent()
                    // Refractive top sheen overlay (top 38% specular reflex)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x20FFFFFF),
                                Color(0x06FFFFFF),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.40f
                        ),
                        size = Size(size.width, size.height * 0.40f)
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
 * Glassy UI Slab Modifier: for buttons, icons, pills that rest above the page.
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
 * Authentic Glassy UI Status Badge with calibrated 4.5:1 text contrast.
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
 * Glassy UI Button Composable.
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
// BACKWARD COMPATIBILITY
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
    scaleDown: Float = 0.95f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
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
