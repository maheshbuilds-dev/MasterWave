package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.data.backup.BackupManager
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.SleekSmallFont
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val themeMode by ThemeState.themeMode.collectAsState()

    val favorites by repository.favorites.collectAsState()
    val playlists by repository.playlists.collectAsState()

    val backupManager = remember { BackupManager(context, repository, player) }
    var lastBackupMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // SAF File Creator for Export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                val success = backupManager.exportToUri(uri)
                isProcessing = false
                if (success) {
                    lastBackupMessage = "Backup exported successfully!"
                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to export backup", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // SAF File Opener for Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                val success = backupManager.restoreFromUri(uri)
                isProcessing = false
                if (success) {
                    lastBackupMessage = "Backup imported! Settings, playlists & favorites restored."
                    Toast.makeText(context, "Settings & Library Restored!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Invalid .mwbackup file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFFFAF0)
    val cardBg = if (isAmoled) Color(0xFF101018) else Color(0xFFF5F0E0)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF0A0A0A)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF6A6A6A)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Backup & Restore",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "Save and migrate all settings, themes, and playlists",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }
                }
            }

            // Status Banner if an operation finished
            if (!lastBackupMessage.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF165B33).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF165B33).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = lastBackupMessage!!,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                ),
                                color = textPrimary
                            )
                        }
                    }
                }
            }

            // --- 1. Export Backup Card ---
            item {
                BackupActionCard(
                    title = "Export Backup",
                    subtitle = "Generates a portable .mwbackup file containing all your theme modes, colors, audio engine settings, custom playlists, and favorite songs.",
                    icon = Icons.Default.FileUpload,
                    buttonLabel = "Create & Export (.mwbackup)",
                    cardBg = cardBg,
                    accentColor = accentColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isProcessing = isProcessing,
                    onClick = {
                        val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        exportLauncher.launch("MasterWave_Backup_$dateTag.mwbackup")
                    }
                )
            }

            // Quick Direct Save to Downloads option
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                isProcessing = true
                                try {
                                    val f = backupManager.exportToDownloads()
                                    lastBackupMessage = "Saved directly to Downloads: ${f.name}"
                                    Toast.makeText(context, "Saved to Downloads: ${f.name}", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                                isProcessing = false
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quick Export to Downloads",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                            Text(
                                text = "Automatically save to phone's public /Downloads folder",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = textSecondary
                            )
                        }
                    }
                }
            }

            // --- 2. Import & Restore Backup Card ---
            item {
                BackupActionCard(
                    title = "Import & Restore Backup",
                    subtitle = "Select an existing .mwbackup file to immediately restore your preferences, theme layout, playlists, and listening history.",
                    icon = Icons.Default.SettingsBackupRestore,
                    buttonLabel = "Choose Backup File (.mwbackup)",
                    cardBg = cardBg,
                    accentColor = Color(0xFF165B33),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isProcessing = isProcessing,
                    onClick = {
                        importLauncher.launch(arrayOf("*/*"))
                    }
                )
            }

            // --- 3. Current System State Overview ---
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Current Backup Scope",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                        }

                        DataRow("Theme Mode", themeMode.name, textPrimary, textSecondary)
                        DataRow("Active Playlists", "${playlists.size} playlists", textPrimary, textSecondary)
                        DataRow("Favorited Songs", "${favorites.size} tracks", textPrimary, textSecondary)
                        DataRow("Liquid Glass Dock", if (ThemeState.liquidGlassEnabled.collectAsState().value) "Enabled" else "Off", textPrimary, textSecondary)
                        DataRow("Audio Engine", "Studio Engine • 320 kbps High-Res", textPrimary, textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    buttonLabel: String,
    cardBg: Color,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardBg,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Launcher-standard snapshot",
                        style = SleekSmallFont.copy(fontSize = 11.sp),
                        color = textSecondary
                    )
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = textSecondary
            )

            Button(
                onClick = onClick,
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = buttonLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DataRow(label: String, value: String, textPrimary: Color, textSecondary: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = textSecondary)
        Text(text = value, style = SleekSmallFont.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp), color = textPrimary)
    }
}
