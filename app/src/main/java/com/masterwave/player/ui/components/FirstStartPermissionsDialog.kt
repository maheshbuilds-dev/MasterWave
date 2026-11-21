package com.masterwave.player.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.WebAccentEmerald
import com.masterwave.player.ui.theme.WebAccentGreen
import com.masterwave.player.ui.theme.WebAccentPeach
import com.masterwave.player.ui.theme.WebAccentSky
import com.masterwave.player.ui.theme.rememberAppThemeColors

/**
 * Checks if all permissions required for a complete music app experience are granted.
 */
fun hasRequiredPermissions(context: Context): Boolean {
    val permissions = getRequiredPermissions()
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

/**
 * Supercool First-Start Permissions Dialog:
 * Styled with MasterWave Tactile Bento & Liquid Glass Aesthetic.
 * Welcomes the user, explains permissions transparently, and asks with 1 high-contrast tap.
 */
@Composable
fun FirstStartPermissionsDialog(
    onPermissionsGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = rememberAppThemeColors()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted || hasRequiredPermissions(context)) {
            onPermissionsGranted()
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                shadowElevation = if (colors.isDark) 0.dp else 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // App Logo Badge with Waveform
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(WebAccentGreen)
                            .border(2.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "MasterWave",
                            tint = Color(0xFF0A0A0A),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Welcome to MasterWave",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = colors.ink,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WebAccentEmerald)
                                .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STUDIO HI-FI SOUND • ZERO ADS",
                                fontFamily = DisplayFontFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0A0A0A)
                            )
                        }
                    }

                    Text(
                        text = "To enable lossless playback, offline storage downloads, and lock-screen controls, MasterWave requires standard audio permissions:",
                        fontFamily = DisplayFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = colors.body,
                        textAlign = TextAlign.Center
                    )

                    // Permission Items in tactile bento cards
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PermissionBentoRow(
                            icon = Icons.Default.MusicNote,
                            title = "Music & Internal Storage",
                            desc = "Download high-fidelity songs & scan local audio files",
                            badgeColor = WebAccentGreen,
                            colors = colors
                        )

                        PermissionBentoRow(
                            icon = Icons.Default.Image,
                            title = "Cover Art & Artwork",
                            desc = "Display HD album thumbnails & save embedded art",
                            badgeColor = WebAccentPeach,
                            colors = colors
                        )

                        PermissionBentoRow(
                            icon = Icons.Default.NotificationsActive,
                            title = "Player & Download Alerts",
                            desc = "Lock screen media controls & real-time download status",
                            badgeColor = WebAccentSky,
                            colors = colors
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Grant All Button
                    Button(
                        onClick = {
                            permissionLauncher.launch(getRequiredPermissions())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF165B33),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Grant Permissions & Start",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Continue with Basic Features",
                            fontFamily = DisplayFontFamily,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBentoRow(
    icon: ImageVector,
    title: String,
    desc: String,
    badgeColor: Color,
    colors: com.masterwave.player.ui.theme.AppThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.canvas)
            .border(1.5.dp, colors.hairline, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(badgeColor)
                .border(1.5.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF0A0A0A),
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = colors.ink
            )
            Text(
                text = desc,
                fontFamily = DisplayFontFamily,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = colors.muted
            )
        }
    }
}
