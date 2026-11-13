package com.masterwave.player.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch

data class MoodItem(
    val title: String,
    val subtitle: String,
    val colors: List<Color>,
    val query: String
)

data class GenreItem(
    val title: String,
    val subtitle: String,
    val colors: List<Color>,
    val query: String
)

@Composable
fun MoodGenresScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoadingVibe by remember { mutableStateOf(false) }

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFF7F5F8)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)
    val backBg = if (isAmoled) Color(0xFF161622) else Color(0xFFEFECEF)

    val moods = remember {
        listOf(
            MoodItem("Chill", "Mellow & relaxed beats", listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)), "Chill relaxing music"),
            MoodItem("Commute", "On-the-go driving tracks", listOf(Color(0xFF1565C0), Color(0xFF0D47A1)), "Commute driving hits"),
            MoodItem("Diwali", "Festive lights & joy", listOf(Color(0xFFE65100), Color(0xFFBF360C)), "Diwali celebration festive songs"),
            MoodItem("Energize", "High BPM adrenaline", listOf(Color(0xFFC2185B), Color(0xFF880E4F)), "High energy workout pump songs"),
            MoodItem("Feel good", "Uplifting pop & smile", listOf(Color(0xFFF57F17), Color(0xFFE65100)), "Feel good happy songs"),
            MoodItem("Focus", "Deep work & flow state", listOf(Color(0xFF00838F), Color(0xFF006064)), "Deep focus binaural study beats"),
            MoodItem("Gaming", "Synthwave & deep bass", listOf(Color(0xFF6A1B9A), Color(0xFF4A148C)), "Gaming synthwave bass trap"),
            MoodItem("Party", "Club bangers & dance", listOf(Color(0xFFD81B60), Color(0xFFAD1457)), "Party dance club hits"),
            MoodItem("Romance", "Soulful love melodies", listOf(Color(0xFFC62828), Color(0xFF8E0000)), "Romantic love songs Bollywood melodies"),
            MoodItem("Sad", "Heartbreak & rain", listOf(Color(0xFF37474F), Color(0xFF263238)), "Sad soulful emotional songs"),
            MoodItem("Sleep", "Peaceful ambient rest", listOf(Color(0xFF283593), Color(0xFF1A237E)), "Sleep meditation calm music"),
            MoodItem("Workout", "Heavy gym & pump", listOf(Color(0xFFD32F2F), Color(0xFFB71C1C)), "Gym workout motivation EDM"),
            MoodItem("Study", "Lo-Fi & gentle piano", listOf(Color(0xFF00695C), Color(0xFF004D40)), "Lofi study beats piano relax"),
            MoodItem("Rainy Day", "Acoustic coffeehouse", listOf(Color(0xFF455A64), Color(0xFF263238)), "Rainy day acoustic guitar songs"),
            MoodItem("Late Night", "R&B & midnight drive", listOf(Color(0xFF311B92), Color(0xFF1A237E)), "Late night drive chill songs"),
            MoodItem("Devotional", "Spiritual & sacred chants", listOf(Color(0xFFEF6C00), Color(0xFFE65100)), "Devotional bhajans spiritual hymns")
        )
    }

    val genres = remember {
        listOf(
            GenreItem("Tamil", "Kollywood & Anirudh hits", listOf(Color(0xFFE53935), Color(0xFF8E0000)), "Tamil top hits Anirudh"),
            GenreItem("Telugu", "Tollywood & Thaman energy", listOf(Color(0xFFFB8C00), Color(0xFFE65100)), "Telugu hit songs Tollywood"),
            GenreItem("Hindi / Bollywood", "Evergreen chartbusters", listOf(Color(0xFFD81B60), Color(0xFFAD1457)), "Bollywood top hits 2026"),
            GenreItem("Punjabi", "Desi drill & Bhangra", listOf(Color(0xFFFFB300), Color(0xFFFF8F00)), "Punjabi trending hits Karan Aujla"),
            GenreItem("Malayalam", "Mollywood indie melodies", listOf(Color(0xFF43A047), Color(0xFF2E7D32)), "Malayalam hit songs indie"),
            GenreItem("Kannada", "Sandalwood melodies", listOf(Color(0xFF00ACC1), Color(0xFF00838F)), "Kannada latest hit songs"),
            GenreItem("Desi Hip-Hop", "KR\$NA, Divine, Seedhe Maut", listOf(Color(0xFF424242), Color(0xFF212121)), "Desi Hip Hop rap songs"),
            GenreItem("Indian Indie", "Prateek Kuhad, Anuv Jain", listOf(Color(0xFF8E24AA), Color(0xFF6A1B9A)), "Indian indie acoustic songs"),
            GenreItem("Sufi & Qawwali", "Soulful Nusrat & Rahat", listOf(Color(0xFF6D4C41), Color(0xFF4E342E)), "Sufi Qawwali songs"),
            GenreItem("Ghazals", "Jagjit Singh & timeless poetry", listOf(Color(0xFF546E7A), Color(0xFF37474F)), "Jagjit Singh ghazals classic"),
            GenreItem("Carnatic Classical", "Traditional south ragas", listOf(Color(0xFFC0CA33), Color(0xFF9E9D24)), "Carnatic classical violin vocal"),
            GenreItem("Hindustani Classical", "North classical gharanas", listOf(Color(0xFF3949AB), Color(0xFF283593)), "Hindustani classical sitar"),
            GenreItem("Bengali", "Rabindra sangeet & modern", listOf(Color(0xFF00897B), Color(0xFF00695C)), "Bengali hits Rabindra Sangeet"),
            GenreItem("Bhojpuri", "High energy desi folk", listOf(Color(0xFFFF7043), Color(0xFFF4511E)), "Bhojpuri superhit songs"),
            GenreItem("Global Pop", "Billboard Top 40", listOf(Color(0xFF1E88E5), Color(0xFF1565C0)), "Global pop billboard top 40"),
            GenreItem("EDM & Dance", "Tomorrowland festival bangers", listOf(Color(0xFF00E676), Color(0xFF00C853)), "EDM festival dance bangers"),
            GenreItem("Lo-Fi Beats", "Midnight chillhop vibes", listOf(Color(0xFF5E35B1), Color(0xFF4527A0)), "Lofi chillhop beats sleep"),
            GenreItem("Acoustic & Unplugged", "Pure vocals & guitar", listOf(Color(0xFF8D6E63), Color(0xFF5D4037)), "Acoustic unplugged cover songs")
        )
    }

    fun playVibe(query: String, label: String) {
        if (isLoadingVibe) return
        isLoadingVibe = true
        scope.launch {
            Toast.makeText(context, "Streaming $label mix...", Toast.LENGTH_SHORT).show()
            val result = repository.search(query)
            if (result.songs.isNotEmpty()) {
                player.playTrack(result.songs.first(), result.songs)
            } else {
                Toast.makeText(context, "No songs found for $label", Toast.LENGTH_SHORT).show()
            }
            isLoadingVibe = false
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header ---
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(backBg)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Moods & Genres",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Curated Indian & Global soundscapes",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = textSecondary
                    )
                }

                if (isLoadingVibe) {
                    Spacer(modifier = Modifier.weight(1f))
                    CircularProgressIndicator(
                        color = accentColor,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        // --- Moods Section Header ---
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "16 Mood Vibes",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = textPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(moods) { mood ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { playVibe(mood.query, mood.title) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(mood.colors))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = mood.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mood.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 2
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- Genres Section Header ---
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Indian & Global Genres",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = textPrimary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }

        items(genres) { genre ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { playVibe(genre.query, genre.title) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(genre.colors))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = genre.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = genre.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 2
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
