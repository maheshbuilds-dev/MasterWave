package com.masterwave.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.masterwave.player.data.update.AppUpdateManager
import com.masterwave.player.data.update.UpdateInfo
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.WebAccentEmerald
import com.masterwave.player.ui.theme.WebAccentGreen
import com.masterwave.player.ui.theme.WebAccentSky
import com.masterwave.player.ui.theme.rememberAppThemeColors

/**
 * MasterWave In-App Update Banner:
 * Seamlessly matches the app's tactile bento & liquid glass aesthetic across light and dark modes.
 */
@Composable
fun InAppUpdateBanner(
    update: UpdateInfo,
    onUpdateClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = rememberAppThemeColors()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpdateClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(WebAccentGreen)
                    .border(1.5.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = Color(0xFF0A0A0A),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MasterWave Update",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.ink
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(WebAccentEmerald)
                            .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "v${update.versionName}",
                            fontFamily = DisplayFontFamily,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0A0A0A)
                        )
                    }
                }
                Text(
                    text = "Tap to update app directly",
                    fontFamily = DisplayFontFamily,
                    fontSize = 12.sp,
                    color = colors.muted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onUpdateClick,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (colors.isDark) colors.ink else Color(0xFF0A0A0A),
                    contentColor = if (colors.isDark) Color(0xFF0A0A0A) else Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "Update",
                    fontFamily = DisplayFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = colors.muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * MasterWave In-App Update Modal Dialog:
 * Styled identically to the official release cards, featuring real-time download progress
 * and trusted 1-tap system package installation.
 */
@Composable
fun InAppUpdateModal(
    update: UpdateInfo,
    updateManager: AppUpdateManager,
    onDismiss: () -> Unit
) {
    val colors = rememberAppThemeColors()
    val downloadProgress by updateManager.downloadProgress.collectAsState()
    val isInstalling by updateManager.isInstalling.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "update_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Dialog(onDismissRequest = {
        if (downloadProgress == null) onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = colors.surfaceCard,
            border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
            shadowElevation = if (colors.isDark) 0.dp else 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(WebAccentGreen)
                        .border(2.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color(0xFF0A0A0A),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "New MasterWave Update",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WebAccentEmerald)
                                .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v${update.versionName}",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF0A0A0A)
                            )
                        }
                        Text(
                            text = "•  ${update.apkSize}  •  Official Release",
                            fontFamily = DisplayFontFamily,
                            fontSize = 12.sp,
                            color = colors.muted
                        )
                    }
                }

                // Changelog Bento Box
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceSoft,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "What's in this version:",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.ink
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = update.releaseNotes,
                            fontFamily = DisplayFontFamily,
                            fontSize = 12.sp,
                            color = colors.body,
                            lineHeight = 17.sp
                        )
                    }
                }

                // Download Progress / Status
                val currentProgress = downloadProgress
                if (currentProgress != null) {
                    val progressFloat = currentProgress.coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFloat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = WebAccentGreen,
                            trackColor = colors.hairline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isInstalling) "Opening Installer..." else "Downloading update package...",
                                fontFamily = DisplayFontFamily,
                                fontSize = 12.sp,
                                color = colors.muted
                            )
                            Text(
                                text = "${(progressFloat * 100).toInt()}%",
                                fontFamily = DisplayFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.ink
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (downloadProgress == null) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceSoft,
                                contentColor = colors.ink
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                                .height(46.dp)
                        ) {
                            Text(
                                text = "Later",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (downloadProgress == null) {
                                updateManager.downloadAndInstall(update)
                            }
                        },
                        enabled = downloadProgress == null || isInstalling,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colors.isDark) colors.ink else Color(0xFF0A0A0A),
                            contentColor = if (colors.isDark) Color(0xFF0A0A0A) else Color.White
                        ),
                        modifier = Modifier
                            .weight(if (downloadProgress == null) 1.5f else 1f)
                            .border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                            .height(46.dp)
                    ) {
                        if (downloadProgress != null) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(rotation)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isInstalling) "Installing..." else "Downloading...",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Update Now",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
