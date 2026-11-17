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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.masterwave.player.ui.theme.SleekSmallFont
import com.masterwave.player.ui.theme.ThemeState

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
 * Elegant First-Start Permissions Dialog:
 * Welcomes the user on app first launch, clearly explains required permissions,
 * and requests all permissions simultaneously in one cohesive step.
 */
@Composable
fun FirstStartPermissionsDialog(
    onPermissionsGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

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

    val glassBg = if (isAmoled) {
        Brush.verticalGradient(
            listOf(Color(0xF5181724), Color(0xEB0E0D14), Color(0xF50A0910))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFDFDFE), Color(0xF7F4F7), Color(0xEEEDF2))
        )
    }

    val specularBorder = if (isAmoled) {
        Brush.verticalGradient(
            listOf(Color(0x60FFFFFF), Color(0x20FFFFFF), Color(0x05FFFFFF))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFFFFFFF), Color(0xCCFFFFFF), Color(0x40165B33))
        )
    }

    val textPrimary = if (isAmoled) Color.White else Color(0xFF141316)
    val textSecondary = if (isAmoled) Color(0xFFA0A0B2) else Color(0xFF656470)

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
                .background(Color.Black.copy(alpha = 0.60f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent,
                shadowElevation = 24.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = if (isAmoled) Color.Black else Color(0x35000000)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(glassBg)
                    .border(1.5.dp, specularBorder, RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // App Logo Badge
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(8.dp, CircleShape, spotColor = accentColor.copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(accentColor, accentColor.copy(alpha = 0.85f))
                                )
                            )
                            .border(1.2.dp, Color(0x60FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "MasterWave",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Welcome to MasterWave",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        ),
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "To enable lossless playback, local library scanning, and lock-screen controls, MasterWave requires standard permissions:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Permission Items
                    PermissionFeatureRow(
                        icon = Icons.Default.MusicNote,
                        title = "Music & Audio Files",
                        desc = "Scan device storage & play downloaded offline tracks",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = accentColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionFeatureRow(
                        icon = Icons.Default.Image,
                        title = "Cover Art & Visuals",
                        desc = "Display high-res album covers & atmospheric blurred aurora",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = accentColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionFeatureRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Player Notifications",
                        desc = "Lock screen controls, notification widget & download alerts",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = accentColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Grant All Button
                    Button(
                        onClick = {
                            permissionLauncher.launch(getRequiredPermissions())
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAmoled) accentColor else Color(0xFF165B33)
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                spotColor = Color(0x35165B33)
                            )
                    ) {
                        Text(
                            text = "Grant All Permissions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = "Continue with Basic Features",
                            style = SleekSmallFont.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = textPrimary
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = textSecondary
            )
        }
    }
}
