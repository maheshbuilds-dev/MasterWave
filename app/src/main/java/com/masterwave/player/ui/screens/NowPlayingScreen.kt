package com.masterwave.player.ui.screens

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.MasterWaveApp
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.AudioPlaybackModal
import com.masterwave.player.ui.components.GlassyStyle
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.components.glassySurface
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.ThemeState
import com.masterwave.player.ui.theme.SwissMono

/**
 * NOW PLAYING SCREEN
 * Apple Music Full-Bleed Artwork with Downward Bleed Effect:
 * - Edge-to-edge full width artwork header with consistent thumbnail presentation for every song
 * - No breathing animation (artwork is stable and sharp)
 * - Deep downward bleed effect dissolving the lower portion of the thumbnail into the matching ambient backdrop
 * - Clean song metadata (Title, Artist, Star, 3-Dots)
 * - Scrubber seekbar with elapsed and "-remaining" time formatted labels
 * - Centered triple playback controls (FastRewind, Play/Pause, FastForward)
 * - Bottom utilities bar (Lyrics, Audio/EQ, Queue)
 * - Downward swipe gesture & top grab bar to collapse
 */
@Composable
fun NowPlayingScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onCollapse: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val isLoading by player.isLoading.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val queue by player.queue.collectAsState()

    val currentTrack = track ?: return
    val isFavorite = favorites.any { it.id == currentTrack.id }
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()
    val accentColor by ThemeState.accentColor.collectAsState()
    val currentArtworkPalette by ThemeState.currentArtworkPalette.collectAsState()

    // Dynamic Artwork Bleed & Ambient Pool Colors:
    // Uses the actual bottom slice color of the current track's thumbnail for a seamless downward bleed,
    // and lively ambient glow pool behind controls.
    val targetBottomBleed = currentArtworkPalette?.bottomBleedColor ?: Color(
        red = (accentColor.red * 0.28f + 0.03f).coerceIn(0f, 1f),
        green = (accentColor.green * 0.28f + 0.03f).coerceIn(0f, 1f),
        blue = (accentColor.blue * 0.28f + 0.03f).coerceIn(0f, 1f),
        alpha = 1f
    )
    val targetAmbientGlow = currentArtworkPalette?.ambientGlowColor ?: accentColor

    val animatedBottomBleed by animateColorAsState(
        targetValue = targetBottomBleed,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 500, easing = FastOutSlowInEasing),
        label = "animatedBottomBleed"
    )
    val animatedAmbientGlow by animateColorAsState(
        targetValue = targetAmbientGlow,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 500, easing = FastOutSlowInEasing),
        label = "animatedAmbientGlow"
    )

    var showMenu by remember { mutableStateOf(false) }
    var showAudioPlaybackModal by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    val playlists by repository.playlists.collectAsState()

    val artworkContent = remember(currentTrack.id, currentTrack.title, currentTrack.artworkUrl) {
        ArtworkContent(currentTrack.id, currentTrack.title, currentTrack.artworkUrl)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(animatedBottomBleed)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 45f) {
                        onCollapse()
                    }
                }
            }
    ) {
        val screenHeight = maxHeight

        // --- 1. Background Ambient Radiant Canvas ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base top-to-bottom gradient
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color(0xFF07080B),
                    0.45f to animatedBottomBleed.copy(alpha = 0.75f),
                    1.0f to animatedBottomBleed
                )
            )

            // Radiant ambient light pool near the lower controls
            val glowCenter = Offset(w * 0.5f, h * 0.78f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedAmbientGlow.copy(alpha = 0.32f),
                        animatedBottomBleed.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = glowCenter,
                    radius = w * 0.88f
                ),
                radius = w * 0.88f,
                center = glowCenter
            )
        }

        // --- 2. Full-Bleed Artwork Hero with Downward Bleed Effect (Edge-to-Edge) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.56f)
        ) {
            Crossfade(
                targetState = artworkContent,
                animationSpec = tween(
                    durationMillis = if (reduceMotion) 100 else 220,
                    easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
                ),
                label = "track_artwork"
            ) { content ->
                val contentModel = remember(content.trackId, content.artworkUrl) {
                    resolveArtworkModel(content.artworkUrl)
                }
                if (contentModel != null) {
                    AsyncImage(
                        model = contentModel,
                        contentDescription = content.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF161822), animatedBottomBleed)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            // Top subtle vignette for status bar icon legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.50f),
                            1.0f to Color.Transparent
                        )
                    )
            )

            // The Downward Bleed Gradient:
            // Smoothly and gradually bleeds the lower portion of the artwork down into the matching background tone
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.34f to Color.Transparent,
                            0.54f to animatedBottomBleed.copy(alpha = 0.30f),
                            0.70f to animatedBottomBleed.copy(alpha = 0.70f),
                            0.86f to animatedBottomBleed.copy(alpha = 0.92f),
                            0.96f to animatedBottomBleed,
                            1.00f to animatedBottomBleed
                        )
                    )
            )
        }

        // --- 3. Structured Foreground Controls ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Collapse Drag Handle with Generous Touch Target
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .bouncyClickable(scaleDown = 0.95f, onClick = onCollapse),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.45f))
                )
            }

            // Spacer allocating visual presence for the upper artwork bleed
            Spacer(modifier = Modifier.weight(1f))

            // Track Details Row: Title, Artist, Star, 3-Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = currentTrack.title,
                        transitionSpec = {
                            (fadeIn(tween(220, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))) + slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 4 })
                                .togetherWith(fadeOut(tween(140)) + slideOutVertically { -it / 4 })
                        },
                        label = "track_title_anim"
                    ) { title ->
                        Text(
                            text = title,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    AnimatedContent(
                        targetState = currentTrack.artist,
                        transitionSpec = {
                            (fadeIn(tween(220, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f))) + slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it / 4 })
                                .togetherWith(fadeOut(tween(140)) + slideOutVertically { -it / 4 })
                        },
                        label = "track_artist_anim"
                    ) { artist ->
                        Text(
                            text = artist,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            letterSpacing = (-0.1).sp,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Star & More Options Cluster (Pixel-iOS Glass Squircles)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Star (Favorite) Button with dynamic spring pop
                    var starPulse by remember { mutableFloatStateOf(1f) }
                    var lastFavoriteState by remember { mutableStateOf(isFavorite) }
                    LaunchedEffect(isFavorite) {
                        if (isFavorite != lastFavoriteState) {
                            lastFavoriteState = isFavorite
                            if (isFavorite) {
                                starPulse = 1.30f
                                delay(120)
                                starPulse = 1f
                            }
                        }
                    }
                    val animatedStarScale by animateFloatAsState(
                        targetValue = starPulse,
                        animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow),
                        label = "star_pop"
                    )
                    val animatedStarColor by animateColorAsState(
                        targetValue = if (isFavorite) Color(0xFFFFD54F) else Color.White,
                        animationSpec = tween(durationMillis = 180),
                        label = "star_color"
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.35f))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.38f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .bouncyClickable(scaleDown = 0.94f, onClick = { repository.toggleFavorite(currentTrack) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = animatedStarScale
                                scaleY = animatedStarScale
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = isFavorite,
                                transitionSpec = {
                                    (scaleIn(spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(120)))
                                        .togetherWith(scaleOut(tween(90)) + fadeOut(tween(90)))
                                },
                                label = "star_icon_swap"
                            ) { fav ->
                                Icon(
                                    imageVector = if (fav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = animatedStarColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // 3-Dots Options Menu Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.35f))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.38f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .bouncyClickable(scaleDown = 0.94f, onClick = { showMenu = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More Options",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .glassySurface(
                                    shape = RoundedCornerShape(18.dp),
                                    style = GlassyStyle.SURFACE,
                                    elevation = 16.dp
                                )
                        ) {
                            DropdownMenuItem(
                                text = { Text("Queue (${queue.size})", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = accentColor) },
                                onClick = {
                                    showMenu = false
                                    onOpenQueue()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Lyrics", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = accentColor) },
                                onClick = {
                                    showMenu = false
                                    onOpenLyrics()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isShuffle) "Shuffle: On" else "Shuffle: Off", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null, tint = if (isShuffle) accentColor else Color.White.copy(alpha = 0.6f)) },
                                onClick = {
                                    showMenu = false
                                    player.toggleShuffle()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Repeat Mode", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, contentDescription = null, tint = accentColor) },
                                onClick = {
                                    showMenu = false
                                    player.cycleRepeatMode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Audio settings", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = accentColor) },
                                onClick = {
                                    showMenu = false
                                    showAudioPlaybackModal = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = accentColor) },
                                onClick = {
                                    showMenu = false
                                    showAddToPlaylistDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Download", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null, tint = accentColor) },
                                onClick = {
                                    showMenu = false
                                    val dm = MusicDownloadManager.getInstance(
                                        context,
                                        MasterWaveApp.instance.okHttpClient,
                                        repository
                                    )
                                    dm.downloadTrack(currentTrack)
                                    Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("More like this", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = accentColor) },
                                onClick = {
                                    showMenu = false
                                    repository.recommendationManager.onMoreLikeThis(currentTrack)
                                    Toast.makeText(context, "More like ${currentTrack.artist}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Don't recommend artist", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null, tint = Color(0xFFF43F5E)) },
                                onClick = {
                                    showMenu = false
                                    repository.recommendationManager.onDontRecommendArtist(currentTrack.artist)
                                    Toast.makeText(context, "Artist hidden from recommendations", Toast.LENGTH_SHORT).show()
                                    player.skipToNext()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hide track", color = Color.White, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = Color(0xFFF43F5E)) },
                                onClick = {
                                    showMenu = false
                                    repository.recommendationManager.onHideTrack(currentTrack)
                                    Toast.makeText(context, "Track hidden", Toast.LENGTH_SHORT).show()
                                    player.skipToNext()
                                }
                            )
                        }
                    }
                }
            }

            // --- Scrubber Seekbar with Dynamic Expansion & Thumb (Pixel-iOS Hybrid) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .padding(top = 14.dp, bottom = 4.dp)
            ) {
                var isDragging by remember { mutableStateOf(false) }
                var dragPositionMs by remember { mutableStateOf<Long?>(null) }
                val activePosition = dragPositionMs ?: positionMs
                val progressFraction = if (durationMs > 0) (activePosition.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

                val scrubberBarHeight by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 4.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "scrubber_height"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .pointerInput(durationMs) {
                            detectTapGestures { offset ->
                                if (durationMs > 0 && size.width > 0) {
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    val targetMs = (fraction * durationMs).toLong()
                                    player.seekTo(targetMs)
                                }
                            }
                        }
                        .pointerInput(durationMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    if (durationMs > 0 && size.width > 0) {
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        dragPositionMs = (fraction * durationMs).toLong()
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
                                    if (durationMs > 0 && size.width > 0) {
                                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                        dragPositionMs = (fraction * durationMs).toLong()
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
                    // Filled progress
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

                    // Dynamic Floating Thumb Knob when scrubbing with organic spring pop
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isDragging,
                        enter = scaleIn(spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(120)),
                        exit = scaleOut(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium)) + fadeOut(tween(100)),
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

                // Timestamps: 0:42 and -2:45
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
                        color = Color.White.copy(alpha = 0.60f)
                    )

                    val remainingMs = (durationMs - activePosition).coerceAtLeast(0L)
                    Text(
                        text = "-${formatTime(remainingMs)}",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }
            }

            // --- Playback Controls: Apple Music Tactile Transport Cluster (Shuffle, Prev, Play/Pause, Next, Repeat) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button (Tactile Squircle)
                val animatedShuffleTint by animateColorAsState(
                    targetValue = if (isShuffle) accentColor else Color.White.copy(alpha = 0.65f),
                    animationSpec = tween(180),
                    label = "shuffle_tint"
                )
                val animatedShuffleBg by animateColorAsState(
                    targetValue = if (isShuffle) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                    animationSpec = tween(180),
                    label = "shuffle_bg"
                )
                val animatedShuffleBorder by animateColorAsState(
                    targetValue = if (isShuffle) accentColor.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.15f),
                    animationSpec = tween(180),
                    label = "shuffle_border"
                )
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(animatedShuffleBg)
                        .border(
                            width = 1.dp,
                            color = animatedShuffleBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .bouncyClickable(scaleDown = 0.94f) { player.toggleShuffle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = animatedShuffleTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous Track (Tactile Frosted Squircle)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.35f))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.32f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .bouncyClickable(scaleDown = 0.93f) { player.skipToPrevious() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastRewind,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause (Large Frosted Liquid Glass Pod with Ambient Glow)
                val playShadowElevation by animateDpAsState(
                    targetValue = if (isPlaying) 18.dp else 12.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "play_shadow"
                )
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .shadow(
                            elevation = playShadowElevation,
                            shape = RoundedCornerShape(26.dp),
                            spotColor = accentColor.copy(alpha = if (isPlaying) 0.60f else 0.35f),
                            ambientColor = Color.Black.copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.88f),
                                    accentColor.copy(alpha = 0.75f)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.75f),
                                    Color.White.copy(alpha = 0.20f)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .bouncyClickable(scaleDown = 0.92f) { player.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = Pair(isLoading, isPlaying),
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(140)))
                                .togetherWith(scaleOut(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)) + fadeOut(tween(100)))
                        },
                        label = "play_pause_content"
                    ) { (loading, playing) ->
                        if (loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(34.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Next Track (Tactile Frosted Squircle)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(0.35f))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.32f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .bouncyClickable(scaleDown = 0.93f) { player.skipToNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FastForward,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Repeat Button (Tactile Squircle)
                val repeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
                val animatedRepeatTint by animateColorAsState(
                    targetValue = if (repeatActive) accentColor else Color.White.copy(alpha = 0.65f),
                    animationSpec = tween(180),
                    label = "repeat_tint"
                )
                val animatedRepeatBg by animateColorAsState(
                    targetValue = if (repeatActive) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                    animationSpec = tween(180),
                    label = "repeat_bg"
                )
                val animatedRepeatBorder by animateColorAsState(
                    targetValue = if (repeatActive) accentColor.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.15f),
                    animationSpec = tween(180),
                    label = "repeat_border"
                )
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(animatedRepeatBg)
                        .border(
                            width = 1.dp,
                            color = animatedRepeatBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .bouncyClickable(scaleDown = 0.94f) { player.toggleRepeat() },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE,
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(120)))
                                .togetherWith(scaleOut(tween(90)) + fadeOut(tween(90)))
                        },
                        label = "repeat_icon_swap"
                    ) { isOne ->
                        Icon(
                            imageVector = if (isOne) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = animatedRepeatTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Bottom Utilities: Lyrics, Audio/EQ, Queue (Floating Frosted Capsule Dock) ---
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.06f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 22.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lyrics Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .bouncyClickable(scaleDown = 0.92f) { onOpenLyrics() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubbleOutline,
                            contentDescription = "Lyrics",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Audio & EQ effects button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .bouncyClickable(scaleDown = 0.92f) { showAudioPlaybackModal = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = "Audio settings",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Queue Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .bouncyClickable(scaleDown = 0.92f) { onOpenQueue() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // --- Audio & Playback Modal ---
        AudioPlaybackModal(
            player = player,
            isOpen = showAudioPlaybackModal,
            onDismiss = { showAudioPlaybackModal = false }
        )

        // --- Add to Playlist Dialog ---
        if (showAddToPlaylistDialog) {
            val selectablePlaylists = playlists.filter { it.id != "liked_songs" && it.id != "local_songs" }
            Dialog(onDismissRequest = { showAddToPlaylistDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF161622),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Add to Playlist",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        if (selectablePlaylists.isEmpty()) {
                            Text(
                                text = "No custom playlists yet. Create one in the Playlist tab!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 300.dp)) {
                                items(selectablePlaylists, key = { it.id }) { pl ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.06f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bouncyClickable {
                                                repository.addTrackToPlaylist(pl.id, currentTrack)
                                                showAddToPlaylistDialog = false
                                                Toast.makeText(context, "Added to ${pl.title}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(pl.title, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = Color.White)
                                                Text("${pl.trackCount} tracks • ${pl.source}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            androidx.compose.material3.TextButton(onClick = { showAddToPlaylistDialog = false }) {
                                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private data class ArtworkContent(
    val trackId: String,
    val title: String,
    val artworkUrl: String?
)

private fun resolveArtworkModel(artworkUrl: String?): Any? = when {
    artworkUrl.isNullOrBlank() -> null
    artworkUrl.startsWith("http://") || artworkUrl.startsWith("https://") -> artworkUrl
    artworkUrl.startsWith("content://") || artworkUrl.startsWith("file://") ->
        android.net.Uri.parse(artworkUrl)
    else -> java.io.File(artworkUrl)
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    var reduceMotion by remember(context) {
        mutableStateOf(
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) <= 0f
        )
    }

    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduceMotion = Settings.Global.getFloat(
                    context.contentResolver,
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                ) <= 0f
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    return reduceMotion
}
