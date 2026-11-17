package com.masterwave.player.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.cloud.CloudAccountManager
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.GlassyBadge
import com.masterwave.player.ui.components.GlassyBadgeTone
import com.masterwave.player.ui.components.GlassyStyle
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.components.glassySlab
import com.masterwave.player.ui.components.glassySurface
import com.masterwave.player.ui.theme.NothingHeadlineLarge
import com.masterwave.player.ui.theme.NothingHeadlineMedium
import com.masterwave.player.ui.theme.NothingHeadlineSmall
import com.masterwave.player.ui.theme.NothingSectionHeader
import com.masterwave.player.ui.theme.NothingTechMono
import com.masterwave.player.ui.theme.NothingDotBadge
import com.masterwave.player.ui.theme.EditorialHeadlineLarge
import com.masterwave.player.ui.theme.EditorialHeadlineMedium
import com.masterwave.player.ui.theme.EditorialHeadlineSmall
import com.masterwave.player.ui.theme.ExpressiveSectionHeader
import com.masterwave.player.ui.theme.ExpressiveDisplayFont
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import com.masterwave.player.data.recommendation.CommunityPlaylist
import com.masterwave.player.data.recommendation.CommunityPlaylistsProvider

data class DiscoverHeroItem(
    val title: String,
    val subtitle: String,
    val badge: String,
    val artworkUrl: String,
    val querySeed: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    player: MasterWavePlayer,
    repository: MusicRepository,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToLikedSongs: () -> Unit = {},
    onNavigateToGenerator: () -> Unit = {},
    onPlaylistSelected: (Playlist) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val favorites by repository.favorites.collectAsState()
    val localTracks by repository.localTracks.collectAsState()
    val recentTracks by repository.recentTracks.collectAsState()

    val downloadManager = remember {
        MusicDownloadManager.getInstance(context, repository.okHttpClient, repository)
    }
    val downloadedIds by downloadManager.downloadedIds.collectAsState()
    val cloudManager = remember { CloudAccountManager.getInstance(context, repository) }
    val userProfile by cloudManager.userProfile.collectAsState()

    val currentTrack by player.currentTrack.collectAsState()
    var activeLanguage by remember { mutableStateOf("Trending") }

    val scope = rememberCoroutineScope()
    var trendingTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var quickPickTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoadingTrending by remember { mutableStateOf(false) }

    // Dynamic Time-Based Greeting and Date (LastWave Page 1)
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late night vibes"
        }
    }

    val formattedDate = remember {
        try {
            SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(Date()).uppercase()
        } catch (e: Exception) {
            "TODAY"
        }
    }

    // Dynamic Language & Viral Music Recommendation Engine:
    // Triggers instantly whenever the active song changes!
    LaunchedEffect(currentTrack?.id) {
        isLoadingTrending = true
        scope.launch {
            val (lang, res) = com.masterwave.player.data.recommendation.LanguageMusicRecommender.getLanguageViralTracks(
                repository,
                currentTrack
            )
            activeLanguage = lang
            trendingTracks = res
            quickPickTracks = if (res.size >= 16) res.take(16) else (res + res).take(16)
            isLoadingTrending = false
        }
    }

    var madeForYouTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var dailyMixTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var becauseYouListenedHeader by remember { mutableStateOf("Because You Listened") }
    var becauseYouListenedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var rediscoverTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

    LaunchedEffect(currentTrack?.id, favorites.size) {
        scope.launch {
            try {
                madeForYouTracks = repository.recommendationManager.getMadeForYou(limit = 10)
                dailyMixTracks = repository.recommendationManager.getDailyMix(limit = 10)
                val seed = currentTrack ?: favorites.firstOrNull() ?: repository.recentTracks.value.firstOrNull()
                if (seed != null) {
                    val (header, recs) = repository.recommendationManager.getBecauseYouListenedTo(seed, limit = 8)
                    becauseYouListenedHeader = header
                    becauseYouListenedTracks = recs
                }
                rediscoverTracks = repository.recommendationManager.getRediscoverTracks(limit = 8)
            } catch (_: Exception) {}
        }
    }

    val topArtist = remember(trendingTracks) {
        trendingTracks.firstOrNull()?.artist?.takeIf { it.isNotBlank() } ?: "Sai Abhyankkar"
    }

    val similarTracks = remember(trendingTracks) {
        trendingTracks.drop(2).take(6)
    }

    val favoriteArtists = remember(activeLanguage, trendingTracks) {
        val fromTrending = trendingTracks.map { it.artist }.distinct().filter { it.isNotBlank() && it != "Unknown" }
        val fallback = when (activeLanguage) {
            "Tamil" -> listOf("Sai Abhyankkar", "Anirudh Ravichander", "A.R. Rahman", "Yuvan Shankar Raja", "Harris Jayaraj", "Sid Sriram", "Santhosh Narayanan", "D. Imman")
            "Punjabi" -> listOf("Diljit Dosanjh", "Sidhu Moose Wala", "Karan Aujla", "AP Dhillon", "B Praak", "Ammy Virk", "Guru Randhawa", "Shubh")
            "Telugu" -> listOf("Devi Sri Prasad", "Thaman S", "Sid Sriram", "Anurag Kulkarni", "Ram Miriyala", "M.M. Keeravaani", "Armaan Malik")
            "Hindi" -> listOf("Arijit Singh", "Pritam", "Shreya Ghoshal", "Sachin-Jigar", "KR\$NA", "Atif Aslam", "Vishal-Shekhar", "Neha Kakkar")
            else -> listOf("The Weeknd", "Dua Lipa", "Coolio", "Post Malone", "Ed Sheeran", "Billie Eilish", "Taylor Swift", "Drake")
        }
        (fromTrending + fallback).distinct().take(10)
    }

    val bgColor = if (isAmoled) Color(0xFF000000) else Color(0xFFF6F5F3)
    val cardBg = if (isAmoled) Color(0xFF101018) else Color(0xFFFFFFFF)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF1C1B1F)
    val textSecondary = if (isAmoled) Color(0xFFB0B0C0) else Color(0xFF5A5A66)
    val chipInactiveBg = if (isAmoled) Color(0xFF14141E) else Color(0xFFEFECEF)

    fun openCommunityPlaylist(playlist: CommunityPlaylist) {
        scope.launch {
            Toast.makeText(context, "Opening ${playlist.title}...", Toast.LENGTH_SHORT).show()
            val fetchedSongs = try {
                repository.search(playlist.searchQuery).songs
            } catch (_: Exception) {
                emptyList()
            }
            val pl = Playlist(
                id = playlist.id,
                title = playlist.title,
                description = playlist.description,
                trackCount = if (fetchedSongs.isNotEmpty()) fetchedSongs.size else playlist.trackCount,
                tracks = fetchedSongs,
                artworkUrl = playlist.artworkUrl,
                source = "Community • $activeLanguage",
                createdAt = playlist.updatedToday
            )
            onPlaylistSelected(pl)
        }
    }

    // Curated Discover Hero items (Echo Music Carousel Style - Dynamically reflects activeLanguage!)
    val discoverCards = remember(activeLanguage, trendingTracks) {
        when (activeLanguage) {
            "Tamil" -> listOf(
                DiscoverHeroItem(
                    title = "Kollywood Viral 50",
                    subtitle = "Most played Tamil bangers right now",
                    badge = "✦ #1 IN TAMIL NADU",
                    artworkUrl = trendingTracks.getOrNull(0)?.artworkUrl ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&fit=crop",
                    querySeed = "trending viral tamil hits 2026 songs"
                ),
                DiscoverHeroItem(
                    title = "Anirudh Universe",
                    subtitle = "High-octane bass, rock guitars & iconic hooks",
                    badge = "POPULAR",
                    artworkUrl = trendingTracks.getOrNull(1)?.artworkUrl ?: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&fit=crop",
                    querySeed = "Anirudh Ravichander best songs 2026"
                ),
                DiscoverHeroItem(
                    title = "Sai Abhyankkar Hits",
                    subtitle = "Aane Wala Star, Katchi Sera & new wave indie",
                    badge = "TRENDING",
                    artworkUrl = trendingTracks.getOrNull(2)?.artworkUrl ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&fit=crop",
                    querySeed = "Sai Abhyankkar songs Aane Wala Star Katchi Sera"
                ),
                DiscoverHeroItem(
                    title = "Tamil Melodies Uncut",
                    subtitle = "Soulful violins, live strings & heartfelt lyrics",
                    badge = "MELODIES",
                    artworkUrl = trendingTracks.getOrNull(3)?.artworkUrl ?: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800&fit=crop",
                    querySeed = "tamil melody songs soulful hits"
                ),
                DiscoverHeroItem(
                    title = "Yuvan 2000s Nostalgia",
                    subtitle = "Unmatched timeless tracks & pure soul",
                    badge = "NOSTALGIA",
                    artworkUrl = trendingTracks.getOrNull(4)?.artworkUrl ?: "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800&fit=crop",
                    querySeed = "Yuvan Shankar Raja 2000s hits songs"
                ),
                DiscoverHeroItem(
                    title = "Madras Hip-Hop & Rap",
                    subtitle = "Street poetry & hard 808 basslines",
                    badge = "HIP-HOP",
                    artworkUrl = trendingTracks.getOrNull(5)?.artworkUrl ?: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800&fit=crop",
                    querySeed = "tamil hip hop rap independent songs"
                )
            )
            "Punjabi" -> listOf(
                DiscoverHeroItem(
                    title = "Punjabi Wave 2026",
                    subtitle = "High-octane bass, flow & anthems",
                    badge = "✦ #1 IN PUNJAB",
                    artworkUrl = trendingTracks.getOrNull(0)?.artworkUrl ?: "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800&fit=crop",
                    querySeed = "punjabi trending hits 2026 songs"
                ),
                DiscoverHeroItem(
                    title = "Sidhu Moose Wala Forever",
                    subtitle = "The legend's timeless hip-hop anthems",
                    badge = "LEGEND",
                    artworkUrl = trendingTracks.getOrNull(1)?.artworkUrl ?: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&fit=crop",
                    querySeed = "sidhu moose wala best songs"
                ),
                DiscoverHeroItem(
                    title = "Diljit Dosanjh Global",
                    subtitle = "Coachella vibes & energetic pop flow",
                    badge = "POPULAR",
                    artworkUrl = trendingTracks.getOrNull(2)?.artworkUrl ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&fit=crop",
                    querySeed = "diljit dosanjh top hits"
                ),
                DiscoverHeroItem(
                    title = "Karan Aujla Way Ahead",
                    subtitle = "Four You, Making Memories & lethal rhymes",
                    badge = "TRENDING",
                    artworkUrl = trendingTracks.getOrNull(3)?.artworkUrl ?: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800&fit=crop",
                    querySeed = "karan aujla hit songs"
                )
            )
            "Telugu" -> listOf(
                DiscoverHeroItem(
                    title = "Tollywood Chartbusters",
                    subtitle = "Mass beats, melodies & cinematic power",
                    badge = "✦ #1 IN TELUGU",
                    artworkUrl = trendingTracks.getOrNull(0)?.artworkUrl ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&fit=crop",
                    querySeed = "telugu trending hits 2026 songs"
                ),
                DiscoverHeroItem(
                    title = "Telugu Melodic Romance",
                    subtitle = "Heartfelt unplugged guitar & soothing vocals",
                    badge = "ROMANCE",
                    artworkUrl = trendingTracks.getOrNull(1)?.artworkUrl ?: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800&fit=crop",
                    querySeed = "telugu melody songs soulful hits"
                ),
                DiscoverHeroItem(
                    title = "Thaman & DSP Mass Anthems",
                    subtitle = "High voltage theatre drums & celebration",
                    badge = "MASS ANTHEMS",
                    artworkUrl = trendingTracks.getOrNull(2)?.artworkUrl ?: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&fit=crop",
                    querySeed = "thaman dsp telugu mass hits"
                )
            )
            "Hindi" -> listOf(
                DiscoverHeroItem(
                    title = "Bollywood Top 50",
                    subtitle = "Grand cinematic soundtracks & melodies",
                    badge = "✦ #1 IN INDIA",
                    artworkUrl = trendingTracks.getOrNull(0)?.artworkUrl ?: "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800&fit=crop",
                    querySeed = "bollywood latest hits 2026 songs"
                ),
                DiscoverHeroItem(
                    title = "Arijit Singh Soulful Vault",
                    subtitle = "Intimate emotions & studio perfection",
                    badge = "POPULAR",
                    artworkUrl = trendingTracks.getOrNull(1)?.artworkUrl ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&fit=crop",
                    querySeed = "arijit singh best songs"
                ),
                DiscoverHeroItem(
                    title = "Desi Hip-Hop Cypher",
                    subtitle = "KR\$NA, Divine, Seedhe Maut & raw bars",
                    badge = "DHH CYPHER",
                    artworkUrl = trendingTracks.getOrNull(2)?.artworkUrl ?: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800&fit=crop",
                    querySeed = "desi hip hop krsna divine seedhe maut"
                )
            )
            else -> listOf(
                DiscoverHeroItem(
                    title = "Infinite Radio",
                    subtitle = "Continuous playback based on your favorites",
                    badge = "✦ MADE FOR YOU",
                    artworkUrl = trendingTracks.getOrNull(0)?.artworkUrl ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800&fit=crop",
                    querySeed = "trending global hits"
                ),
                DiscoverHeroItem(
                    title = "Late Night Lo-Fi",
                    subtitle = "Intimate melodies & deep calm",
                    badge = "DISCOVER MIX",
                    artworkUrl = trendingTracks.getOrNull(1)?.artworkUrl ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&fit=crop",
                    querySeed = "lofi chill midnight vibes"
                ),
                DiscoverHeroItem(
                    title = "Trending Heat",
                    subtitle = "Most played bangers right now",
                    badge = "TRENDING",
                    artworkUrl = trendingTracks.getOrNull(2)?.artworkUrl ?: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&fit=crop",
                    querySeed = "top trending songs"
                ),
                DiscoverHeroItem(
                    title = "Synthwave Highway",
                    subtitle = "Retro neon synth & midnight drive",
                    badge = "SYNTHWAVE",
                    artworkUrl = trendingTracks.getOrNull(3)?.artworkUrl ?: "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&fit=crop",
                    querySeed = "synthwave outrun retro electro"
                )
            )
        }
    }

    val quickPickColumns = remember(quickPickTracks) {
        if (quickPickTracks.isEmpty()) emptyList() else quickPickTracks.chunked(4)
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Podcasts", "Sleep", "Relax", "Feel good", "Romance", "Energize", "Workout", "Focus", "Party")

    var categoryFilterTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoadingCategoryFilter by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != "All") {
            isLoadingCategoryFilter = true
            val query = when (selectedCategory) {
                "Podcasts" -> "podcast episodes music discussion"
                "Sleep" -> "deep sleep ambient calming delta wave music"
                "Relax" -> "relaxing calm ambient music"
                "Feel good" -> "feel good happy upbeat pop songs"
                "Romance" -> "romantic hindi english love songs"
                "Energize" -> "high energy workout edm party tracks"
                "Workout" -> "gym workout motivation bass tracks"
                "Focus" -> "lo-fi study focus instrumental beats"
                "Party" -> "party dance anthems club hits"
                else -> "$selectedCategory hits"
            }
            try {
                val results = repository.search(query).songs
                categoryFilterTracks = results
            } catch (_: Exception) {
                categoryFilterTracks = emptyList()
            } finally {
                isLoadingCategoryFilter = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // --- Scrolling Feed Content with plenty of clearance above dock ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 188.dp, bottom = 240.dp)
        ) {
            // --- 1. Greeting & Uppercase Date (LastWave Page 1) ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = greeting,
                        style = NothingHeadlineLarge.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formattedDate.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = accentColor
                    )
                }
            }

            // --- 2. Big Covered Discover Hero Carousel (Echo Music / Apple Music Style) ---
            if (selectedCategory == "All") {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    val pagerState = rememberPagerState(pageCount = { discoverCards.size })

                    LaunchedEffect(pagerState, discoverCards.size) {
                        while (true) {
                            kotlinx.coroutines.delay(5000L)
                            if (!pagerState.isScrollInProgress && discoverCards.isNotEmpty()) {
                                val next = (pagerState.currentPage + 1) % discoverCards.size
                                pagerState.animateScrollToPage(next)
                            }
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(265.dp)
                    ) { page ->
                        val card = discoverCards[page]
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                        val cardScale = androidx.compose.ui.util.lerp(0.92f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        val cardAlpha = androidx.compose.ui.util.lerp(0.65f, 1f, 1f - pageOffset.coerceIn(0f, 1f))

                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = cardBg,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = cardScale
                                    scaleY = cardScale
                                    alpha = cardAlpha
                                }
                                .bouncyClickable {
                                    val heroPlaylist = Playlist(
                                        id = "carousel_${card.title.hashCode()}",
                                        title = card.title,
                                        description = card.querySeed,
                                        trackCount = 25,
                                        artworkUrl = card.artworkUrl.ifBlank { null },
                                        source = "Featured • Curated",
                                        createdAt = "Updated Today"
                                    )
                                    onPlaylistSelected(heroPlaylist)
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Full Artwork
                                if (card.artworkUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = card.artworkUrl,
                                        contentDescription = card.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(accentColor.copy(alpha = 0.7f), Color(0xFF0F0E16))
                                                )
                                            )
                                    )
                                }

                                // Dark Gradient Scrim Overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0x22000000),
                                                    Color(0x44000000),
                                                    Color(0xCC08070D),
                                                    Color(0xF008070D)
                                                )
                                            )
                                        )
                                )

                                // Top Left Glassy Badge
                                GlassyBadge(
                                    text = card.badge,
                                    tone = GlassyBadgeTone.GOLD,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.TopStart)
                                )

                                // Bottom Content: Title, Subtitle, and View Songs Action Pill
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(20.dp)
                                ) {
                                    Text(
                                        text = card.title,
                                        style = EditorialHeadlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp
                                        ),
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = card.subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = Color.White.copy(alpha = 0.8f)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // View Songs Glassy Prominent Button
                                    Box(
                                        modifier = Modifier.glassySurface(
                                            shape = CircleShape,
                                            style = GlassyStyle.PROMINENT,
                                            tintColor = accentColor,
                                            elevation = 4.dp,
                                            onClick = {
                                                val heroPlaylist = Playlist(
                                                    id = "carousel_${card.title.hashCode()}",
                                                    title = card.title,
                                                    description = card.querySeed,
                                                    trackCount = 25,
                                                    artworkUrl = card.artworkUrl.ifBlank { null },
                                                    source = "Featured • Curated",
                                                    createdAt = "Updated Today"
                                                )
                                                onPlaylistSelected(heroPlaylist)
                                            }
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QueueMusic,
                                                contentDescription = "View Songs",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "View Songs",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 2.0A Category Filtered Discovery (When a category chip is active) ---
            if (selectedCategory != "All") {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$selectedCategory Discovery",
                                style = ExpressiveSectionHeader.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = textPrimary
                            )
                            if (isLoadingCategoryFilter) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = accentColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (categoryFilterTracks.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(categoryFilterTracks) { track ->
                                Column(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable { player.playTrack(track) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(chipInactiveBg)
                                    ) {
                                        if (!track.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = track.artworkUrl,
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF34C759),
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .align(Alignment.TopStart)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .padding(5.dp)
                                                    .size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 2.0B Clean Quick Access Cards: Liked Songs & Downloads (Zero Clutter) ---
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Liked Songs Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .glassySurface(
                                shape = RoundedCornerShape(22.dp),
                                style = GlassyStyle.CARD,
                                isDark = isAmoled,
                                onClick = onNavigateToLikedSongs
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(com.masterwave.player.ui.theme.MasterWaveTurquoise.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Liked Songs",
                                    tint = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Liked Songs",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "${favorites.size} tracks",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    // 2. Downloads Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .glassySurface(
                                shape = RoundedCornerShape(22.dp),
                                style = GlassyStyle.CARD,
                                isDark = isAmoled,
                                onClick = onNavigateToDownloads
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(com.masterwave.player.ui.theme.MasterWaveCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Downloads",
                                    tint = com.masterwave.player.ui.theme.MasterWaveCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Downloads",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = textPrimary
                                )
                                Text(
                                    text = "${downloadedIds.size} tracks",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // --- 2.1 Continue Listening (Recent History) ---
            if (recentTracks.isNotEmpty()) {
                item {
                    RecommendationShelf(
                        title = "Continue Listening",
                        subtitle = "Jump back into what you were playing",
                        tracks = recentTracks.take(10),
                        badge = "RECENT",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                        chipInactiveBg = chipInactiveBg,
                        onTrackClick = { player.playTrack(it, recentTracks) }
                    )
                }
            }

            // --- 2.2 Made For You (Local Hybrid Recommendation Engine) ---
            if (madeForYouTracks.isNotEmpty()) {
                item {
                    RecommendationShelf(
                        title = "Made For You",
                        subtitle = "Tuned to your listening habits & taste profile",
                        tracks = madeForYouTracks,
                        badge = "✦ PERSONALIZED",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                        chipInactiveBg = chipInactiveBg,
                        onTrackClick = { player.playTrack(it, madeForYouTracks) }
                    )
                }
            }

            // --- 2.3 Daily Mix ---
            if (dailyMixTracks.isNotEmpty()) {
                item {
                    RecommendationShelf(
                        title = "Daily Mix",
                        subtitle = "Your top favorites mixed with related tracks",
                        tracks = dailyMixTracks,
                        badge = "DAILY",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                        chipInactiveBg = chipInactiveBg,
                        onTrackClick = { player.playTrack(it, dailyMixTracks) }
                    )
                }
            }

            // --- 2.4 Because You Listened To ---
            if (becauseYouListenedTracks.isNotEmpty()) {
                item {
                    RecommendationShelf(
                        title = becauseYouListenedHeader,
                        subtitle = "Music influenced by your recent exploration",
                        tracks = becauseYouListenedTracks,
                        badge = "ARTIST RADAR",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                        chipInactiveBg = chipInactiveBg,
                        onTrackClick = { player.playTrack(it, becauseYouListenedTracks) }
                    )
                }
            }

            // --- 2.5 Rediscover ---
            if (rediscoverTracks.isNotEmpty()) {
                item {
                    RecommendationShelf(
                        title = "Rediscover",
                        subtitle = "Past favorites you haven't played recently",
                        tracks = rediscoverTracks,
                        badge = "TIMELESS",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = com.masterwave.player.ui.theme.MasterWaveTurquoise,
                        chipInactiveBg = chipInactiveBg,
                        onTrackClick = { player.playTrack(it, rediscoverTracks) }
                    )
                }
            }

            // --- 2.6 My genres horizontal chips (LastWave Page 1) ---
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "My genres",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val myGenres = listOf("Pop", "Rock", "Hip Hop", "Indie", "Electronic", "R&B", "Bollywood", "Punjabi", "Soul")
                val brandTurquoise = com.masterwave.player.ui.theme.MasterWaveTurquoise

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(myGenres.indices.toList()) { index ->
                        val genre = myGenres[index]
                        val dotColor = brandTurquoise
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardBg,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    val tracks = repository.search("$genre hits").songs
                                    if (tracks.isNotEmpty()) {
                                        player.playPlaylist(tracks, 0)
                                        Toast.makeText(context, "Playing $genre radio", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = genre,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // --- 2.2 Discover New Music (LastWave Page 1) ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Discover New Music • $activeLanguage",
                            style = ExpressiveSectionHeader.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "Fresh single releases & daily recommendations",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (isAmoled) Color(0xFF161622) else Color(0xFFE8E5E8),
                        modifier = Modifier.clickable(onClick = onNavigateToSearch)
                    ) {
                        Text(
                            text = "Search >",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(trendingTracks.take(8)) { track ->
                        Column(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { player.playTrack(track) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(135.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(chipInactiveBg)
                            ) {
                                if (!track.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // --- 2.3 Trending community playlists Card (LastWave Page 2) ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Trending community playlists",
                        style = ExpressiveSectionHeader.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Top picks curated by the MasterWave community",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardBg,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .bouncyClickable {
                            val pl = Playlist(
                                id = "comm_global_top_50",
                                title = "Global Top 50 Radar",
                                description = "50 tracks • Updated today • Curated",
                                trackCount = trendingTracks.size.coerceAtLeast(50),
                                tracks = trendingTracks,
                                artworkUrl = trendingTracks.firstOrNull()?.artworkUrl,
                                source = "Community • Global",
                                createdAt = "Updated Today"
                            )
                            onPlaylistSelected(pl)
                        }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val collageArts = trendingTracks.take(4).mapNotNull { it.artworkUrl }
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(chipInactiveBg)
                            ) {
                                if (collageArts.size >= 4) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            AsyncImage(model = collageArts[0], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight())
                                            AsyncImage(model = collageArts[1], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight())
                                        }
                                        Row(modifier = Modifier.weight(1f)) {
                                            AsyncImage(model = collageArts[2], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight())
                                            AsyncImage(model = collageArts[3], contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.weight(1f).fillMaxHeight())
                                        }
                                    }
                                } else if (trendingTracks.isNotEmpty()) {
                                    AsyncImage(
                                        model = trendingTracks.firstOrNull()?.artworkUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Global Top 50 Radar",
                                    style = EditorialHeadlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "50 tracks • Tap to choose songs",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = textSecondary
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF165B33),
                                modifier = Modifier.bouncyClickable {
                                    val pl = Playlist(
                                        id = "comm_global_top_50",
                                        title = "Global Top 50 Radar",
                                        description = "50 tracks • Updated today • Curated",
                                        trackCount = trendingTracks.size.coerceAtLeast(50),
                                        tracks = trendingTracks,
                                        artworkUrl = trendingTracks.firstOrNull()?.artworkUrl,
                                        source = "Community • Global",
                                        createdAt = "Updated Today"
                                    )
                                    onPlaylistSelected(pl)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = "View Songs",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "View Songs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        val previewTracks = trendingTracks.take(3)
                        if (previewTracks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            previewTracks.forEachIndexed { idx, track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { player.playTrack(track) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = textSecondary,
                                        modifier = Modifier.width(20.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(chipInactiveBg)
                                    ) {
                                        if (!track.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = track.artworkUrl,
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. Quick Picks Section (LastWave Page 2: 4-track columns in horizontal scroll) ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Quick picks • $activeLanguage",
                            style = ExpressiveSectionHeader.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = if (currentTrack != null) "Viral $activeLanguage hits • Matching ${currentTrack?.title}" else "Trending viral songs worth playing now",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isAmoled) Color(0xFF161622) else Color(0xFFE8E5E8))
                            .clickable {
                                if (quickPickTracks.isNotEmpty()) {
                                    player.playPlaylist(quickPickTracks.shuffled(), 0)
                                    Toast.makeText(context, "Shuffling quick picks", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // YouTube Music / LastWave Style Quick Picks: Horizontal scroll of 4-track columns
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(quickPickColumns) { columnTracks ->
                        Column(
                            modifier = Modifier.width(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            columnTracks.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(cardBg)
                                        .clickable { player.playTrack(track) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(chipInactiveBg)
                                    ) {
                                        if (!track.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = track.artworkUrl,
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = textSecondary,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp
                                            ),
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(onClick = { player.playTrack(track) }) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = accentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. Fresh finds with NEW badge (LastWave Page 3) ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fresh finds",
                            style = ExpressiveSectionHeader.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 23.sp
                            ),
                            color = textPrimary
                        )
                        Text(
                            text = "Brand new tracks picked for you",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isAmoled) Color(0xFF161622) else Color(0xFFE8E5E8))
                            .clickable {
                                val shuffled = trendingTracks.shuffled()
                                if (shuffled.isNotEmpty()) {
                                    player.playPlaylist(shuffled, 0)
                                    Toast.makeText(context, "Shuffling fresh finds", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(trendingTracks.asReversed().take(8)) { track ->
                        Column(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { player.playTrack(track) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(135.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(chipInactiveBg)
                            ) {
                                if (!track.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF34C759),
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "NEW",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // --- 5. Favorite Artists Section (Real Artist Photography - LastWave Page 3) ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Favorite Artists",
                        style = ExpressiveSectionHeader.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Artists you listen to frequently",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favoriteArtists) { artistName ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(86.dp)
                                .clickable { onNavigateToArtist(artistName) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .border(1.5.dp, com.masterwave.player.ui.theme.MasterWaveTurquoise.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = getArtistPhotoUrl(artistName),
                                    contentDescription = artistName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp
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
            }

            // --- 6. Because you listened to [Top Artist] (LastWave Page 3) ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Because you listened to $topArtist",
                        style = ExpressiveSectionHeader.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp
                        ),
                        color = textPrimary
                    )
                    Text(
                        text = "Similar vibe and recommended tracks",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(similarTracks) { track ->
                        Column(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { player.playTrack(track) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(135.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(chipInactiveBg)
                            ) {
                                if (!track.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // --- 7. Trending Community Playlists Showcase (16+ Daily Updated Playlists for activeLanguage) ---
            item {
                Spacer(modifier = Modifier.height(28.dp))
                val communityPlaylists = remember(activeLanguage) {
                    CommunityPlaylistsProvider.getPlaylistsForLanguage(activeLanguage)
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Community Playlists • $activeLanguage",
                                style = ExpressiveSectionHeader.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 23.sp
                                ),
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "16 trending playlists • Curated daily by community",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = textSecondary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "DAILY SYNC",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Top 4 Featured Big Cards in a horizontal row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(communityPlaylists.take(4)) { playlist ->
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = cardBg,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .width(220.dp)
                                .height(260.dp)
                                .bouncyClickable {
                                    openCommunityPlaylist(playlist)
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = playlist.artworkUrl,
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0x33000000),
                                                    Color(0x66000000),
                                                    Color(0xEE0A0A12),
                                                    Color(0xFF07070E)
                                                )
                                            )
                                        )
                                )

                                Surface(
                                    shape = CircleShape,
                                    color = Color(0x99000000),
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = playlist.trendTag,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = playlist.title,
                                        style = EditorialHeadlineSmall.copy(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${playlist.trackCount} tracks • ${playlist.updatedToday}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF165B33),
                                        modifier = Modifier.bouncyClickable {
                                            openCommunityPlaylist(playlist)
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QueueMusic,
                                                contentDescription = "View Tracks",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "View Songs",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The other 12 Community Playlists in clean vertical cards
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    communityPlaylists.drop(4).forEach { playlist ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = cardBg,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bouncyClickable {
                                    openCommunityPlaylist(playlist)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipInactiveBg)
                                ) {
                                    AsyncImage(
                                        model = playlist.artworkUrl,
                                        contentDescription = playlist.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        ),
                                        color = textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${playlist.description} • ${playlist.trackCount} tracks",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = accentColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = playlist.trendTag,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        openCommunityPlaylist(playlist)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = "View Songs",
                                        tint = accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // --- Floating Curved Header matching Glassy UI Shelf Architecture ---
        val headerShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassySurface(
                        shape = headerShape,
                        style = GlassyStyle.HEADER,
                        isDark = isAmoled,
                        borderWidth = 1.dp,
                        elevation = 8.dp,
                        hasSheen = true
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        // Row 1: Brand Logo ("MASTER(WAVE)"), Audio Engine tag, and Action items
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "MASTERWAVE",
                                        style = NothingHeadlineLarge.copy(
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(com.masterwave.player.ui.theme.MasterWaveTurquoise)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "PRECISION AUDIO ENGINE • 48 kHz",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = textSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Downloads Glassy Slab Button
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .glassySlab(
                                            shape = CircleShape,
                                            isDark = isAmoled,
                                            onClick = onNavigateToDownloads
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Downloads",
                                        tint = textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Search Glassy Slab Button
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .glassySlab(
                                            shape = CircleShape,
                                            isDark = isAmoled,
                                            onClick = onNavigateToSearch
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // User Avatar with glowing halo
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .border(
                                            2.dp,
                                            Brush.sweepGradient(
                                                listOf(
                                                    com.masterwave.player.ui.theme.MasterWaveTurquoise,
                                                    com.masterwave.player.ui.theme.MasterWaveCyan,
                                                    com.masterwave.player.ui.theme.MasterWaveTurquoise
                                                )
                                            ),
                                            CircleShape
                                        )
                                        .bouncyClickable(onClick = onNavigateToSettings),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (userProfile.photoUrl != null && userProfile.isSignedIn) {
                                        AsyncImage(
                                            model = userProfile.photoUrl,
                                            contentDescription = userProfile.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(accentColor.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = userProfile.displayName.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Row 2: Category Selector Glassy Chips
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = category == selectedCategory
                                Box(
                                    modifier = Modifier.glassySurface(
                                        shape = RoundedCornerShape(18.dp),
                                        style = if (isSelected) GlassyStyle.PROMINENT else GlassyStyle.SLAB,
                                        isDark = isAmoled,
                                        tintColor = if (isSelected) accentColor else null,
                                        elevation = if (isSelected) 4.dp else 2.dp,
                                        onClick = {
                                            selectedCategory = category
                                            if (category != "All") {
                                                Toast.makeText(context, "Filtered: $category", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            letterSpacing = 0.2.sp
                                        ),
                                        color = if (isSelected) Color.White else textPrimary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getArtistPhotoUrl(artistName: String): String {
    return com.masterwave.player.data.recommendation.ArtistPhotoProvider.getPhotoUrl(artistName)
}

@Composable
private fun RecommendationShelf(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    badge: String? = null,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    chipInactiveBg: Color,
    onTrackClick: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = ExpressiveSectionHeader.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = textPrimary
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(tracks) { track ->
                Column(
                    modifier = Modifier
                        .width(138.dp)
                        .clickable { onTrackClick(track) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(138.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(chipInactiveBg)
                    ) {
                        if (!track.artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(36.dp).align(Alignment.Center)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
