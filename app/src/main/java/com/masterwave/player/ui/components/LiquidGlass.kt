package com.masterwave.player.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass Modifier providing iOS/VisionOS style translucent refraction:
 * - Angle-specular highlight borders (linear gradient highlight).
 * - Multi-stop translucent refractive fill.
 * - Ambient tinted drop shadow with depth.
 * - Reactive liquid spring squish on touch.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(32.dp),
    isAmoledDark: Boolean = false,
    accentGlow: Color = Color.Transparent,
    elevation: Dp = 12.dp,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Liquid squish spring animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "liquid_scale"
    )

    // Glass refraction gradient
    val glassBrush = if (isAmoledDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xCC1A1A24),
                Color(0x99101016),
                Color(0xCC14141C)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xF0FFFFFF),
                Color(0xCCF7F5F8),
                Color(0xE6FFFFFF)
            )
        )
    }

    // Specular highlight border simulating light hitting the glass rim
    val specularBorderBrush = Brush.linearGradient(
        colors = if (isAmoledDark) {
            listOf(
                Color(0x66FFFFFF),
                Color(0x1AFFFFFF),
                Color(0x33FFFFFF)
            )
        } else {
            listOf(
                Color(0xB3FFFFFF),
                Color(0x33FFFFFF),
                Color(0x80FFFFFF)
            )
        }
    )

    return this
        .scale(scale)
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = if (accentGlow != Color.Transparent) accentGlow else Color.Black.copy(alpha = if (isAmoledDark) 0.6f else 0.15f)
        )
        .clip(shape)
        .background(glassBrush)
        .border(width = 1.2.dp, brush = specularBorderBrush, shape = shape)
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
