package com.masterwave.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.ThemeState
import io.github.fletchmckee.liquid.LiquidState

enum class MainTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    STATS("Stats", Icons.Default.Equalizer),
    TOGETHER("Together", Icons.Default.People),
    LIBRARY("Library", Icons.AutoMirrored.Filled.QueueMusic),
    SEARCH("Search", Icons.Default.Search);

    companion object {
        val FEED = HOME
        val PLAYLISTS = LIBRARY
    }
}

/**
 * Universal Apple-Grade Liquid Glass Navigation Dock:
 * - Unified 5-tab floating shelf with authentic optical translucency.
 * - Tactile spring illumination: Active tab glows with a soft radial-tinted capsule.
 * - Responsive auto-compacting when feed scrolls (isMinimized).
 * - Specular dual-bevel directional border and soft multi-stop elevation.
 */
@Composable
fun FloatingNavigationDock(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null,
    isSearchActive: Boolean = false,
    liquidState: LiquidState? = null,
    isGlassEnabled: Boolean = true,
    isMinimized: Boolean = false,
    isSplit: Boolean = isMinimized
) {
    val isAmoled = ThemeState.isAmoledTheme()
    val allTabs = listOf(MainTab.HOME, MainTab.STATS, MainTab.TOGETHER, MainTab.LIBRARY, MainTab.SEARCH)
    val minimized = isMinimized || isSplit

    val dockSpring = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.84f,
        stiffness = Spring.StiffnessMediumLow
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (minimized) 28.dp else 14.dp,
        animationSpec = dockSpring,
        label = "dock_horizontal_padding"
    )
    val dockHeight by animateDpAsState(
        targetValue = if (minimized) 52.dp else 64.dp,
        animationSpec = dockSpring,
        label = "dock_island_height"
    )
    val iconCapsuleHeight by animateDpAsState(
        targetValue = if (minimized) 36.dp else 38.dp,
        animationSpec = dockSpring,
        label = "dock_capsule_height"
    )

    val unselectedColor = if (isAmoled) Color(0xFFB0B7C6) else Color(0xFF5A6072)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .height(dockHeight)
            .glassySurface(
                shape = RoundedCornerShape(32.dp),
                style = GlassyStyle.DOCK,
                isDark = isAmoled,
                borderWidth = 1.3.dp,
                elevation = if (minimized) 10.dp else 14.dp,
                accentGlow = Color.Transparent,
                tintColor = null,
                hasSheen = true,
                liquidState = liquidState,
                isGlassEnabled = isGlassEnabled
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = if (minimized) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            allTabs.forEach { tab ->
                val isSelected = (tab == currentTab && !isSearchActive) || (tab == MainTab.SEARCH && isSearchActive)

                // Distinct calibrated accent color palette for each tab
                val (tabAccent, tabTintLight, tabTintDark) = when (tab) {
                    MainTab.HOME -> Triple(Color(0xFFE53935), Color(0xFFFFECEB), Color(0x35E53935))
                    MainTab.STATS -> Triple(Color(0xFF00C853), Color(0xFFE8F8EE), Color(0x3500C853))
                    MainTab.TOGETHER -> Triple(Color(0xFFD81B60), Color(0xFFFFEDF3), Color(0x35FF4081))
                    MainTab.LIBRARY -> Triple(Color(0xFF2979FF), Color(0xFFEDF4FF), Color(0x352979FF))
                    MainTab.SEARCH -> Triple(Color(0xFFFF6D00), Color(0xFFFFF3E0), Color(0x35FF6D00))
                }

                val activeTint = if (isAmoled) tabTintDark else tabTintLight
                val activeBorder = if (isAmoled) tabAccent.copy(alpha = 0.50f) else tabAccent.copy(alpha = 0.35f)

                val itemBg by animateColorAsState(
                    targetValue = if (isSelected) activeTint else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "dock_bg_${tab.name}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .bouncyClickable(scaleDown = 0.94f) {
                            if (tab == MainTab.SEARCH) {
                                onSearchClick?.invoke() ?: onTabSelected(MainTab.SEARCH)
                            } else {
                                onTabSelected(tab)
                            }
                        }
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Glowing Icon Capsule
                        Box(
                            modifier = Modifier
                                .height(iconCapsuleHeight)
                                .fillMaxWidth(if (minimized) 0.85f else 0.78f)
                                .shadow(
                                    elevation = if (isSelected && !isAmoled) 2.dp else 0.dp,
                                    shape = CircleShape,
                                    spotColor = tabAccent.copy(alpha = 0.25f),
                                    ambientColor = Color.Transparent
                                )
                                .clip(CircleShape)
                                .background(itemBg)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = if (isAmoled) 0.60f else 0.90f),
                                                    activeBorder
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) tabAccent else unselectedColor,
                                modifier = Modifier.size(if (minimized) 20.dp else 21.dp)
                            )
                        }

                        // Label underneath with Fucek typography
                        AnimatedVisibility(
                            visible = !minimized,
                            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
                            exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                        ) {
                            Text(
                                text = tab.title,
                                fontFamily = DisplayFontFamily,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 10.sp,
                                color = if (isSelected) tabAccent else unselectedColor,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
