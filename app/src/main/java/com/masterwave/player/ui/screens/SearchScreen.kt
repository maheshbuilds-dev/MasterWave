package com.masterwave.player.ui.screens

import android.content.Context
import java.util.Locale
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.recommendation.ArtistPhotoProvider
import com.masterwave.player.data.model.Album
import com.masterwave.player.data.model.Artist
import com.masterwave.player.data.model.SearchResult
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.GlassyStyle
import com.masterwave.player.ui.components.MiniPlayerDock
import com.masterwave.player.ui.components.TrackItem
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.components.glassySurface
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch

enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PODCASTS("Podcasts"),
    PLAYLISTS("Playlists")
}

@Composable
fun SearchScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onBack: () -> Unit,
    onArtistSelected: (String) -> Unit = {},
    onAlbumSelected: (Album) -> Unit = {},
    onExpandNowPlaying: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("marshall_search_prefs", Context.MODE_PRIVATE) }

    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }
    var searchResult by remember { mutableStateOf<SearchResult?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Live search suggestions as user types
    LaunchedEffect(query) {
        if (query.trim().length >= 2 && searchResult == null) {
            kotlinx.coroutines.delay(200L)
            try {
                val fetched = repository.getSearchSuggestions(query.trim())
                suggestions = fetched.take(8)
            } catch (_: Exception) {
                suggestions = emptyList()
            }
        } else {
            suggestions = emptyList()
        }
    }

    // Search History
    var searchHistory by remember {
        mutableStateOf(
            prefs.getStringSet("history", emptySet())?.toList() ?: emptyList()
        )
    }

    fun saveSearchQuery(q: String) {
        if (q.isBlank()) return
        val updated = (listOf(q.trim()) + searchHistory.filter { !it.equals(q.trim(), ignoreCase = true) }).take(10)
        searchHistory = updated
        prefs.edit().putStringSet("history", updated.toSet()).apply()
    }

    fun removeSearchQuery(q: String) {
        val updated = searchHistory.filter { it != q }
        searchHistory = updated
        prefs.edit().putStringSet("history", updated.toSet()).apply()
    }

    fun clearSearchHistory() {
        searchHistory = emptyList()
        prefs.edit().remove("history").apply()
    }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val favorites by repository.favorites.collectAsState()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFF6F5F3)
    val fieldBg = if (isAmoled) Color(0xFF14141E) else Color(0xFFEFECEF)
    val cardBg = if (isAmoled) Color(0xFF101018) else Color(0xFFEFECEF).copy(alpha = 0.6f)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)
    val textTertiary = if (isAmoled) Color(0xFF707080) else Color(0xFF8E8E9A)

    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val isLoading by player.isLoading.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()

    fun performSearch(q: String) {
        if (q.isBlank()) return
        focusManager.clearFocus()
        saveSearchQuery(q)
        isSearching = true
        coroutineScope.launch {
            val result = repository.search(q)
            searchResult = result
            isSearching = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        // Search Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
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

            TextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.isBlank()) {
                        searchResult = null
                    }
                },
                placeholder = {
                    Text(
                        text = "Search tracks, artists, albums, podcasts...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = textTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            searchResult = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch(query) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldBg,
                    unfocusedContainerColor = fieldBg,
                    disabledContainerColor = fieldBg,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
        }

        // Filter Chips Row (All, Songs, Albums, Artists, Podcasts, Playlists)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SearchFilter.entries) { filter ->
                val isSelected = filter == selectedFilter
                Box(
                    modifier = Modifier.glassySurface(
                        shape = RoundedCornerShape(16.dp),
                        style = if (isSelected) GlassyStyle.PROMINENT else GlassyStyle.SLAB,
                        isDark = isAmoled,
                        tintColor = if (isSelected) accentColor else null,
                        elevation = if (isSelected) 3.dp else 1.dp,
                        onClick = { selectedFilter = filter }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = if (isSelected) Color.White else textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Body: History or Loading or Results
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (searchResult == null) {
            // Live Autocomplete Suggestions as user types
            if (suggestions.isNotEmpty() && query.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Suggestions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cardBg)
                                    .bouncyClickable {
                                        query = suggestion
                                        performSearch(suggestion)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp
                                    ),
                                    color = textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Search",
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else if (searchHistory.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = accentColor,
                            modifier = Modifier.bouncyClickable { clearSearchHistory() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(searchHistory) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(cardBg)
                                    .bouncyClickable {
                                        query = item
                                        performSearch(item)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { removeSearchQuery(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Search for your favorite songs, artists, and playlists",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textTertiary
                    )
                }
            }
        } else {
            // Search Results View
            val result = searchResult!!
            val songs = result.songs
            val topSong = songs.firstOrNull()

            val artists = remember(songs) {
                songs.map { it.artist }.distinct().filter { it.isNotBlank() && it != "Unknown" }.take(6)
            }

            val albums = remember(songs, query) {
                val list = mutableListOf<Album>()
                if (songs.isNotEmpty()) {
                    val cleanQuery = query.trim().split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
                    val topArt = songs.firstOrNull()?.artworkUrl
                    val composers = songs.map { it.artist }.distinct().take(3).joinToString(", ")
                    list.add(
                        Album(
                            id = "movie_${cleanQuery.lowercase().replace(" ", "_")}",
                            title = "$cleanQuery (Soundtrack)",
                            artist = composers.ifBlank { "Original Motion Picture Soundtrack" },
                            year = "2024",
                            thumbnailUrl = topArt,
                            trackCount = songs.size,
                            tracks = songs
                        )
                    )
                    val groupedByAlbum = songs.filter { it.album.isNotBlank() && it.album != "Unknown" }.groupBy { it.album }
                    groupedByAlbum.forEach { (albTitle, albTracks) ->
                        if (list.none { it.title.equals(albTitle, ignoreCase = true) }) {
                            list.add(
                                Album(
                                    id = "album_${albTitle.lowercase().replace(" ", "_")}",
                                    title = albTitle,
                                    artist = albTracks.firstOrNull()?.artist ?: "Various Artists",
                                    thumbnailUrl = albTracks.firstOrNull()?.artworkUrl,
                                    trackCount = albTracks.size,
                                    tracks = albTracks
                                )
                            )
                        }
                    }
                }
                list
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // --- 1. Top Result Card (Exact match / Best song) ---
                if (topSong != null && (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.SONGS)) {
                    item {
                        Text(
                            text = "Top Result",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            ),
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = cardBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { player.playTrack(topSong) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(fieldBg)
                                ) {
                                    if (!topSong.artworkUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = topSong.artworkUrl,
                                            contentDescription = topSong.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = accentColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "SONG",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp,
                                                letterSpacing = 1.sp
                                            ),
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = topSong.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = topSong.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // --- 2. Artists Section (if All or Artists) ---
                if (artists.isNotEmpty() && (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.ARTISTS)) {
                    item {
                        Text(
                            text = "Artists",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(artists) { artistName ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(86.dp)
                                        .clickable { onArtistSelected(artistName) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.15f))
                                            .border(1.5.dp, Brush.linearGradient(listOf(accentColor, Color(0xFF9C27B0))), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = ArtistPhotoProvider.getPhotoUrl(artistName),
                                            contentDescription = artistName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = artistName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "Artist",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // --- 3. Albums & Movie Soundtracks Section ---
                if (albums.isNotEmpty() && (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.ALBUMS)) {
                    item {
                        Text(
                            text = if (selectedFilter == SearchFilter.ALBUMS) "Albums & Movie Soundtracks" else "Albums",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        if (selectedFilter == SearchFilter.ALBUMS) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                albums.forEach { album ->
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = cardBg,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onAlbumSelected(album) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = fieldBg,
                                                modifier = Modifier.size(64.dp)
                                            ) {
                                                if (!album.thumbnailUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = album.thumbnailUrl,
                                                        contentDescription = album.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Album,
                                                            contentDescription = null,
                                                            tint = accentColor,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = album.title,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp
                                                    ),
                                                    color = textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${album.artist} • ${album.trackCount} Tracks",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                    color = textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "View",
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(albums) { album ->
                                    Column(
                                        modifier = Modifier
                                            .width(135.dp)
                                            .clickable { onAlbumSelected(album) }
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = cardBg,
                                            shadowElevation = 6.dp,
                                            modifier = Modifier.size(135.dp)
                                        ) {
                                            if (!album.thumbnailUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = album.thumbnailUrl,
                                                    contentDescription = album.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Album,
                                                        contentDescription = null,
                                                        tint = accentColor,
                                                        modifier = Modifier.size(44.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            ),
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = "${album.trackCount} Songs",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // --- 4. Songs List (if All or Songs) ---
                if (songs.isNotEmpty() && (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.SONGS)) {
                    item {
                        Text(
                            text = "Songs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    items(songs.drop(if (selectedFilter == SearchFilter.ALL) 1 else 0)) { track ->
                        TrackItem(
                            track = track,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isFavorite = favorites.any { it.id == track.id },
                            onClick = { player.playTrack(track) },
                            onFavoriteToggle = { repository.toggleFavorite(track) }
                        )
                    }
                }

                // --- 4. Podcasts / Albums / Playlists Fallback ---
                if (selectedFilter == SearchFilter.PODCASTS) {
                    item {
                        val podcasts = songs.filter {
                            it.title.contains("podcast", ignoreCase = true) ||
                                    it.title.contains("show", ignoreCase = true) ||
                                    it.title.contains("episode", ignoreCase = true)
                        }.ifEmpty { songs.take(5) }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            podcasts.forEach { track ->
                                TrackItem(
                                    track = track,
                                    isPlaying = isPlaying && currentTrack?.id == track.id,
                                    isFavorite = favorites.any { it.id == track.id },
                                    onClick = { player.playTrack(track) },
                                    onFavoriteToggle = { repository.toggleFavorite(track) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(130.dp))
                }
            }
        }
    }

    // Floating Frozen Glass MiniPlayer in Search Screen
        if (currentTrack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                MiniPlayerDock(
                    track = currentTrack!!,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onExpand = onExpandNowPlaying,
                    onPlayPause = { player.togglePlayPause() },
                    onNext = { player.skipToNext() }
                )
            }
        }
    }
}
