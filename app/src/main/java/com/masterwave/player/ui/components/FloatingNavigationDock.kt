package com.masterwave.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
 * - Two distinct floating optical glass islands separated by an optical air gap.
 * - Main Island: Primary tabs (Feed, Stats, Playlists) with spring physics active expansion.
 * - Satellite Island: Independent circular glass jewel button dedicated to quick Search.
 * - Completely neutral, crystal-clear frosted glass without any unwanted green tint.
 */
@Composable
fun FloatingNavigationDock(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null,
    isSearchActive: Boolean = false,
    liquidState: LiquidState? = null,
    isGlassEnabled: Boolean = true
) {
    val isAmoled = ThemeState.isAmoledTheme()
    val mainTabs = listOf(MainTab.HOME, MainTab.STATS, MainTab.LIBRARY)
    val isSatelliteActive = isSearchActive || currentTab == MainTab.SEARCH

    // Neutral monochrome colors - no green tints
    val selectedBg = if (isAmoled) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.White
    }
    val selectedBorder = if (isAmoled) {
        Color.White.copy(alpha = 0.32f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val selectedContent = if (isAmoled) Color.White else Color(0xFF141720)
    val unselectedColor = if (isAmoled) Color(0xFF9093A2) else Color(0xFF6B6E7D)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- Main Navigation Island ---
        Box(
            modifier = Modifier
                .weight(1f)
                .glassySurface(
                    shape = CircleShape,
                    style = GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.dp,
                    elevation = 10.dp,
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
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                mainTabs.forEach { tab ->
                    val isSelected = tab == currentTab && !isSatelliteActive
                    val tabWeight by animateFloatAsState(
                        targetValue = if (isSelected) 1.35f else 0.85f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dock_weight_${tab.name}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(tabWeight)
                            .height(44.dp)
                            .shadow(
                                elevation = if (isSelected && !isAmoled) 3.dp else 0.dp,
                                shape = CircleShape,
                                spotColor = Color.Black.copy(alpha = 0.10f),
                                ambientColor = Color.Black.copy(alpha = 0.04f)
                            )
                            .clip(CircleShape)
                            .background(
                                if (isSelected) selectedBg else Color.Transparent
                            )
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = selectedBorder,
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
                            .padding(horizontal = 8.dp),
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
                                modifier = Modifier.size(20.dp)
                            )
                            AnimatedVisibility(
                                visible = isSelected,
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
                .size(54.dp)
                .glassySurface(
                    shape = CircleShape,
                    style = if (isSatelliteActive) GlassyStyle.PROMINENT else GlassyStyle.DOCK,
                    isDark = isAmoled,
                    borderWidth = 1.dp,
                    elevation = if (isSatelliteActive) 8.dp else 4.dp,
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
                    .size(44.dp)
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
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
