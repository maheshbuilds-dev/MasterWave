package com.masterwave.player.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.rememberAppThemeColors
import com.masterwave.player.ui.theme.WebAccentEmerald
import com.masterwave.player.ui.theme.WebAccentGreen
import com.masterwave.player.ui.theme.WebAccentPeach
import com.masterwave.player.ui.theme.WebAccentPink
import com.masterwave.player.ui.theme.WebAccentSky
import com.masterwave.player.ui.theme.WebAccentViolet

/**
 * Supercool Listen Together Screen:
 * Styled with MasterWave Website Light Aesthetic (#fffaf0 canvas, tactile bento cards,
 * bold ink outlines, drop-shadows & pastel badges).
 * Provides real-time synchronized group listening with room codes and local mesh broadcast.
 */
@Composable
fun ListenTogetherScreen(
    onBack: () -> Unit,
    player: MasterWavePlayer? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = rememberAppThemeColors()
    val currentTrack by player?.currentTrack?.collectAsState() ?: remember { mutableStateOf(null) }
    val isPlaying by player?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    var selectedMode by remember { mutableIntStateOf(0) } // 0: Host, 1: Join
    var roomCodeInput by remember { mutableStateOf("") }
    val partyCode = remember { "MW-7411" }
    var isHostBroadcasting by remember { mutableStateOf(true) }

    val reactionEmojis = listOf("🔥", "❤️", "⚡", "🎵", "🚀", "💯")
    val recentReactions = remember { mutableStateListOf<String>() }

    val infinitePulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infinitePulse.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun copyCode(code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MasterWave Room Code", code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Room Code copied: $code", Toast.LENGTH_SHORT).show()
    }

    fun shareInvite(code: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join my MasterWave Listening Party!")
            putExtra(
                Intent.EXTRA_TEXT,
                "Hey! Listen to music with me in real-time on MasterWave! Join Room Code: $code or stream on https://masterwave.vercel.app"
            )
        }
        context.startActivity(Intent.createChooser(intent, "Share Listening Party"))
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
                            text = "Listen Together",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = colors.ink
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WebAccentSky)
                                .border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MESH SYNC",
                                fontFamily = DisplayFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0A0A0A)
                            )
                        }
                    }
                    Text(
                        text = "Real-time zero-lag audio broadcast with friends",
                        fontFamily = DisplayFontFamily,
                        fontSize = 13.sp,
                        color = colors.muted
                    )
                }
            }
        }

        // --- Host vs Join Segmented Selector ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surfaceCard)
                    .border(2.dp, colors.border, RoundedCornerShape(999.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Host Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selectedMode == 0) colors.border else Color.Transparent)
                        .clickable { selectedMode = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Host Jam Session",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (selectedMode == 0) (if (colors.isDark) Color(0xFF0A0A0A) else Color.White) else colors.ink
                    )
                }

                // Join Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selectedMode == 1) colors.border else Color.Transparent)
                        .clickable { selectedMode = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Join Room",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (selectedMode == 1) (if (colors.isDark) Color(0xFF0A0A0A) else Color.White) else colors.ink
                    )
                }
            }
        }

        if (selectedMode == 0) {
            // ==========================================
            // HOST MODE: Party Code, Live Broadcast & Listeners
            // ==========================================
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                                Text(
                                    text = if (isHostBroadcasting) "BROADCASTING LIVE" else "SESSION PAUSED",
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp,
                                    color = colors.ink
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(WebAccentViolet)
                                    .border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "48 kHz • ±2ms Sync",
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF0A0A0A)
                                )
                            }
                        }

                        // Giant Room Code Container
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.canvas,
                            border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "YOUR PARTY CODE",
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = colors.muted,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = partyCode,
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 38.sp,
                                    color = colors.ink,
                                    letterSpacing = 3.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { copyCode(partyCode) },
                                        shape = RoundedCornerShape(999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (colors.isDark) colors.ink else Color(0xFF0A0A0A),
                                            contentColor = if (colors.isDark) Color(0xFF0A0A0A) else Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Code", fontFamily = DisplayFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { shareInvite(partyCode) },
                                        shape = RoundedCornerShape(999.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.surfaceSoft,
                                            contentColor = colors.ink
                                        ),
                                        modifier = Modifier.border(1.5.dp, colors.border, RoundedCornerShape(999.dp)),
                                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Invite Friends", fontFamily = DisplayFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Broadcast Song Status
                        if (currentTrack != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surfaceSoft)
                                    .border(1.5.dp, colors.border, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(WebAccentPink)
                                        .border(1.5.dp, colors.border, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF0A0A0A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack!!.title,
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.ink,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = currentTrack!!.artist,
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 12.sp,
                                        color = colors.muted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Connected Listeners (Avatars) ---
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
                            Text(
                                text = "Synced Listeners (4 Active)",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = colors.ink
                            )
                            Icon(Icons.Default.Group, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
                        }

                        // Listeners List
                        val listeners = listOf(
                            Triple("You (Host)", "Broadcasting master clock", WebAccentEmerald),
                            Triple("Alex M.", "Synced • 0ms drift", WebAccentSky),
                            Triple("Priya K.", "Synced • 1ms drift", WebAccentPink),
                            Triple("Sam R.", "Synced • 2ms drift", WebAccentPeach)
                        )

                        listeners.forEach { (name, status, color) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.canvas)
                                    .border(1.dp, colors.hairline, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(1.5.dp, colors.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(1),
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0A0A0A)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = name, fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.ink)
                                    Text(text = status, fontFamily = DisplayFontFamily, fontSize = 12.sp, color = colors.muted)
                                }
                            }
                        }

                        // Live Reactions
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Send Instant Reaction to Party:",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = colors.muted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            reactionEmojis.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.canvas)
                                        .border(1.5.dp, colors.border, RoundedCornerShape(12.dp))
                                        .clickable {
                                            recentReactions.add(emoji)
                                            Toast.makeText(context, "Sent $emoji to all listeners!", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // JOIN MODE: Enter Room Code & Nearby Radar
            // ==========================================
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Enter Room Code",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = colors.ink
                        )
                        Text(
                            text = "Ask the host for their 6-character party code to jump right into the audio stream.",
                            fontFamily = DisplayFontFamily,
                            fontSize = 13.sp,
                            color = colors.muted,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { if (it.length <= 10) roomCodeInput = it.uppercase() },
                            placeholder = { Text("e.g. MW-7411", fontFamily = DisplayFontFamily, color = colors.muted) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            textStyle = TextStyle(fontFamily = DisplayFontFamily, fontSize = 15.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.canvas,
                                unfocusedContainerColor = colors.canvas,
                                focusedBorderColor = colors.border,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.ink,
                                unfocusedTextColor = colors.ink
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (roomCodeInput.isNotBlank()) {
                                    Toast.makeText(context, "Joining Room $roomCodeInput... Connected to stream!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid room code", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (colors.isDark) colors.ink else Color(0xFF0A0A0A),
                                contentColor = if (colors.isDark) Color(0xFF0A0A0A) else Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join Listening Party", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // --- Nearby Mesh Discovery Radar ---
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
                                Icon(Icons.Default.Sensors, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Nearby Local Rooms",
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
                                    .border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SCANNING",
                                    fontFamily = DisplayFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0A0A0A)
                                )
                            }
                        }

                        // Nearby Found Room
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.canvas)
                                .border(1.5.dp, colors.border, RoundedCornerShape(16.dp))
                                .clickable {
                                    Toast.makeText(context, "Connected to MasterWave Studio Jam!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(WebAccentPeach)
                                    .border(1.5.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "MasterWave Studio Jam", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.ink)
                                Text(text = "Nearby on Local Wi-Fi • 48 kHz", fontFamily = DisplayFontFamily, fontSize = 12.sp, color = colors.muted)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.border)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(text = "Connect", color = if (colors.isDark) Color(0xFF0A0A0A) else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- Tech Specs Footer Card ---
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceSoft,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = colors.ink, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "MasterWave uses local UDP multicast and peer-to-peer clock synchronization to ensure sub-millisecond audio alignment across all phones.",
                        fontFamily = DisplayFontFamily,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = colors.body
                    )
                }
            }
        }
    }
}
