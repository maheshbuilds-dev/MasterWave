package com.masterwave.player.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
 * Authentic Apple Liquid Glass Tab Switcher Button Modifier (iOS 26 Liquid Glass Material).
 * Eliminates flat opaque white paint and renders an optical refractive glass lens:
 * - Optical Base: Translucent gradient reflecting background colors with zero muddy occlusion.
 * - Specular Hairline: 0.9dp dual-bevel rim catching overhead illumination and ground bounce.
 * - Specular Reflex Sheen: Delicate top crest arc for authentic curvature depth.
 * - Caustic Elevation: Soft diffuse ambient occlusion with subtle chromatic accent underglow.
 */
@Composable
fun Modifier.liquidGlassTabSwitcher(
    isSelected: Boolean,
    isDark: Boolean,
    accentColor: Color,
    shape: Shape = CircleShape
): Modifier {
    val activeAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.84f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tab_switcher_active_alpha"
    )

    if (activeAlpha <= 0.01f) return this

    val fillBrush = remember(isDark, activeAlpha) {
        Brush.verticalGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.24f * activeAlpha),
                Color.White.copy(alpha = 0.08f * activeAlpha),
                Color.White.copy(alpha = 0.16f * activeAlpha)
            ) else listOf(
                Color.White.copy(alpha = 0.42f * activeAlpha),
                Color.White.copy(alpha = 0.16f * activeAlpha),
                Color.White.copy(alpha = 0.28f * activeAlpha)
            )
        )
    }

    val borderBrush = remember(isDark, activeAlpha) {
        Brush.verticalGradient(
            colors = if (isDark) listOf(
                Color.White.copy(alpha = 0.65f * activeAlpha),
                Color.White.copy(alpha = 0.25f * activeAlpha),
                Color.Transparent,
                Color.White.copy(alpha = 0.22f * activeAlpha)
            ) else listOf(
                Color.White.copy(alpha = 0.90f * activeAlpha),
                Color.White.copy(alpha = 0.40f * activeAlpha),
                Color.Black.copy(alpha = 0.06f * activeAlpha),
                Color.White.copy(alpha = 0.35f * activeAlpha)
            )
        )
    }

    return this
        .shadow(
            elevation = (2.5.dp * activeAlpha),
            shape = shape,
            spotColor = Color.Black.copy(alpha = (if (isDark) 0.35f else 0.08f) * activeAlpha),
            ambientColor = accentColor.copy(alpha = 0.10f * activeAlpha)
        )
        .clip(shape)
        .background(fillBrush)
        .border(
            width = 0.9.dp,
            brush = borderBrush,
            shape = shape
        )
        .drawWithContent {
            drawContent()
            val sheenHeight = size.height * 0.36f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (if (isDark) 0.25f else 0.42f) * activeAlpha),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = sheenHeight
                ),
                size = Size(size.width, sheenHeight)
            )
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

    // High-clarity Apple Liquid Glass tab styling matching authentic iOS 26 Liquid Glass.
    // Content uses vibrant accent color for high-contrast legibility.
    val selectedContent = accentColor
    // Crisp high-contrast dark slate unselected color from Image 1 reference
    val unselectedColor = if (isAmoled) Color(0xFFD1D5DB) else Color(0xFF282D37).copy(alpha = 0.85f)

    val dockSpring = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (minimized) 28.dp else 16.dp,
        animationSpec = dockSpring,
        label = "dock_horizontal_padding"
    )
    // Generous, luxurious height matching Image 1 reference (62dp)
    val islandHeight by animateDpAsState(
        targetValue = if (minimized) 52.dp else 62.dp,
        animationSpec = dockSpring,
        label = "dock_island_height"
    )
    val satelliteSize by animateDpAsState(
        targetValue = if (minimized) 52.dp else 62.dp,
        animationSpec = dockSpring,
        label = "dock_satellite_size"
    )
    val tabItemHeight by animateDpAsState(
        targetValue = if (minimized) 42.dp else 48.dp,
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
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
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

                    Box(
                        modifier = Modifier
                            .weight(tabWeight)
                            .height(tabItemHeight)
                            .liquidGlassTabSwitcher(
                                isSelected = isSelected,
                                isDark = isAmoled,
                                accentColor = accentColor,
                                shape = CircleShape
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
                    accentGlow = if (isSatelliteActive) accentColor.copy(alpha = 0.25f) else Color.Transparent,
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
                    .size(if (minimized) 40.dp else 46.dp)
                    .liquidGlassTabSwitcher(
                        isSelected = isSatelliteActive,
                        isDark = isAmoled,
                        accentColor = accentColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isSatelliteActive) selectedContent else unselectedColor,
                    modifier = Modifier.size(if (minimized) 20.dp else 22.dp)
                )
            }
        }
    }
}
