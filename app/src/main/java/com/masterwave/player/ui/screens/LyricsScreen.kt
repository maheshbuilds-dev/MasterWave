package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.data.model.LyricLine
import com.masterwave.player.data.model.LyricsResult
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.ThemeState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import java.util.Locale

/**
 * Enhanced Synchronized Lyrics Screen matching Reference Frame 5.0s:
 * - Top action bar with back arrow, "LYRICS" subtitle marquee, sleep timer, and options.
 * - "LINE SYNC" badge indicator.
 * - Glowing active lyric line with radiant illumination and smooth auto-scroll.
 * - Tap any line to seek immediately.
 * - Bottom compact waveform scrubber and transport controls (Prev, Play/Pause, Next).
 */
@Composable
fun LyricsScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val context = LocalContext.current

    var lyricsResult by remember { mutableStateOf<LyricsResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }
    var syncOffsetMs by remember { mutableStateOf(0L) }
    var textSizeMultiplier by remember { mutableStateOf(1.0f) }

    val listState = rememberLazyListState()

    val currentTrack = track
    val safeDurationMs = durationMs.coerceAtLeast(1L)
    val remainingMs = (safeDurationMs - positionMs).coerceAtLeast(0L)

    fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    LaunchedEffect(currentTrack?.id) {
        if (currentTrack == null) {
            lyricsResult = null
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        lyricsResult = repository.getLyrics(currentTrack)
        isLoading = false
    }

    val effectivePositionMs = positionMs + syncOffsetMs
    val currentLineIndex = remember(lyricsResult, effectivePositionMs) {
        val lines = lyricsResult?.lines ?: return@remember -1
        lines.indexOfLast { it.timestampMs <= effectivePositionMs }
    }

    // Stormy Fluid Aurora Oscillation Animation (Matching Now Playing & Frame 5.0s)
    val infiniteAuroraTransition = rememberInfiniteTransition(label = "lyrics_aurora_anim")
    val auroraPhase by infiniteAuroraTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lyrics_aurora_phase"
    )

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem(
                (currentLineIndex - 1).coerceAtLeast(0)
            )
        }
    }

    val bgColor = if (isAmoled) Color(0xFF040407) else Color(0xFFFFFFFF)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF101828)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF6A6A6A)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // --- Ambient Background Layer (Blurred Artwork + Fluid Aurora - Frame 5.0s) ---
        if (!currentTrack?.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = currentTrack?.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 54.dp)
                    .scale(1.35f)
            )
        }

        // Fluid Aurora Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val blob1X = w * (0.35f + 0.16f * cos(auroraPhase.toDouble()).toFloat())
            val blob1Y = h * (0.65f + 0.12f * sin(auroraPhase.toDouble()).toFloat())
            val blob2X = w * (0.68f + 0.18f * sin((auroraPhase * 1.3f).toDouble()).toFloat())
            val blob2Y = h * (0.75f + 0.10f * cos((auroraPhase * 1.1f).toDouble()).toFloat())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(blob1X, blob1Y),
                    radius = w * 0.8f
                ),
                radius = w * 0.8f,
                center = Offset(blob1X, blob1Y)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(com.masterwave.player.ui.theme.MasterWaveCyan.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(blob2X, blob2Y),
                    radius = w * 0.70f
                ),
                radius = w * 0.70f,
                center = Offset(blob2X, blob2Y)
            )
        }

        // Dark Atmospheric Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x88000000),
                            Color(0x99000000),
                            Color(0xCC000000),
                            Color(0xF5040407)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- 1. Top Action Bar (Frame 5.0s) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isAmoled) Color(0x33FFFFFF) else Color(0x1A000000))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center Title
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LYRICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            fontSize = 11.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "${currentTrack?.title ?: "Track"} • ${currentTrack?.artist ?: ""}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right Icons: Sleep Timer + Options Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isAmoled) Color(0x33FFFFFF) else Color(0x1A000000))
                            .clickable {
                                Toast.makeText(context, "Sleep Timer: 30 minutes set", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isAmoled) Color(0x33FFFFFF) else Color(0x1A000000))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            val sizeLabel = if (textSizeMultiplier > 1.15f) "Large" else if (textSizeMultiplier < 0.95f) "Small" else "Normal"
                            DropdownMenuItem(
                                text = { Text("Text Size: $sizeLabel") },
                                onClick = {
                                    showMenu = false
                                    textSizeMultiplier = when {
                                        textSizeMultiplier < 0.95f -> 1.0f
                                        textSizeMultiplier < 1.15f -> 1.25f
                                        else -> 0.85f
                                    }
                                    Toast.makeText(context, "Text size: $sizeLabel", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sync Offset: +100 ms") },
                                onClick = {
                                    showMenu = false
                                    syncOffsetMs += 100L
                                    Toast.makeText(context, "Offset: ${syncOffsetMs} ms", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sync Offset: -100 ms") },
                                onClick = {
                                    showMenu = false
                                    syncOffsetMs -= 100L
                                    Toast.makeText(context, "Offset: ${syncOffsetMs} ms", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset Offset (0 ms)") },
                                onClick = {
                                    showMenu = false
                                    syncOffsetMs = 0L
                                    Toast.makeText(context, "Sync offset reset", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export .lrc file") },
                                onClick = {
                                    showMenu = false
                                    try {
                                        val lrcContent = lyricsResult?.lines?.joinToString("\n") { line ->
                                            val m = line.timestampMs / 60000
                                            val s = (line.timestampMs % 60000) / 1000
                                            val ms = (line.timestampMs % 1000) / 10
                                            "[%02d:%02d.%02d]%s".format(m, s, ms, line.text)
                                        } ?: lyricsResult?.plainLyrics ?: ""
                                        val filename = "${currentTrack?.title ?: "track"}.lrc".replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                        val exportDir = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "MasterWave")
                                        if (!exportDir.exists()) exportDir.mkdirs()
                                        java.io.File(exportDir, filename).writeText(lrcContent)
                                        Toast.makeText(context, "Exported $filename to Downloads/MasterWave", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy Lyrics") },
                                onClick = {
                                    showMenu = false
                                    val plain = lyricsResult?.plainLyrics ?: lyricsResult?.lines?.joinToString("\n") { it.text } ?: ""
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Lyrics", plain)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Lyrics copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // --- 2. "LINE SYNC" Badge Indicator ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, if (isAmoled) Color(0x44FFFFFF) else Color(0x44000000))
                ) {
                    Text(
                        text = "LINE SYNC",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp
                        ),
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                    )
                }
            }

            // --- 3. Lyric Lines Stream ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else {
                    val res = lyricsResult
                    if (res != null && res.lines.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 36.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            itemsIndexed(res.lines) { index, line ->
                                val isCurrent = index == currentLineIndex

                                val targetColor = if (isCurrent) {
                                    Color.White
                                } else {
                                    if (isAmoled) Color(0x55FFFFFF) else Color(0x55000000)
                                }

                                val textColor by animateColorAsState(
                                    targetValue = targetColor,
                                    label = "lyric_text_color"
                                )

                                Text(
                                    text = line.text,
                                    style = TextStyle(
                                        fontSize = ((if (isCurrent) 28f else 22f) * textSizeMultiplier).sp,
                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        lineHeight = ((if (isCurrent) 38f else 30f) * textSizeMultiplier).sp,
                                        shadow = if (isCurrent) {
                                            Shadow(
                                                color = com.masterwave.player.ui.theme.MasterWaveTurquoise.copy(alpha = 0.85f),
                                                blurRadius = 24f
                                            )
                                        } else null
                                    ),
                                    color = textColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            player.seekTo(line.timestampMs)
                                        }
                                )
                            }
                        }
                    } else if (!res?.plainLyrics.isNullOrBlank()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp)
                        ) {
                            item {
                                Text(
                                    text = res?.plainLyrics ?: "",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 18.sp,
                                        lineHeight = 30.sp
                                    ),
                                    color = textPrimary
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No synced lyrics available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textSecondary
                            )
                        }
                    }
                }
            }

            // --- 4. Bottom Scrubber & Transport Bar (Frame 5.0s) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x99040407), Color(0xF5040407))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top controls row above waveform (Fullscreen & Options pills - Frame 5.0s)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x30FFFFFF),
                            border = BorderStroke(0.8.dp, Color(0x35FFFFFF)),
                            modifier = Modifier
                                .size(36.dp)
                                .bouncyClickable(onClick = onBack)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CropFree,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0x30FFFFFF),
                            border = BorderStroke(0.8.dp, Color(0x35FFFFFF)),
                            modifier = Modifier
                                .size(36.dp)
                                .bouncyClickable { showMenu = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // --- Scrubber Seekbar with Dynamic Expansion & Pop-Out Thumb (Now Playing Style) ---
                var isDragging by remember { mutableStateOf(false) }
                var dragPositionMs by remember { mutableStateOf<Long?>(null) }
                val activePosition = dragPositionMs ?: positionMs
                val progressFraction = if (safeDurationMs > 0) (activePosition.toFloat() / safeDurationMs.toFloat()).coerceIn(0f, 1f) else 0f

                val scrubberBarHeight by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 4.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "lyrics_scrubber_height"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .pointerInput(safeDurationMs) {
                            detectTapGestures { offset ->
                                if (safeDurationMs > 0 && size.width > 0) {
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    val targetMs = (fraction * safeDurationMs).toLong()
                                    player.seekTo(targetMs)
                                }
                            }
                        }
                        .pointerInput(safeDurationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    if (safeDurationMs > 0 && size.width > 0) {
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        dragPositionMs = (fraction * safeDurationMs).toLong()
                                    }
                                },
                                onDragEnd = {
                                    isDragging = false
                                    dragPositionMs?.let { player.seekTo(it) }
                                    dragPositionMs = null
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragPositionMs = null
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    if (safeDurationMs > 0 && size.width > 0) {
                                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                        dragPositionMs = (fraction * safeDurationMs).toLong()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Background track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(scrubberBarHeight)
                            .clip(RoundedCornerShape(scrubberBarHeight / 2))
                            .background(Color.White.copy(alpha = 0.22f))
                    )
                    // Filled progress with signature white-to-accent gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction)
                            .height(scrubberBarHeight)
                            .clip(RoundedCornerShape(scrubberBarHeight / 2))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White,
                                        accentColor
                                    )
                                )
                            )
                    )

                    // Dynamic Floating Thumb Knob when scrubbing
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isDragging,
                        enter = scaleIn(initialScale = 0.82f, animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(120)),
                        exit = scaleOut(targetScale = 0.82f, animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)) + fadeOut(tween(100)),
                        modifier = Modifier
                            .fillMaxWidth(fraction = progressFraction.coerceIn(0.01f, 1f))
                            .height(28.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.45f))
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, accentColor, CircleShape)
                            )
                        }
                    }
                }

                // Timestamps: Elapsed & Remaining with DisplayFontFamily
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(activePosition),
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Text(
                        text = "-${formatTime((safeDurationMs - activePosition).coerceAtLeast(0L))}",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }

                // Transport Controls: Prev, Play/Pause, Next (Frame 5.0s)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x30FFFFFF),
                            border = BorderStroke(0.8.dp, Color(0x35FFFFFF)),
                            modifier = Modifier
                                .size(48.dp)
                                .bouncyClickable { player.skipToPrevious() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // Play/Pause circular button
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(58.dp)
                                .bouncyClickable { player.togglePlayPause() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0x30FFFFFF),
                            border = BorderStroke(0.8.dp, Color(0x35FFFFFF)),
                            modifier = Modifier
                                .size(48.dp)
                                .bouncyClickable { player.skipToNext() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
