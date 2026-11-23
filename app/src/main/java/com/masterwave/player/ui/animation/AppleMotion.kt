package com.masterwave.player.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Apple iOS & Apple Music Motion Architecture for MasterWave:
 * Implements authentic Apple Human Interface Guidelines physics:
 * - Fluid spring dynamics (Damping 0.82-0.86, Stiffness MediumLow).
 * - Tactile spring depression on press (0.96x scale with immediate elastic recovery).
 * - Full-sheet iOS presentations with damped slide-up transitions.
 * - Zero breathing/pulsing artifacts on static album artwork.
 */
object AppleMotion {

    /**
     * Standard Apple fluid spring specification (smooth, no overshoot).
     */
    fun <T> springStandard() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Apple tactile control bounce spring specification (subtle bouncy recovery).
     */
    fun <T> springTactile() = spring<T>(
        dampingRatio = 0.76f,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Apple iOS Modal Sheet presentation enter transition.
     */
    fun sheetEnter(): EnterTransition = slideInVertically(
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = Spring.StiffnessMediumLow
        )
    ) { fullHeight -> fullHeight } + fadeIn(
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
    )

    /**
     * Apple iOS Modal Sheet dismissal exit transition.
     */
    fun sheetExit(): ExitTransition = slideOutVertically(
        animationSpec = spring(
            dampingRatio = 0.90f,
            stiffness = Spring.StiffnessMediumLow
        )
    ) { fullHeight -> fullHeight } + fadeOut(
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
    )
}

/**
 * Modifier that applies Apple Music's signature tactile scale depression when pressed.
 * Scales down to [targetScale] (default 0.96) and bounces back with authentic spring physics.
 */
fun Modifier.applePressEffect(
    targetScale: Float = 0.96f,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = if (isPressed) {
            spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium)
        } else {
            spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "apple_press_scale"
    )

    val modifierWithClick = if (onClick != null) {
        this.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        this
    }

    modifierWithClick.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
