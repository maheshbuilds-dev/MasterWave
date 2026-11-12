package com.masterwave.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.ThemeState
import java.util.Locale

/**
 * Audio & Playback Modal engineered to match LastWave Reference Frame 15.0s:
 * - Header: Audio & Playback (Volume, seamless transitions & speed), Close button.
 * - In-App Volume: 0-100% draggable slider.
 * - Speed & Pitch: "Linked" toggle, Reset button, Speed (0.50x-2.00x) & Pitch (0.50x-2.00x).
 * - Acoustic Tuning & EQ: Launch Snapdragon / Dolby Atmos DSP.
 * - Music Haptics: Rhythm synchronization via haptic motor.
 */
@Composable
fun AudioPlaybackModal(
    player: MasterWavePlayer,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val volume by player.volume.collectAsState()
    val speed by player.playbackSpeed.collectAsState()
    val pitch by player.playbackPitch.collectAsState()
    val isLinked by player.isSpeedPitchLinked.collectAsState()
    val musicHaptics by player.musicHapticsEnabled.collectAsState()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val sheetBg = if (isAmoled) Color(0xFF101016) else Color(0xFFF9F7FA)
    val cardBg = if (isAmoled) Color(0xFF181822) else Color(0xFFEFECEF).copy(alpha = 0.65f)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)
    val greenAccent = Color(0xFF34C759)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = sheetBg,
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Intercept clicks so sheet doesn't close
                )
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drag handle pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (isAmoled) Color(0x33FFFFFF) else Color(0x33000000))
                )

                // --- 1. Header (Icon + Title & Subtitle + Close Button) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(greenAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = greenAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio & Playback",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "Volume, seamless transitions & speed",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isAmoled) Color(0xFF222230) else Color(0xFFE0DCE2))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // --- 2. In-App Volume Card ---
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "In-App Volume",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )
                            }
                            Text(
                                text = "${(volume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = textPrimary
                            )
                        }

                        // Volume Slider Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeDown,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Slider(
                                value = volume,
                                onValueChange = { player.setVolume(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = textPrimary,
                                    activeTrackColor = accentColor,
                                    inactiveTrackColor = if (isAmoled) Color(0xFF2C2C3C) else Color(0xFFD4D0D8)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // --- 3. Speed & Pitch Card (Linked + Reset) ---
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Speed & Pitch",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )
                                Text(
                                    text = if (isLinked) "Tempo & pitch linked" else "Independent pitch & tempo",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = textSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Linked Pill Badge
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isLinked) greenAccent.copy(alpha = 0.2f)
                                            else (if (isAmoled) Color(0xFF262634) else Color(0xFFDFDBE1))
                                        )
                                        .clickable { player.toggleSpeedPitchLinked() }
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isLinked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = if (isLinked) greenAccent else textSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isLinked) "Linked" else "Unlinked",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = if (isLinked) greenAccent else textSecondary
                                        )
                                    }
                                }

                                // Reset circular button
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isAmoled) Color(0xFF262634) else Color(0xFFDFDBE1))
                                        .clickable { player.resetPlaybackParameters() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset",
                                        tint = textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        // Playback Speed Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Playback Speed",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = textSecondary
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2fx", speed),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = greenAccent
                                    )
                                )
                            }
                            Slider(
                                value = speed,
                                onValueChange = { player.setPlaybackSpeed(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = greenAccent,
                                    activeTrackColor = greenAccent,
                                    inactiveTrackColor = if (isAmoled) Color(0xFF2C2C3C) else Color(0xFFD4D0D8)
                                )
                            )
                        }

                        // Pitch Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pitch",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = textSecondary
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2fx", pitch),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = greenAccent
                                    )
                                )
                            }
                            Slider(
                                value = pitch,
                                onValueChange = { player.setPlaybackPitch(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = greenAccent,
                                    activeTrackColor = greenAccent,
                                    inactiveTrackColor = if (isAmoled) Color(0xFF2C2C3C) else Color(0xFFD4D0D8)
                                )
                            )
                        }
                    }
                }

                // --- 4. Acoustic Tuning & EQ Card ---
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(greenAccent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = greenAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Acoustic Tuning & EQ",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )
                                Text(
                                    text = "AutoEQ correction, target curves & PEQ",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = textSecondary
                                )
                            }
                        }

                        // Open AutoEQ / Parametric EQ Button
                        Surface(
                            shape = CircleShape,
                            color = greenAccent.copy(alpha = 0.22f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(CircleShape)
                                .clickable { player.openSystemEqualizer() }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headset,
                                    contentDescription = null,
                                    tint = greenAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Open AutoEQ / Parametric EQ",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = greenAccent
                                )
                            }
                        }
                    }
                }

                // --- 5. Music Haptics Card ---
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(greenAccent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = greenAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Music Haptics",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )
                                Text(
                                    text = "Physical rhythm synchronization via haptic motor",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = musicHaptics,
                            onCheckedChange = { player.toggleMusicHaptics(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = greenAccent,
                                uncheckedThumbColor = textSecondary,
                                uncheckedTrackColor = if (isAmoled) Color(0xFF2C2C3C) else Color(0xFFD4D0D8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
