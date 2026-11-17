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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
 * APPLE MUSIC-INSPIRED HERO FLOATING LIQUID GLASS DOCK
 * Phase 5 Refinements:
 * - Compact floating shape (height = 54dp).
 * - Avoids solid opaque colored pills; uses an authentic translucent glass lens.
 * - Dynamic artwork-adaptive reflection with restrained caustic underglow.
 * - Excellent icon contrast across light and dark modes.
 * - Zero internal bottom padding that could cause overlap with parent or mini-player.
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
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Floating Optical Glass Capsule
        Box(
            modifier = Modifier
                .glassySurface(
                    shape = CircleShape,
                    style = GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.dp,
                    elevation = 10.dp,
                    accentGlow = animatedAccent.copy(alpha = 0.15f),
                    hasSheen = true
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val appleCrimson = Color(0xFF880E4F)

                MainTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab

                    val unselectedColor = if (isAmoled) Color(0xFF9898A6) else Color(0xFF757582)

                    if (isSelected) {
                        // Active State: Refined Apple Music Crimson Liquid Glass Lens Pill
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = CircleShape,
                                    spotColor = appleCrimson.copy(alpha = 0.40f),
                                    ambientColor = Color.Black.copy(alpha = 0.12f)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            appleCrimson.copy(alpha = if (isAmoled) 0.35f else 0.16f),
                                            appleCrimson.copy(alpha = if (isAmoled) 0.18f else 0.08f)
                                        )
                                    )
                                )
                                .drawWithContent {
                                    drawContent()
                                    // Subtle top specular highlight on active lens
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY = size.height * 0.40f
                                        ),
                                        size = Size(size.width, size.height * 0.40f)
                                    )
                                }
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            appleCrimson.copy(alpha = 0.65f),
                                            appleCrimson.copy(alpha = 0.20f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .bouncyClickable(scaleDown = 0.95f, onClick = { onTabSelected(tab) })
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isAmoled) Color.White else appleCrimson,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isAmoled) Color.White else appleCrimson
                                )
                            }
                        }
                    } else {
                        // Unselected State: High-contrast, tactile clear-glass control
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(CircleShape)
                                .bouncyClickable(scaleDown = 0.92f, onClick = { onTabSelected(tab) })
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = unselectedColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
