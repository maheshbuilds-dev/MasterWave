package com.masterwave.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.theme.ThemeState

enum class MainTab(val title: String, val icon: ImageVector) {
    FEED("Feed", Icons.Default.Home),
    STATS("Stats", Icons.Default.BarChart),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.QueueMusic)
}

/**
 * Pixel-perfect Liquid Glass Floating Navigation Dock:
 * - Compact centered stadium pill (width ~260dp, height ~52dp) matching Screenshot 5 exactly.
 * - Frosted glass refraction with specular white border.
 * - Soft colored capsule indicator on active tab.
 * - Spring touch physics.
 */
@Composable
fun FloatingNavigationDock(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    // Glass fill colors matching screenshot
    val glassBg = if (isAmoled) {
        Color(0xD9101018) // Frosted deep obsidian
    } else {
        Color(0xE6FFFFFF) // Frosted crisp white glass
    }

    val glassBorder = if (isAmoled) {
        Brush.verticalGradient(
            listOf(Color(0x55FFFFFF), Color(0x1AFFFFFF), Color(0x33FFFFFF))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xE6FFFFFF), Color(0x88FFFFFF), Color(0xCCFFFFFF))
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating compact capsule pill (centered, not full-width!)
        Surface(
            shape = CircleShape,
            color = glassBg,
            shadowElevation = if (isAmoled) 16.dp else 12.dp,
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    spotColor = if (isAmoled) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.12f)
                )
                .clip(CircleShape)
                .border(width = 1.2.dp, brush = glassBorder, shape = CircleShape)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = tab == currentTab

                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isAmoled) accentColor.copy(alpha = 0.32f) else accentColor.copy(alpha = 0.16f)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pill_bg"
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            if (isAmoled) Color.White else accentColor
                        } else {
                            if (isAmoled) Color(0xFF888898) else Color(0xFF6E6E7A)
                        },
                        label = "pill_content"
                    )

                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(containerColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab) }
                            )
                            .padding(horizontal = if (isSelected) 18.dp else 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
