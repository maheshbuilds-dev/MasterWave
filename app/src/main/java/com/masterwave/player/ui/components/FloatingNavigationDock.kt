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
import com.masterwave.player.ui.theme.SleekSmallFont
import com.masterwave.player.ui.theme.ThemeState

enum class MainTab(val title: String, val icon: ImageVector) {
    FEED("Home", Icons.Default.Home),
    STATS("Stats", Icons.Default.BarChart),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.QueueMusic)
}

/**
 * Authentic 3D Liquid Glass Floating Navigation Dock:
 * - Real multi-layer frosted acrylic glass with optical refraction.
 * - 3D top-edge specular highlight & bottom contact bevel.
 * - Deep 24dp dual-stage ambient drop shadow.
 * - Tactile 3D pill indicator with micro-relief highlight.
 * - Spring touch physics and sleek geometric typography.
 */
@Composable
fun FloatingNavigationDock(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    // Dynamic Color Morphing Accent based on Active Tab
    val morphAccent by animateColorAsState(
        targetValue = when (currentTab) {
            MainTab.FEED -> accentColor
            MainTab.STATS -> Color(0xFF2979FF)
            MainTab.PLAYLISTS -> Color(0xFFD946EF)
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dock_morph_accent"
    )


    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .glassySurface(
                    shape = CircleShape,
                    style = GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.2.dp,
                    elevation = 14.dp,
                    hasSheen = true
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MainTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            Color.White
                        } else {
                            if (isAmoled) Color(0xFFA0A0B0) else Color(0xFF6E6E78)
                        },
                        label = "pill_content"
                    )

                    val pillModifier = if (isSelected) {
                        Modifier
                            .height(42.dp)
                            .glassySurface(
                                shape = CircleShape,
                                style = GlassyStyle.PROMINENT,
                                tintColor = morphAccent,
                                elevation = 4.dp
                            )
                    } else {
                        Modifier
                            .height(42.dp)
                            .clip(CircleShape)
                    }

                    Row(
                        modifier = pillModifier
                            .bouncyClickable(scaleDown = 0.92f, onClick = { onTabSelected(tab) })
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
                                style = SleekSmallFont.copy(
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
