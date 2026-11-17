package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch

data class GeneratorMode(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val bgGradients: List<Color>
)

@Composable
fun GeneratorScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val favorites by repository.favorites.collectAsState()
    val localTracks by repository.localTracks.collectAsState()
    val currentTrack by player.currentTrack.collectAsState()

    var generatingModeId by remember { mutableStateOf<String?>(null) }

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFF6F5F3)
    val cardBg = if (isAmoled) Color(0xFF11111A) else Color(0xFFFFFFFF)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)

    val modes = remember {
        listOf(
            GeneratorMode(
                id = "recent",
                title = "Recent Tracks",
                description = "Generate an extended queue from your listening patterns and recent sessions",
                icon = Icons.Default.History,
                accent = Color(0xFF2979FF),
                bgGradients = listOf(Color(0xFF1E3C72).copy(alpha = 0.22f), Color.Transparent)
            ),
            GeneratorMode(
                id = "radio",
                title = "Song Radio",
                description = "Build an endless dynamic radio based on the current song or your top track",
                icon = Icons.Default.Radio,
                accent = Color(0xFF00E676),
                bgGradients = listOf(Color(0xFF00E676).copy(alpha = 0.18f), Color.Transparent)
            ),
            GeneratorMode(
                id = "similar",
                title = "Similar Artists",
                description = "Explore tracks from artists sharing acoustic DNA with your favorites",
                icon = Icons.Default.Group,
                accent = Color(0xFFD500F9),
                bgGradients = listOf(Color(0xFFD500F9).copy(alpha = 0.18f), Color.Transparent)
            ),
            GeneratorMode(
                id = "tag",
                title = "By Tag / Genre",
                description = "Generate high-vibe playlists matching specific mood tags and electronic sub-genres",
                icon = Icons.Default.Category,
                accent = Color(0xFFFF9100),
                bgGradients = listOf(Color(0xFFFF9100).copy(alpha = 0.18f), Color.Transparent)
            ),
            GeneratorMode(
                id = "mymix",
                title = "My Mix",
                description = "Algorithmic fusion blending 50% familiar favorites and 50% fresh discoveries",
                icon = Icons.Default.AutoAwesome,
                accent = Color(0xFFFF1744),
                bgGradients = listOf(Color(0xFFFF1744).copy(alpha = 0.18f), Color.Transparent)
            ),
            GeneratorMode(
                id = "recommendation",
                title = "My Recommendation",
                description = "Daily personalized algorithmic stream tailored to your listening taste profile",
                icon = Icons.Default.Recommend,
                accent = Color(0xFF00E5FF),
                bgGradients = listOf(Color(0xFF00E5FF).copy(alpha = 0.18f), Color.Transparent)
            ),
            GeneratorMode(
                id = "neverheard",
                title = "Never Heard",
                description = "Discover hidden gems, underground singles, and tracks completely new to you",
                icon = Icons.Default.Explore,
                accent = Color(0xFFFFD600),
                bgGradients = listOf(Color(0xFFFFD600).copy(alpha = 0.18f), Color.Transparent)
            ),
            GeneratorMode(
                id = "library",
                title = "My Library",
                description = "Smart shuffle and forgotten favorites from your saved local and cloud playlists",
                icon = Icons.Default.LibraryMusic,
                accent = Color(0xFF7C4DFF),
                bgGradients = listOf(Color(0xFF7C4DFF).copy(alpha = 0.18f), Color.Transparent)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isAmoled) Color(0xFF161622) else Color(0xFFE8E5E8))
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

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "✦ Generator",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "Algorithmic playlists & acoustic flows",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }
                }
            }

            // Info Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(accentColor.copy(alpha = 0.18f), Color.Transparent)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "Smart Generator Engine",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Select a mode below to instantly synthesize a seamless 25-track continuous queue.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 8 Generator Modes (Page 7 of LastWave)
            items(modes, key = { it.id }) { mode ->
                val isGenerating = generatingModeId == mode.id

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = cardBg,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = generatingModeId == null) {
                            generatingModeId = mode.id
                            scope.launch {
                                try {
                                    val generatedTracks = when (mode.id) {
                                        "recent" -> {
                                            val pool = if (favorites.isNotEmpty()) favorites else localTracks
                                            if (pool.isNotEmpty()) pool.shuffled().take(25)
                                            else repository.getTrendingTracks().take(25)
                                        }
                                        "radio" -> {
                                            val seed = currentTrack?.title ?: "Popular hits"
                                            val res = repository.search("$seed radio mix").songs
                                            if (res.isNotEmpty()) res else repository.getTrendingTracks()
                                        }
                                        "similar" -> {
                                            val artist = currentTrack?.artist?.takeIf { it.isNotBlank() } ?: "Arijit Singh"
                                            val res = repository.search("Similar to $artist top songs").songs
                                            if (res.isNotEmpty()) res else repository.getTrendingTracks()
                                        }
                                        "tag" -> {
                                            val res = repository.search("Electronic Lo-Fi Indie Pop hits").songs
                                            if (res.isNotEmpty()) res else repository.getTrendingTracks()
                                        }
                                        "mymix" -> {
                                            val favSlice = favorites.shuffled().take(12)
                                            val trendingSlice = repository.getTrendingTracks().take(13)
                                            (favSlice + trendingSlice).shuffled()
                                        }
                                        "recommendation" -> {
                                            repository.getTrendingTracks().shuffled().take(25)
                                        }
                                        "neverheard" -> {
                                            val res = repository.search("Undiscovered indie fresh alternative").songs
                                            val knownIds = (favorites.map { it.id } + localTracks.map { it.id }).toSet()
                                            res.filterNot { it.id in knownIds }.take(25)
                                        }
                                        "library" -> {
                                            val pool = (favorites + localTracks).distinctBy { it.id }
                                            if (pool.isNotEmpty()) pool.shuffled().take(25)
                                            else repository.getTrendingTracks().take(25)
                                        }
                                        else -> repository.getTrendingTracks().take(25)
                                    }

                                    if (generatedTracks.isNotEmpty()) {
                                        player.playPlaylist(generatedTracks, 0)
                                        Toast.makeText(
                                            context,
                                            "Playing ${mode.title} (${generatedTracks.size} tracks)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "No tracks generated", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Generation error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    generatingModeId = null
                                }
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(mode.bgGradients))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(mode.accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        color = mode.accent,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = mode.icon,
                                        contentDescription = null,
                                        tint = mode.accent,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = textSecondary,
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(mode.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Generate",
                                    tint = mode.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
