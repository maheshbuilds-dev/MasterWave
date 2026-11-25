package com.masterwave.player.ui.screens

import android.content.Context
import java.util.Locale
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpatialAudio
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.model.Album
import com.masterwave.player.data.model.SearchResult
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.ArtistAvatar
import com.masterwave.player.ui.components.TrackItem
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch

enum class SearchFilter(val label: String) {
    ALL("Top Results"),
    SONGS("Songs"),
    LOCAL("Local Audio"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists")
}

data class AppleMusicCategory(
    val title: String,
    val icon: ImageVector,
    val gradient: Brush
)

/**
 * Apple Music Search Screen with Material 3 Expressive Shapes:
 * - Large Bold Headline "Search"
 * - Stadium Pill Search Bar with responsive debounce & clear
 * - Browse Categories 2-Column Grid with rich expressive gradients
 * - Recent Searches with quick clear
 * - Live suggestions dropdown
 * - Expressive Song, Artist & Album result components
 */
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
    val prefs = remember { context.getSharedPreferences("masterwave_search_prefs_v2", Context.MODE_PRIVATE) }

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

    // Search History Persistence
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
    val accentColor by ThemeState.effectiveAccentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFFFFFFF)
    val cardBg = if (isAmoled) Color(0xFF14151C) else Color(0xFFF1F3F5)
    val fieldBg = if (isAmoled) Color(0xFF1E202C) else Color(0xFFF8F9FA)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF101828)
    val textSecondary = if (isAmoled) Color(0xFFA0A5B8) else Color(0xFF6A6A6A)

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

    // Iconic Apple Music Browse Categories with Material 3 Expressive Gradients
    val browseCategories = remember {
        listOf(
            AppleMusicCategory(
                title = "Spatial Audio",
                icon = Icons.Default.SpatialAudio,
                gradient = Brush.linearGradient(listOf(Color(0xFF581C87), Color(0xFFA855F7)))
            ),
            AppleMusicCategory(
                title = "Top Charts & Hits",
                icon = Icons.Default.TrendingUp,
                gradient = Brush.linearGradient(listOf(Color(0xFFBE123C), Color(0xFFFB7185)))
            ),
            AppleMusicCategory(
                title = "Bollywood & India",
                icon = Icons.Default.MusicNote,
                gradient = Brush.linearGradient(listOf(Color(0xFFC2410C), Color(0xFFFDBA74)))
            ),
            AppleMusicCategory(
                title = "Pop Hits",
                icon = Icons.Default.GraphicEq,
                gradient = Brush.linearGradient(listOf(Color(0xFFB45309), Color(0xFFFBBF24)))
            ),
            AppleMusicCategory(
                title = "Hip-Hop / Rap",
                icon = Icons.Default.Headphones,
                gradient = Brush.linearGradient(listOf(Color(0xFF047857), Color(0xFF34D399)))
            ),
            AppleMusicCategory(
                title = "Rock & Metal",
                icon = Icons.Default.GraphicEq,
                gradient = Brush.linearGradient(listOf(Color(0xFF991B1B), Color(0xFFDC2626)))
            ),
            AppleMusicCategory(
                title = "Dance & Electronic",
                icon = Icons.Default.MusicNote,
                gradient = Brush.linearGradient(listOf(Color(0xFF0369A1), Color(0xFF38BDF8)))
            ),
            AppleMusicCategory(
                title = "R&B / Soul",
                icon = Icons.Default.Headphones,
                gradient = Brush.linearGradient(listOf(Color(0xFF7E22CE), Color(0xFFC084FC)))
            ),
            AppleMusicCategory(
                title = "Chill & Acoustic",
                icon = Icons.Default.GraphicEq,
                gradient = Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF2DD4BF)))
            ),
            AppleMusicCategory(
                title = "Classical & Piano",
                icon = Icons.Default.MusicNote,
                gradient = Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF94A3B8)))
            ),
            AppleMusicCategory(
                title = "Indie & Alternative",
                icon = Icons.Default.Headphones,
                gradient = Brush.linearGradient(listOf(Color(0xFF3730A3), Color(0xFF818CF8)))
            ),
            AppleMusicCategory(
                title = "Workout & Energy",
                icon = Icons.Default.TrendingUp,
                gradient = Brush.linearGradient(listOf(Color(0xFFB91C1C), Color(0xFFF87171)))
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 1. Apple Music Display Header & Back Button ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isAmoled) Color(0xFF14151E) else Color(0xFFE5E7EB))
                        .border(
                            width = 1.dp,
                            color = if (isAmoled) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .bouncyClickable(scaleDown = 0.92f, onClick = onBack),
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
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.6).sp
                    ),
                    color = textPrimary
                )
            }

            // --- 2. Stadium Pill Search Bar (Material 3 Expressive) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
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
                            text = "Artists, Songs, Lyrics and More",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                            color = textSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = accentColor,
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
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }

            // --- 3. Filter Chips Row (Material 3 Expressive Pill Capsules) ---
            if (query.isNotBlank() || searchResult != null) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SearchFilter.entries) { filter ->
                        val isSelected = filter == selectedFilter
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) accentColor else (if (isAmoled) Color(0xFF1E202C) else Color(0xFFEFEBEB)),
                            modifier = Modifier.bouncyClickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                ),
                                color = if (isSelected) Color.White else textPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // --- 4. Main Body: Categories / Suggestions / History / Results ---
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
                // When Search Query is Empty -> Apple Music Categories & Recent Searches
                if (suggestions.isNotEmpty() && query.isNotBlank()) {
                    // Suggestions List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = cardBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bouncyClickable {
                                        query = suggestion
                                        performSearch(suggestion)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Recent Searches (if available)
                        if (searchHistory.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recent Searches",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                letterSpacing = (-0.3).sp
                                            ),
                                            color = textPrimary
                                        )
                                        Text(
                                            text = "Clear All",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp
                                            ),
                                            color = accentColor,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .bouncyClickable(scaleDown = 0.94f) { clearSearchHistory() }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(searchHistory) { item ->
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = cardBg,
                                                border = BorderStroke(1.dp, if (isAmoled) Color(0x1FFFFFFF) else Color(0x15000000)),
                                                modifier = Modifier.bouncyClickable {
                                                    query = item
                                                    performSearch(item)
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.History,
                                                        contentDescription = null,
                                                        tint = textSecondary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = item,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = textPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .bouncyClickable(scaleDown = 0.85f) { removeSearchQuery(item) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Remove",
                                                            tint = textSecondary,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Feature 12: Ask Wave & Smart Mood Intents
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Ask Wave • Smart Moods",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = textPrimary
                                )
                                val askWaveIntents = listOf(
                                    "Chill Acoustic" to "chill acoustic relaxing guitar songs",
                                    "High Energy" to "high energy workout edm music",
                                    "Late Night" to "late night lo-fi chill beats",
                                    "Romantic Hits" to "romantic love melodious songs",
                                    "Deep Focus" to "deep focus ambient study music",
                                    "Party Pop" to "club dance party music hits"
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(askWaveIntents) { (label, intentQuery) ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = cardBg,
                                            border = BorderStroke(1.dp, if (isAmoled) Color(0x22FFFFFF) else Color(0x14000000)),
                                            modifier = Modifier.bouncyClickable {
                                                query = label
                                                performSearch(intentQuery)
                                            }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textPrimary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Apple Music "Browse Categories" Grid
                        item {
                            Text(
                                text = "Browse Categories",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    letterSpacing = (-0.4).sp
                                ),
                                color = textPrimary
                            )
                        }

                        // 2-Column Expressive Gradient Cards
                        items(browseCategories.chunked(2)) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { cat ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(96.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(cat.gradient)
                                            .bouncyClickable {
                                                query = cat.title
                                                performSearch(cat.title)
                                            }
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = cat.title,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = Color.White,
                                            letterSpacing = (-0.3).sp,
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.35f),
                                            modifier = Modifier
                                                .size(38.dp)
                                                .align(Alignment.BottomEnd)
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                // Search Results View (Apple Music Clean Structure)
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
                                id = "album_${cleanQuery.lowercase().replace(" ", "_")}",
                                title = "$cleanQuery (Soundtrack)",
                                artist = composers.ifBlank { "Original Motion Picture" },
                                year = "2024",
                                thumbnailUrl = topArt,
                                trackCount = songs.size,
                                tracks = songs
                            )
                        )
                    }
                    list
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Result Card
                    if (topSong != null && (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.SONGS)) {
                        item {
                            Text(
                                text = "Top Result",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = cardBg,
                                border = BorderStroke(1.dp, if (isAmoled) Color(0x1FFFFFFF) else Color(0x10000000)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bouncyClickable {
                                        val q = if (songs.contains(topSong)) songs else listOf(topSong) + songs
                                        player.playTrack(topSong, q)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(14.dp))
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
                                            shape = RoundedCornerShape(50),
                                            color = accentColor.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = "SONG",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp,
                                                    letterSpacing = 1.sp
                                                ),
                                                color = accentColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                        }
                    }

                    // Artists Section
                    if (artists.isNotEmpty() && (selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.ARTISTS)) {
                        item {
                            Text(
                                text = "Artists",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(artists) { artistName ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(86.dp)
                                            .bouncyClickable { onArtistSelected(artistName) }
                                    ) {
                                        ArtistAvatar(name = artistName, photoUrl = null, size = 74.dp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = artistName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
                                            ),
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Songs List Section (Supports ALL, SONGS and LOCAL filters)
                    val displaySongs = when (selectedFilter) {
                        SearchFilter.LOCAL -> songs.filter { it.isLocal }
                        SearchFilter.SONGS, SearchFilter.ALL -> songs
                        else -> emptyList()
                    }

                    if (displaySongs.isNotEmpty()) {
                        item {
                            Text(
                                text = if (selectedFilter == SearchFilter.LOCAL) "Local Audio Tracks" else "Songs",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = textPrimary
                            )
                        }

                        items(displaySongs, key = { it.id }) { track ->
                            TrackItem(
                                track = track,
                                isPlaying = player.currentTrack.collectAsState().value?.id == track.id && player.isPlaying.collectAsState().value,
                                isFavorite = favorites.any { it.id == track.id },
                                onClick = { player.playTrack(track, displaySongs) },
                                onFavoriteToggle = { repository.toggleFavorite(track) }
                            )
                        }
                    } else if (selectedFilter == SearchFilter.LOCAL) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No local audio matches \"$query\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
