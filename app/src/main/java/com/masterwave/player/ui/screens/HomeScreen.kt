package com.masterwave.player.ui.screens

import android.widget.Toast
import android.util.Log
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.masterwave.player.data.cloud.CloudAccountManager
import com.masterwave.player.data.cloud.UserProfile
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.model.Track
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.ArtistAvatar
import com.masterwave.player.ui.components.ArtistPickerSheet
import com.masterwave.player.ui.components.GlassyBadge
import com.masterwave.player.ui.components.GlassyBadgeTone
import com.masterwave.player.ui.components.GlassyStyle
import com.masterwave.player.ui.components.bouncyClickable
import com.masterwave.player.ui.components.glassySlab
import com.masterwave.player.ui.components.glassySurface
import com.masterwave.player.ui.theme.DisplayFontFamily
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
import com.masterwave.player.ui.theme.SwissHairline
import com.masterwave.player.ui.theme.SwissHairlineStrong
import com.masterwave.player.ui.theme.SwissRed
import com.masterwave.player.ui.theme.SwissMono
import com.masterwave.player.ui.theme.SwissSectionHeader
import com.masterwave.player.ui.theme.SwissDisplayLarge
import com.masterwave.player.ui.theme.SwissDisplayMedium
import com.masterwave.player.ui.theme.SwissHeadline
import io.github.fletchmckee.liquid.LiquidState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import com.masterwave.player.data.recommendation.CommunityPlaylist
import com.masterwave.player.data.recommendation.CommunityPlaylistsProvider
import kotlinx.coroutines.CancellationException

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
    liquidState: LiquidState? = null,
    isGlassEnabled: Boolean = true,
    onScrollStateChanged: (Boolean) -> Unit = {},
    scrollToTopTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Apple Music keeps controls on one confident accent. The theme owns that colour.
    val accentColor = MaterialTheme.colorScheme.primary
    val isAmoled = ThemeState.isAmoledTheme()
    val favorites by repository.favorites.collectAsState()
    val localTracks by repository.localTracks.collectAsState()
    val recentTracks by repository.recentTracks.collectAsState()
    val homeMixOptions = remember(favorites, recentTracks) {
        HomeMixPersonalizer.build(favorites, recentTracks)
    }

    val cloudManager = remember { CloudAccountManager.getInstance(context, repository) }
    val userProfile by cloudManager.userProfile.collectAsState()

    val currentTrack by player.currentTrack.collectAsState()
    var activeLanguage by remember { mutableStateOf("Trending") }

    val scope = rememberCoroutineScope()
    var trendingTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loadingHomeMix by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentTrack?.id) {
        activeLanguage = com.masterwave.player.data.recommendation.LanguageMusicRecommender
            .detectLanguage(currentTrack)
        try {
            val (lang, res) = com.masterwave.player.data.recommendation.LanguageMusicRecommender.getLanguageViralTracks(
                repository,
                currentTrack
            )
            activeLanguage = lang
            trendingTracks = res
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("HomeScreen", "Could not load language recommendations", e)
            trendingTracks = emptyList()
        }
    }

    var madeForYouTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var dailyMixTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var becauseYouListenedHeader by remember { mutableStateOf("Because You Listened") }
    var becauseYouListenedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var rediscoverTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

    LaunchedEffect(currentTrack?.id, favorites, recentTracks.firstOrNull()?.id) {
        scope.launch {
            try {
                madeForYouTracks = repository.recommendationManager.getMadeForYou(limit = 48)
                dailyMixTracks = repository.recommendationManager.getDailyMix(limit = 24)
                val seed = currentTrack ?: favorites.firstOrNull() ?: repository.recentTracks.value.firstOrNull()
                if (seed != null) {
                    val (header, recs) = repository.recommendationManager.getBecauseYouListenedTo(seed, limit = 16)
                    becauseYouListenedHeader = header
                    becauseYouListenedTracks = recs
                }
                rediscoverTracks = repository.recommendationManager.getRediscoverTracks(limit = 20)
            } catch (_: Exception) {}
        }
    }

    // Smart Sonic Flow (Dynamic User Taste Radio - Updates on song change)
    var flowTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

    LaunchedEffect(currentTrack?.id, favorites.size, recentTracks.firstOrNull()?.id) {
        scope.launch {
            try {
                val seed = currentTrack ?: favorites.firstOrNull() ?: recentTracks.firstOrNull()
                if (seed != null) {
                    val (_, recs) = repository.recommendationManager.getBecauseYouListenedTo(seed, limit = 24)
                    if (recs.isNotEmpty()) {
                        flowTracks = recs
                    } else {
                        val made = repository.recommendationManager.getMadeForYou(limit = 24)
                        flowTracks = made
                    }
                } else {
                    val made = repository.recommendationManager.getMadeForYou(limit = 24)
                    flowTracks = made
                }
            } catch (_: Exception) {
                flowTracks = madeForYouTracks
            }
        }
    }

    val topArtist = remember(trendingTracks) {
        trendingTracks.firstOrNull()?.artist?.takeIf { it.isNotBlank() } ?: "Sai Abhyankkar"
    }

    val similarTracks = remember(trendingTracks) {
        trendingTracks.drop(2).take(6)
    }

    val followedArtists by repository.followedArtists.collectAsState()
    var isArtistPickerOpen by remember { mutableStateOf(false) }

    val bgColor = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainerLow
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val chipInactiveBg = MaterialTheme.colorScheme.surfaceContainerHigh

    fun openCommunityPlaylist(playlist: CommunityPlaylist) {
        scope.launch {
            val fetchedSongs = try {
                repository.search(playlist.searchQuery).songs
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HomeScreen", "Could not load playlist search: ${playlist.searchQuery}", e)
                Toast.makeText(context, "Couldn't load this playlist. Check your connection and try again.", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (fetchedSongs.isEmpty()) {
                Toast.makeText(context, "No songs were found for this playlist.", Toast.LENGTH_LONG).show()
                return@launch
            }
            val pl = Playlist(
                id = playlist.id,
                title = playlist.title,
                description = "Songs matching: ${playlist.searchQuery}",
                trackCount = fetchedSongs.size,
                tracks = fetchedSongs,
                artworkUrl = playlist.artworkUrl,
                source = "YouTube Music search"
            )
            onPlaylistSelected(pl)
        }
    }

    fun openTrendingPlaylist() {
        if (trendingTracks.isEmpty()) {
            Toast.makeText(context, "No trending tracks are available right now.", Toast.LENGTH_SHORT).show()
            return
        }
        onPlaylistSelected(
            Playlist(
                id = "trending_$activeLanguage",
                title = "Trending now",
                description = "Popular tracks right now",
                trackCount = trendingTracks.size,
                tracks = trendingTracks,
                artworkUrl = trendingTracks.firstOrNull()?.artworkUrl,
                source = "YouTube Music search"
            )
        )
    }

    val personalizedHeroTracks = remember(
        madeForYouTracks,
        dailyMixTracks,
        becauseYouListenedTracks,
        rediscoverTracks,
        favorites
    ) {
        madeForYouTracks + dailyMixTracks + becauseYouListenedTracks + rediscoverTracks + favorites
    }
    var previousCarouselTracks by remember { mutableStateOf(emptyList<Track>()) }
    var heroSelection by remember {
        mutableStateOf(HomeHeroSelection(emptyList(), isPersonalized = false))
    }
    LaunchedEffect(personalizedHeroTracks, trendingTracks, recentTracks, currentTrack?.id) {
        val selection = HomeHeroRecommendations.build(
            personalizedTracks = personalizedHeroTracks,
            trendingTracks = trendingTracks,
            recentlyPlayedTracks = recentTracks + listOfNotNull(currentTrack),
            previousCarouselTracks = previousCarouselTracks,
            limit = 24
        )
        heroSelection = selection
        if (selection.tracks.isNotEmpty()) previousCarouselTracks = selection.tracks
    }
    val heroTracks = heroSelection.tracks
    val quickPickTracks = remember(
        personalizedHeroTracks,
        trendingTracks,
        heroTracks,
        recentTracks,
        currentTrack?.id
    ) {
        HomeHeroRecommendations.buildQuickPicks(
            personalizedTracks = personalizedHeroTracks,
            trendingTracks = trendingTracks,
            carouselTracks = heroTracks,
            recentlyPlayedTracks = recentTracks + listOfNotNull(currentTrack),
            limit = 24
        )
    }

    var selectedCategory by remember { mutableStateOf("All") }
    // Mood-first browse row. Language matching still happens silently behind the scenes.
    val categories = listOf(
        "All", "Trending", "New Releases", "Podcasts", "Playlists", "Top Charts"
    )

    val indianCategories = remember {
        setOf(
            "Hindi", "Tamil", "Telugu", "Punjabi", "Malayalam", "Kannada", "Bengali", "Marathi",
            "Hindustani", "Carnatic", "Classical", "Devotional", "Indie"
        )
    }

    var categoryFilterTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoadingCategoryFilter by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != "All") {
            isLoadingCategoryFilter = true
            try {
                val results = if (selectedCategory in indianCategories) {
                    repository.getIndianMusic(selectedCategory, limit = 25)
                } else {
                    val query = when (selectedCategory) {
                        "Trending" -> "trending global top songs"
                        "New Releases" -> "new music releases"
                        "Podcasts" -> "podcast episodes music discussion"
                        "Playlists" -> "top hit music playlists"
                        "Top Charts" -> "billboard top charts music"
                        else -> "$selectedCategory hits"
                    }
                    repository.search(query).songs
                }
                categoryFilterTracks = results
            } catch (_: Exception) {
                categoryFilterTracks = emptyList()
            } finally {
                isLoadingCategoryFilter = false
            }
        }
    }

    val continueListeningList = remember(recentTracks, madeForYouTracks, dailyMixTracks) {
        if (recentTracks.size >= 4) {
            recentTracks.take(20)
        } else {
            val additional = (madeForYouTracks + dailyMixTracks).filter { cand ->
                recentTracks.none { r -> r.id == cand.id }
            }
            (recentTracks + additional).take(20)
        }
    }

    val homeListState = rememberLazyListState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val headerHeight = if (headerHeightPx == 0) {
        168.dp
    } else {
        with(density) { headerHeightPx.toDp() }
    }
    val isScrolled = homeListState.firstVisibleItemIndex > 0 ||
        homeListState.firstVisibleItemScrollOffset > 0

    // Tell the shell when the feed starts moving so the bottom dock can split.
    LaunchedEffect(homeListState, onScrollStateChanged) {
        snapshotFlow {
            homeListState.firstVisibleItemIndex > 0 ||
                homeListState.firstVisibleItemScrollOffset > 25
        }
            .distinctUntilChanged()
            .collect { onScrollStateChanged(it) }
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0L) {
            homeListState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = headerHeight + 8.dp, bottom = 170.dp)
        ) {
            // --- 1. Greeting, Infinite Radio Hero Card, My genres, and Discover New Music (Reference Page 1) ---
            if (selectedCategory == "All") {
                // Greeting & Uppercase Date
                item {
                    val currentHour = remember { java.time.LocalTime.now().hour }
                    val greeting = when (currentHour) {
                        in 5..11 -> "Good morning"
                        in 12..16 -> "Good afternoon"
                        else -> "Good evening"
                    }
                    val formattedDate = remember {
                        java.time.LocalDate.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d").withLocale(java.util.Locale.ENGLISH)
                        ).uppercase()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            style = SwissMono.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            ),
                            color = textSecondary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = greeting,
                            style = SwissDisplayMedium.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.6).sp
                            ),
                            color = textPrimary
                        )
                    }
                }

                // Smart Sonic Flow (Dynamic User Taste Radio - Updates on song change)
                // --- Smart Sonic Flow (Big Style Featured Showcase) ---
                item {
                    val activeFlowTracks = if (flowTracks.isNotEmpty()) flowTracks else heroTracks
                    val heroTrack = activeFlowTracks.firstOrNull()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        // Section Header: Clean, Bold, Confident
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Smart Sonic Flow",
                                style = SwissDisplayMedium.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = textPrimary
                            )

                            // Play All Action Button
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accentColor,
                                modifier = Modifier.bouncyClickable(scaleDown = 0.92f) {
                                    if (activeFlowTracks.isNotEmpty()) {
                                        player.playPlaylist(activeFlowTracks, 0)
                                        Toast.makeText(context, "Playing Smart Sonic Flow", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Play Flow",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Featured Hero Showcase Card (Big Style)
                        if (heroTrack != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .padding(horizontal = 20.dp)
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(26.dp),
                                        spotColor = Color.Black.copy(alpha = if (isAmoled) 0.6f else 0.20f)
                                    )
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(
                                        if (isAmoled) Color(0xFF161824) else Color(0xFFF5F0E0)
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = if (isAmoled) 0.35f else 0.70f),
                                                Color.White.copy(alpha = if (isAmoled) 0.06f else 0.12f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .bouncyClickable(scaleDown = 0.98f) {
                                        player.playPlaylist(activeFlowTracks, 0)
                                    }
                            ) {
                                // Background Artwork
                                if (!heroTrack.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = heroTrack.artworkUrl,
                                        contentDescription = heroTrack.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Dark acrylic gradient scrim
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                0.0f to Color.Transparent,
                                                0.35f to Color.Black.copy(alpha = 0.30f),
                                                0.70f to Color.Black.copy(alpha = 0.75f),
                                                1.0f to Color.Black.copy(alpha = 0.92f)
                                            )
                                        )
                                )

                                // Foreground Information & Large Play Action
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(22.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = heroTrack.title,
                                                fontFamily = DisplayFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 24.sp,
                                                letterSpacing = (-0.6).sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = heroTrack.artist,
                                                fontFamily = DisplayFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                color = Color.White.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // Big Radiant Play Jewel
                                        Box(
                                            modifier = Modifier
                                                .size(58.dp)
                                                .shadow(10.dp, CircleShape, spotColor = accentColor.copy(alpha = 0.6f))
                                                .clip(CircleShape)
                                                .background(accentColor)
                                                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                                .bouncyClickable(scaleDown = 0.90f) {
                                                    player.playPlaylist(activeFlowTracks, 0)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Large Carousel of Remaining Flow Recommendations (Big 150dp Cards)
                        val flowCarousel = activeFlowTracks.drop(1).take(10)
                        if (flowCarousel.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(flowCarousel) { track ->
                                    val trackIndex = activeFlowTracks.indexOf(track)
                                    Column(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .bouncyClickable(scaleDown = 0.96f) {
                                                player.playPlaylist(activeFlowTracks, trackIndex)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(150.dp)
                                                .shadow(
                                                    elevation = 6.dp,
                                                    shape = RoundedCornerShape(18.dp),
                                                    spotColor = Color.Black.copy(alpha = if (isAmoled) 0.45f else 0.12f)
                                                )
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(if (isAmoled) Color(0xFF1B1E29) else Color(0xFFF5F0E0))
                                                .border(
                                                    BorderStroke(
                                                        1.dp,
                                                        if (isAmoled) Color(0x28FFFFFF) else Color(0xFF0A0A0A)
                                                    ),
                                                    RoundedCornerShape(18.dp)
                                                )
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
                                                        .size(38.dp)
                                                        .align(Alignment.Center)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
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

                // Discover New Music Shelf
                item {
                    val discoverTracks = if (trendingTracks.isNotEmpty()) trendingTracks else heroTracks
                    if (discoverTracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(22.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Discover New Music",
                                style = SwissSectionHeader.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = textPrimary
                            )

                            Text(
                                text = "> See all",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = accentColor,
                                modifier = Modifier.bouncyClickable { onNavigateToSearch() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(discoverTracks.take(10)) { track ->
                                Column(
                                    modifier = Modifier
                                        .width(135.dp)
                                        .bouncyClickable { player.playTrack(track, discoverTracks) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(135.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isAmoled) Color(0xFF1B1E29) else Color(0xFFF5F0E0))
                                            .border(BorderStroke(1.dp, if (isAmoled) Color(0x25FFFFFF) else Color(0xFF0A0A0A)), RoundedCornerShape(14.dp))
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
                                                    .size(32.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
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

            if (selectedCategory == "All" && quickPickTracks.isNotEmpty()) {
                item {
                    QuickPicksSection(
                        tracks = quickPickTracks,
                        isAmoled = isAmoled,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        cardBg = cardBg,
                        chipInactiveBg = chipInactiveBg,
                        onShuffle = {
                            player.playPlaylist(quickPickTracks.shuffled(), 0)
                            Toast.makeText(context, "Shuffling quick picks", Toast.LENGTH_SHORT).show()
                        },
                        onTrackClick = { player.playTrack(it, quickPickTracks) }
                    )
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
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = accentColor,
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

            // --- 2.1 Continue Listening (Recent History & Multi-Track Jump Back In) ---
            if (continueListeningList.isNotEmpty()) {
                item {
                    RecommendationShelf(
                        title = "Continue Listening",
                        tracks = continueListeningList,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = accentColor,
                        chipInactiveBg = chipInactiveBg,
                        onTrackClick = { player.playTrack(it, continueListeningList) }
                    )
                }
            }

            // --- 2.2 Made For You (Local Hybrid Recommendation Engine) ---
            if (madeForYouTracks.isNotEmpty()) {
                item {
                    RecommendationShelf(
                        title = "Made For You",
                        tracks = madeForYouTracks,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = accentColor,
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
                        tracks = dailyMixTracks,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = accentColor,
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
                        tracks = becauseYouListenedTracks,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = accentColor,
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
                        tracks = rediscoverTracks,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        accentColor = accentColor,
                        chipInactiveBg = chipInactiveBg,
                        onTrackClick = { player.playTrack(it, rediscoverTracks) }
                    )
                }
            }

            // --- 2.6 Personalized mixes from favorites and listening history ---
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    val sectionTitle = "Your Artists & Mixes"

                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = homeMixOptions,
                        key = { "${it.kind.name}_${it.label.lowercase(Locale.ROOT)}" }
                    ) { mix ->
                        val isLoading = loadingHomeMix == mix.searchQuery
                        val icon = when (mix.kind) {
                            HomeMixKind.ARTIST -> Icons.Filled.Person
                            HomeMixKind.STARTER -> Icons.Filled.Explore
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = cardBg,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isAmoled) {
                                    Color.White.copy(alpha = 0.10f)
                                } else {
                                    Color.Black.copy(alpha = 0.07f)
                                }
                            ),
                            modifier = Modifier.clickable(enabled = loadingHomeMix == null) {
                                scope.launch {
                                    loadingHomeMix = mix.searchQuery
                                    try {
                                        val tracks = repository.search(mix.searchQuery).songs
                                            .filter { it.isPlayable }
                                        if (tracks.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                "No playable tracks found for ${mix.label}.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            player.playPlaylist(tracks, 0)
                                            Toast.makeText(
                                                context,
                                                "Playing ${mix.label} mix",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Couldn't load ${mix.label}. Check your connection and try again.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        loadingHomeMix = null
                                    }
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .padding(horizontal = 14.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = accentColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = mix.label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 148.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 2.2 Discover New Music (LastWave Page 1) ---
            if (trendingTracks.isNotEmpty()) item {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = SwissHairline,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Releases",
                        style = SwissSectionHeader.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp
                        ),
                        color = textPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable(onClick = onNavigateToSearch)
                    ) {
                        Text(
                            text = "SEE ALL",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp,
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
                                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
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
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    letterSpacing = (-0.1).sp
                                ),
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // --- Language-matched trending tracks ---
            if (trendingTracks.isNotEmpty()) item {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = SwissHairline,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Text(
                    text = "Trending",
                    style = SwissSectionHeader.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp
                    ),
                    color = textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .bouncyClickable {
                            openTrendingPlaylist()
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
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(chipInactiveBg)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
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
                                    text = "Trending Now",
                                    style = EditorialHeadlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${trendingTracks.size} songs • Tap to explore",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        letterSpacing = (-0.1).sp
                                    ),
                                    color = textSecondary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = accentColor,
                                modifier = Modifier.bouncyClickable {
                                    openTrendingPlaylist()
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
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "VIEW",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.5.sp,
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
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { player.playTrack(track) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = textSecondary,
                                        modifier = Modifier.width(22.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(chipInactiveBg)
                                            .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
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
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                letterSpacing = (-0.2).sp
                                            ),
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.5.sp,
                                                letterSpacing = (-0.1).sp
                                            ),
                                            color = textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = accentColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- 4. Fresh finds with NEW badge (LastWave Page 3) ---
            if (trendingTracks.isNotEmpty()) item {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = SwissHairline,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "More to Explore",
                        style = SwissSectionHeader.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp
                        ),
                        color = textPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isAmoled) Color(0xFF161622) else Color(0xFFF5F0E0))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
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
                            modifier = Modifier.size(18.dp)
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
                                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
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
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    letterSpacing = (-0.1).sp
                                ),
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // --- 5. Favorite Artists Section (Persistent, Real Artist Photos & Interactive Add/Edit) ---
            item {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = SwissHairline,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Artists",
                        style = SwissSectionHeader.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp
                        ),
                        color = textPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { isArtistPickerOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Artist",
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ADD",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(followedArtists, key = { it.name }) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(86.dp)
                                .clickable { onNavigateToArtist(artist.name) }
                        ) {
                            ArtistAvatar(
                                name = artist.name,
                                photoUrl = null,
                                size = 76.dp,
                                borderColor = accentColor.copy(alpha = 0.4f),
                                borderWidth = 1.5.dp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = artist.name,
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                        }
                    }

                    // Trailing "+ Add" card in the row
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(86.dp)
                                .clickable { isArtistPickerOpen = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isAmoled) Color(0xFF141624) else Color(0xFFF5F0E0))
                                    .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add More Artists",
                                    tint = accentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Add Artist",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = accentColor,
                                maxLines = 1
                            )

                            Text(
                                text = "Explore",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = textSecondary
                            )
                        }
                    }
                }
            }

            // --- 6. Because you listened to [Top Artist] (LastWave Page 3) ---
            if (similarTracks.isNotEmpty()) item {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = SwissHairline,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Text(
                    text = "Because you listened to $topArtist",
                    style = SwissSectionHeader.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp
                    ),
                    color = textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

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
                                    .border(0.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
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
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    letterSpacing = (-0.1).sp
                                ),
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // --- Curated playlist search shortcuts ---
            item {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(
                    color = SwissHairline,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                val communityPlaylists = remember(activeLanguage) {
                    CommunityPlaylistsProvider.getPlaylistsForLanguage(activeLanguage).take(8)
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Trending community playlists",
                        style = SwissSectionHeader.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp
                        ),
                        color = textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(communityPlaylists, key = { it.id }) { playlist ->
                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .bouncyClickable { openCommunityPlaylist(playlist) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(chipInactiveBg)
                                    .border(1.dp, if (isAmoled) Color(0x28FFFFFF) else Color(0xFF0A0A0A), RoundedCornerShape(16.dp))
                            ) {
                                AsyncImage(
                                    model = playlist.artworkUrl,
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Top Trend Badge Pill
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = playlist.trendTag.uppercase(),
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.6.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                // Subtle Bottom Gradient Scrim
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f))
                                            )
                                        )
                                )

                                // Bottom Track Count Meta
                                Text(
                                    text = "${playlist.trackCount} TRACKS",
                                    style = SwissMono.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                )

                                // Action Play Button (Liquid Glass preserved)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(36.dp)
                                        .glassySurface(
                                            shape = CircleShape,
                                            style = GlassyStyle.CARD,
                                            tintColor = accentColor,
                                            onClick = { openCommunityPlaylist(playlist) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Explore ${playlist.title}",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    letterSpacing = (-0.2).sp
                                ),
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    letterSpacing = (-0.1).sp
                                ),
                                color = textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        HomeScreenHeader(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f)
                .onSizeChanged { headerHeightPx = it.height },
            isScrolled = isScrolled,
            liquidState = liquidState,
            isGlassEnabled = isGlassEnabled,
            isAmoled = isAmoled,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            accentColor = accentColor,
            userProfile = userProfile,
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { category ->
                selectedCategory = category
                if (category != "All") {
                    Toast.makeText(context, "Filtered: $category", Toast.LENGTH_SHORT).show()
                }
            },
            onNavigateToDownloads = onNavigateToDownloads,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToSettings = onNavigateToSettings
        )
    }

    // Interactive Favorite Artists Management Sheet
    if (isArtistPickerOpen) {
        ArtistPickerSheet(
            repository = repository,
            onDismiss = { isArtistPickerOpen = false }
        )
    }
}

// --- Dedicated Measured Header Layer with Liquid Glass Surface ---
@Composable
private fun HomeScreenHeader(
    modifier: Modifier = Modifier,
    isScrolled: Boolean,
    liquidState: LiquidState?,
    isGlassEnabled: Boolean,
    isAmoled: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    userProfile: UserProfile,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val headerShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassySurface(
                shape = headerShape,
                style = GlassyStyle.DOCK,
                isDark = isAmoled,
                isGlassEnabled = isGlassEnabled,
                borderWidth = 1.dp,
                elevation = 10.dp,
                accentGlow = accentColor.copy(alpha = if (isScrolled) 0.12f else 0.08f),
                tintColor = accentColor,
                hasSheen = true,
                liquidState = liquidState
            )
    ) {
        if (isScrolled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            )
        }

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
                // Masthead (Reference Page 1): Clean "Home" title + Download, Search, Profile buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Home",
                        style = SwissDisplayMedium.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.6).sp
                        ),
                        color = textPrimary,
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Download button
                        IconButton(
                            onClick = onNavigateToDownloads,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Downloads",
                                tint = textPrimary,
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        // Search button
                        IconButton(
                            onClick = onNavigateToSearch,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = textPrimary,
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        // User Avatar button (Settings)
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userProfile.photoUrl != null && userProfile.isSignedIn) {
                                    AsyncImage(
                                        model = userProfile.photoUrl,
                                        contentDescription = userProfile.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Row 2: Category Selector Capsule Pills (Reference Page 1)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) accentColor else if (isAmoled) Color(0x28FFFFFF) else Color.White,
                            border = if (isSelected) null else BorderStroke(1.dp, if (isAmoled) Color(0x38FFFFFF) else Color(0xFFE0E6DD)),
                            modifier = Modifier
                                .height(36.dp)
                                .bouncyClickable { onCategorySelected(category) }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = if (isSelected) Color.White else textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPicksSection(
    tracks: List<Track>,
    isAmoled: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    cardBg: Color,
    chipInactiveBg: Color,
    onShuffle: () -> Unit,
    onTrackClick: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    val columns = remember(tracks) { tracks.chunked(3) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
        HorizontalDivider(
            color = SwissHairline,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Picks",
                style = SwissSectionHeader.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                ),
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onShuffle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle quick picks",
                    tint = textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(columns) { columnTracks ->
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
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .clickable { onTrackClick(track) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(chipInactiveBg)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
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
                                            .size(22.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        letterSpacing = (-0.1).sp
                                    ),
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
    }
}

fun getArtistPhotoUrl(artistName: String): String {
    return com.masterwave.player.data.recommendation.ArtistPhotoProvider.getPhotoUrl(artistName)
}

@Composable
private fun RecommendationShelf(
    title: String,
    tracks: List<Track>,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    chipInactiveBg: Color,
    onTrackClick: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
        HorizontalDivider(
            color = SwissHairline,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = SwissSectionHeader.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp
                ),
                color = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(tracks) { track ->
                Column(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable { onTrackClick(track) }
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f)),
                        color = chipInactiveBg,
                        modifier = Modifier
                            .size(135.dp)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        if (!track.artworkUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            letterSpacing = (-0.1).sp
                        ),
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
