package com.masterwave.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
    HOME("Feed", Icons.Default.Home),
    STATS("Stats", Icons.Default.Equalizer),
    LIBRARY("Playlists", Icons.AutoMirrored.Filled.QueueMusic),
    SEARCH("Search", Icons.Default.Search);

    companion object {
        val FEED = HOME
        val PLAYLISTS = LIBRARY
    }
}

/**
 * Split Liquid Glass Navigation Dock:
 * - Main Island: Primary tabs (Feed, Stats, Playlists) with organic spring active pill.
 * - Satellite Island: Independent circular glass jewel dedicated to Search.
 * - When scrolling (isMinimized = true):
 *   Both islands gracefully minimize: active label collapses, height compacts,
 *   and all tab buttons remain fully visible and clickable to switch tabs instantly.
 * - Authentic optical frosted Liquid Glass with 1.3dp specular rim and soft ambient drop shadow.
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
    val accentColor by ThemeState.accentColor.collectAsState()
    val mainTabs = listOf(MainTab.HOME, MainTab.STATS, MainTab.LIBRARY)
    val isSatelliteActive = isSearchActive || currentTab == MainTab.SEARCH
    val minimized = isMinimized || isSplit

    // High-clarity frosted pill styling matching authentic Liquid Glass
    val selectedBg = if (isAmoled) {
        Color.White.copy(alpha = 0.22f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }
    val selectedBorder = if (isAmoled) {
        Color.White.copy(alpha = 0.35f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val selectedContent = if (isAmoled) Color.White else accentColor
    val unselectedColor = if (isAmoled) Color(0xFFB6BBCB) else Color(0xFF4B5565)

    val dockSpring = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (minimized) 32.dp else 16.dp,
        animationSpec = dockSpring,
        label = "dock_horizontal_padding"
    )
    val islandHeight by animateDpAsState(
        targetValue = if (minimized) 50.dp else 56.dp,
        animationSpec = dockSpring,
        label = "dock_island_height"
    )
    val satelliteSize by animateDpAsState(
        targetValue = if (minimized) 50.dp else 56.dp,
        animationSpec = dockSpring,
        label = "dock_satellite_size"
    )
    val tabItemHeight by animateDpAsState(
        targetValue = if (minimized) 42.dp else 46.dp,
        animationSpec = dockSpring,
        label = "dock_tab_item_height"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Main Navigation Island (Feed, Stats, Playlists) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(islandHeight)
                .glassySurface(
                    shape = CircleShape,
                    style = GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.3.dp,
                    elevation = if (minimized) 10.dp else 12.dp,
                    accentGlow = Color.Transparent,
                    tintColor = null,
                    hasSheen = true,
                    liquidState = liquidState,
                    isGlassEnabled = isGlassEnabled
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                mainTabs.forEach { tab ->
                    val isSelected = tab == currentTab && !isSatelliteActive

                    val tabWeight by animateFloatAsState(
                        targetValue = when {
                            minimized -> 1f // In minimized mode: equal compact weights for all tabs
                            isSelected -> 1.40f
                            else -> 0.85f
                        },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dock_weight_${tab.name}"
                    )

                    // Smooth, unhurried fill so the active pill glides instead of popping.
                    val itemBg by animateColorAsState(
                        targetValue = if (isSelected) selectedBg else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dock_bg_${tab.name}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(tabWeight)
                            .height(tabItemHeight)
                            .shadow(
                                elevation = if (isSelected && !isAmoled) 2.dp else 0.dp,
                                shape = CircleShape,
                                spotColor = Color.Black.copy(alpha = 0.10f),
                                ambientColor = Color.Black.copy(alpha = 0.03f)
                            )
                            .clip(CircleShape)
                            .background(itemBg)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 0.8.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = if (isAmoled) 0.40f else 0.95f),
                                                Color.White.copy(alpha = if (isAmoled) 0.10f else 0.45f),
                                                if (isAmoled) Color.White.copy(alpha = 0.12f)
                                                else Color.Black.copy(alpha = 0.05f)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .bouncyClickable(scaleDown = 0.95f) { onTabSelected(tab) }
                            .semantics {
                                role = Role.Tab
                                selected = isSelected
                            }
                            .padding(horizontal = if (minimized) 4.dp else 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) selectedContent else unselectedColor,
                                modifier = Modifier.size(if (minimized) 19.dp else 20.dp)
                            )
                            AnimatedVisibility(
                                visible = isSelected && !minimized,
                                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + expandHorizontally(),
                                exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + shrinkHorizontally()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tab.title,
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = selectedContent,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Satellite Action Island (Search) ---
        Box(
            modifier = Modifier
                .size(satelliteSize)
                .glassySurface(
                    shape = CircleShape,
                    style = if (isSatelliteActive) GlassyStyle.PROMINENT else GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.3.dp,
                    elevation = if (isSatelliteActive) 10.dp else 6.dp,
                    accentGlow = Color.Transparent,
                    tintColor = null,
                    hasSheen = true,
                    liquidState = liquidState,
                    isGlassEnabled = isGlassEnabled,
                    onClick = { onSearchClick?.invoke() ?: onTabSelected(MainTab.SEARCH) }
                )
                .semantics {
                    role = Role.Button
                    selected = isSatelliteActive
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (minimized) 38.dp else 44.dp)
                    .shadow(
                        elevation = if (isSatelliteActive && !isAmoled) 3.dp else 0.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.10f)
                    )
                    .clip(CircleShape)
                    .background(
                        if (isSatelliteActive) selectedBg else Color.Transparent
                    )
                    .then(
                        if (isSatelliteActive) {
                            Modifier.border(
                                width = 1.dp,
                                color = selectedBorder,
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isSatelliteActive) selectedContent else unselectedColor,
                    modifier = Modifier.size(if (minimized) 19.dp else 21.dp)
                )
            }
        }
    }
}
