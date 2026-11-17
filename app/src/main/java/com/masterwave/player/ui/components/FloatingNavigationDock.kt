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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.ThemeState

enum class MainTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    SEARCH("Search", Icons.Default.Search),
    LIBRARY("Library", Icons.AutoMirrored.Filled.QueueMusic);

    companion object {
        val FEED = HOME
        val PLAYLISTS = LIBRARY
        val STATS = LIBRARY
    }
}

/**
 * APPLE MUSIC-INSPIRED HERO FLOATING LIQUID GLASS NAVIGATION DOCK
 * Engineered according to Apple HIG & WWDC25 Liquid Glass specifications:
 * - Genuine optical glass body with layered translucency.
 * - Curved overhead specular reflex arc catching ambient light.
 * - Directional dual-bevel rim (1.dp) with crisp top specular light entry.
 * - Restrained dynamic artwork chromatic caustic underglow.
 * - Interactive Liquid Glass lens pill for the active tab (no flat opaque solid fills).
 * - High-contrast, tactile glass controls for unselected tabs.
 * - Compact, floating ergonomic proportions.
 */
@Composable
fun FloatingNavigationDock(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor by ThemeState.effectiveAccentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dock_accent"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Hero Liquid Glass Shelf Container
        Box(
            modifier = Modifier
                .glassySurface(
                    shape = CircleShape,
                    style = GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.2.dp,
                    elevation = 14.dp,
                    accentGlow = animatedAccent.copy(alpha = 0.18f),
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
                            if (isAmoled) Color.White.copy(alpha = 0.78f) else Color(0xFF222532)
                        },
                        label = "pill_content"
                    )

                    // Individual interactive glass tab control
                    if (isSelected) {
                        // Selected State: Liquid Glass Lens Pill
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = CircleShape,
                                    spotColor = animatedAccent.copy(alpha = 0.45f),
                                    ambientColor = Color.Black.copy(alpha = 0.20f)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            animatedAccent.copy(alpha = 0.90f),
                                            animatedAccent.copy(alpha = 0.72f)
                                        )
                                    )
                                )
                                .drawWithContent {
                                    drawContent()
                                    // Top specular sheen on active pill
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                Color.White.copy(alpha = 0.10f),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY = size.height * 0.45f
                                        ),
                                        size = Size(size.width, size.height * 0.45f)
                                    )
                                }
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.80f),
                                            Color.White.copy(alpha = 0.25f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .bouncyClickable(scaleDown = 0.94f, onClick = { onTabSelected(tab) })
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Unselected State: High-contrast, tactile clear-glass control
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(CircleShape)
                                .bouncyClickable(scaleDown = 0.92f, onClick = { onTabSelected(tab) })
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = contentColor,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
