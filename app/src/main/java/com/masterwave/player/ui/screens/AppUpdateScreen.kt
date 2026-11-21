package com.masterwave.player.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.rememberAppThemeColors
import com.masterwave.player.ui.theme.WebAccentEmerald
import com.masterwave.player.ui.theme.WebAccentGreen
import com.masterwave.player.ui.theme.WebAccentOchre
import com.masterwave.player.ui.theme.WebAccentPeach
import com.masterwave.player.ui.theme.WebAccentPink
import com.masterwave.player.ui.theme.WebAccentSky
import com.masterwave.player.ui.theme.WebAccentViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Supercool App Update Screen:
 * Styled with MasterWave Website Warm Light Aesthetic (#fffaf0 canvas, tactile bento cards,
 * bold ink outlines, drop-shadows & pastel badges).
 * Provides interactive release checking, official website mirrors,
 * changelog breakdown, and direct production APK downloads.
 */
@Composable
fun AppUpdateScreen(
    onBack: () -> Unit,
    updateManager: com.masterwave.player.data.update.AppUpdateManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = rememberAppThemeColors()

    val actualUpdateManager = remember {
        updateManager ?: com.masterwave.player.data.update.AppUpdateManager.getInstance(
            context,
            com.masterwave.player.MasterWaveApp.instance.okHttpClient
        )
    }

    val availableUpdate by actualUpdateManager.availableUpdate.collectAsState()
    val downloadProgress by actualUpdateManager.downloadProgress.collectAsState()
    val isInstalling by actualUpdateManager.isInstalling.collectAsState()
    val isChecking by actualUpdateManager.isChecking.collectAsState()

    var lastCheckedStatus by remember {
        mutableStateOf("Up to date • v${com.masterwave.player.BuildConfig.VERSION_NAME} Production")
    }
    var autoCheckWifi by remember { mutableStateOf(true) }
    var notifyNewReleases by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "update_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun checkForUpdates() {
        if (isChecking) return
        actualUpdateManager.checkForUpdate(silent = false) { update ->
            if (update != null) {
                lastCheckedStatus = "Update Available: v${update.versionName} • Tap Download Below"
                Toast.makeText(context, "New MasterWave v${update.versionName} available!", Toast.LENGTH_SHORT).show()
            } else {
                lastCheckedStatus = "Checked just now • You are on the latest v${com.masterwave.player.BuildConfig.VERSION_NAME}"
                Toast.makeText(context, "MasterWave is up to date!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- Top Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, colors.border, CircleShape)
                        .background(colors.surfaceSoft, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "App Updates",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = colors.ink
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WebAccentGreen)
                                .border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "v2.0.0 STABLE",
                                fontFamily = DisplayFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0A0A0A)
                            )
                        }
                    }
                    Text(
                        text = "Official release channel & website mirrors",
                        fontFamily = DisplayFontFamily,
                        fontSize = 13.sp,
                        color = colors.muted
                    )
                }
            }
        }

        // --- Current Version Hero Card ---
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(WebAccentEmerald)
                            .border(2.5.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF0A0A0A),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MasterWave v${com.masterwave.player.BuildConfig.VERSION_NAME}",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = colors.ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Build 2026 • 48 kHz Studio Architecture",
                            fontFamily = DisplayFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.muted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lastCheckedStatus,
                            fontFamily = DisplayFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WebAccentGreen
                        )
                    }

                    // Update Check Pill Button (Black button, bold white text)
                    Button(
                        onClick = { checkForUpdates() },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0A0A0A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .then(if (isChecking) Modifier.rotate(rotation) else Modifier)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isChecking) "Checking Updates..." else "Update Check",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- Share MasterWave Card (Replaces Standalone APK Download) ---
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WebAccentEmerald)
                                    .border(1.5.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = "Share MasterWave",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.ink
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WebAccentGreen)
                                .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = "Invite Friends", fontFamily = DisplayFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A0A0A))
                        }
                    }

                    Text(
                        text = "Share MasterWave with friends, family, and music lovers so they can experience pure, ad-free studio audio playback and live Listen Together music rooms.",
                        fontFamily = DisplayFontFamily,
                        fontSize = 13.sp,
                        color = colors.body,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "MasterWave Studio Player")
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Hey! Try MasterWave — the ultimate music player with Listen Together, high-fidelity sound, and zero ads: https://masterwave.vercel.app"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share MasterWave via"))
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0A0A0A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, colors.border, RoundedCornerShape(999.dp))
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share App with Friends",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- What's New Bento Breakdown ---
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "What's New in v2.0.0",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.ink
                    )

                    val changelog = listOf(
                        Triple("Apple Music Master Audio", "Tuned DSP acoustic profile with 32-bit float output & wide binaural soundstage.", WebAccentViolet),
                        Triple("100% Ad-Free Forever", "Zero audio ads, zero banners, zero promotional trackers.", WebAccentEmerald),
                        Triple("Real-Time Synced Lyrics", "Integrated LRCLIB synchronized lyrics with verse seek.", WebAccentPink),
                        Triple("Adaptive Bento UI System", "Smooth tactile surfaces with dynamic dark and light mode adaptation.", WebAccentOchre),
                        Triple("Listen Together Mesh Sync", "Real-time party broadcast with sub-millisecond clock sync.", WebAccentSky)
                    )

                    changelog.forEach { (title, desc, accent) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.hairline, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                                    .border(1.5.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "✓", fontFamily = DisplayFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF0A0A0A))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = title, fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.ink)
                                Text(text = desc, fontFamily = DisplayFontFamily, fontSize = 12.sp, color = colors.muted, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- Official Website Mirrors Card ---
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceSoft,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Official Release Mirrors",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.ink
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl("https://masterwave.vercel.app") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "https://masterwave.vercel.app", fontFamily = DisplayFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.ink)
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl("https://masterwave-music.vercel.app") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "https://masterwave-music.vercel.app", fontFamily = DisplayFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.ink)
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl("https://masterwave.shark.hns.to") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "https://masterwave.shark.hns.to (Web3)", fontFamily = DisplayFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.ink)
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // --- Settings Toggles ---
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Auto-Check on Wi-Fi", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.ink)
                            Text(text = "Silently check for new versions on connection", fontFamily = DisplayFontFamily, fontSize = 12.sp, color = colors.muted)
                        }
                        Switch(
                            checked = autoCheckWifi,
                            onCheckedChange = { autoCheckWifi = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.border,
                                uncheckedThumbColor = colors.muted,
                                uncheckedTrackColor = colors.canvas
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Release Notifications", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.ink)
                            Text(text = "Notify when a new stable version is available", fontFamily = DisplayFontFamily, fontSize = 12.sp, color = colors.muted)
                        }
                        Switch(
                            checked = notifyNewReleases,
                            onCheckedChange = { notifyNewReleases = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.border,
                                uncheckedThumbColor = colors.muted,
                                uncheckedTrackColor = colors.canvas
                            )
                        )
                    }
                }
            }
        }
    }
}
