package com.masterwave.player

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.masterwave.player.ui.theme.ThemeState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import kotlinx.coroutines.launch
import com.masterwave.player.data.download.MusicDownloadManager
import com.masterwave.player.data.model.Album
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlaybackService
import com.masterwave.player.ui.components.FirstStartPermissionsDialog
import com.masterwave.player.ui.components.FloatingNavigationDock
import com.masterwave.player.ui.components.MainTab
import com.masterwave.player.ui.components.MiniPlayerDock
import com.masterwave.player.ui.components.hasRequiredPermissions
import com.masterwave.player.ui.screens.AiHubScreen
import com.masterwave.player.ui.screens.AlbumDetailScreen
import com.masterwave.player.ui.screens.AppUpdateScreen
import com.masterwave.player.ui.screens.AppearanceScreen
import com.masterwave.player.ui.screens.ArtistScreen
import com.masterwave.player.ui.screens.AudioPlaybackScreen
import com.masterwave.player.ui.screens.BackupRestoreScreen
import com.masterwave.player.ui.screens.ConnectionDiagnosticsScreen
import com.masterwave.player.ui.screens.DownloadsScreen
import com.masterwave.player.ui.screens.GeneratorScreen
import com.masterwave.player.ui.screens.HomeScreen
import com.masterwave.player.ui.screens.LikedSongsScreen
import com.masterwave.player.ui.screens.LyricsScreen
import com.masterwave.player.ui.screens.NowPlayingScreen
import com.masterwave.player.ui.screens.PlaylistDetailScreen
import com.masterwave.player.ui.screens.PlaylistsScreen
import com.masterwave.player.ui.screens.QueueScreen
import com.masterwave.player.ui.screens.SearchScreen
import com.masterwave.player.ui.screens.SettingsScreen
import com.masterwave.player.ui.screens.SpotifySyncScreen
import com.masterwave.player.ui.screens.StatsScreen
import com.masterwave.player.ui.screens.SupportCommunityScreen
import com.masterwave.player.ui.screens.YouTubeSyncScreen
import com.masterwave.player.ui.theme.LightBackground
import com.masterwave.player.ui.theme.MasterWaveTheme

class MainActivity : ComponentActivity() {

    private var playbackService: MasterWavePlaybackService? = null
    private val isServiceBoundState = mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MasterWavePlaybackService.LocalBinder
            playbackService = binder?.getService()
            isServiceBoundState.value = playbackService != null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBoundState.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable Hardware Window-Level Blur on Android 12+ (Snapdragon 6s Gen 3)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = 40
        }

        val serviceIntent = Intent(this, MasterWavePlaybackService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            MasterWaveTheme {
                MasterWaveRoot(
                    playbackService = if (isServiceBoundState.value) playbackService else null
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBoundState.value) {
            unbindService(serviceConnection)
        }
    }
}

@Composable
fun MasterWaveRoot(playbackService: MasterWavePlaybackService?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { MusicRepository.getInstance(MasterWaveApp.instance) }

    var showPermissionsDialog by remember {
        mutableStateOf(!hasRequiredPermissions(context))
    }

    if (showPermissionsDialog) {
        FirstStartPermissionsDialog(
            onPermissionsGranted = {
                showPermissionsDialog = false
                scope.launch {
                    repository.loadLocalTracks()
                }
                MusicDownloadManager.getInstance(context, repository.okHttpClient, repository).scanDownloadedFiles()
            },
            onDismiss = {
                showPermissionsDialog = false
                scope.launch {
                    repository.loadLocalTracks()
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        repository.loadLocalTracks()
        MusicDownloadManager.getInstance(context, repository.okHttpClient, repository).scanDownloadedFiles()
    }

    if (playbackService == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val player = playbackService.player
    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val isLoading by player.isLoading.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()

    // Dynamic Artwork Color Extraction - Isolated to Now Playing ambient backdrop
    LaunchedEffect(currentTrack?.id, currentTrack?.artworkUrl) {
        if (currentTrack != null) {
            val extracted = com.masterwave.player.ui.theme.ArtworkColorExtractor.extractVibrantColor(context, currentTrack?.artworkUrl)
            com.masterwave.player.ui.theme.ThemeState.setArtworkColor(extracted)
        } else {
            com.masterwave.player.ui.theme.ThemeState.setArtworkColor(null)
        }
    }

    // Navigation States (Apple Music Liquid Glass Dock: Home, Search, Library)
    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var isLyricsOpen by remember { mutableStateOf(false) }
    var isQueueOpen by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isAppearanceOpen by remember { mutableStateOf(false) }
    var isAudioPlaybackOpen by remember { mutableStateOf(false) }
    var isBackupRestoreOpen by remember { mutableStateOf(false) }
    var isAppUpdateOpen by remember { mutableStateOf(false) }
    var isSupportOpen by remember { mutableStateOf(false) }
    var isYouTubeSyncOpen by remember { mutableStateOf(false) }
    var isSpotifySyncOpen by remember { mutableStateOf(false) }
    var isDiagnosticsOpen by remember { mutableStateOf(false) }
    var isAiHubOpen by remember { mutableStateOf(false) }
    var isDownloadsOpen by remember { mutableStateOf(false) }
    var isLikedSongsOpen by remember { mutableStateOf(false) }
    var isGeneratorOpen by remember { mutableStateOf(false) }
    var selectedArtistForDetail by remember { mutableStateOf<String?>(null) }
    var selectedAlbumForDetail by remember { mutableStateOf<Album?>(null) }
    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }

    // Back handling for nested views
    BackHandler(
        enabled = isNowPlayingExpanded || isLyricsOpen || isQueueOpen ||
                isSearchOpen || isSettingsOpen || isAppearanceOpen ||
                isAudioPlaybackOpen || isBackupRestoreOpen || isAppUpdateOpen || isSupportOpen ||
                isYouTubeSyncOpen || isSpotifySyncOpen ||
                isDiagnosticsOpen || isAiHubOpen || isDownloadsOpen ||
                isLikedSongsOpen || isGeneratorOpen || selectedArtistForDetail != null ||
                selectedAlbumForDetail != null ||
                selectedPlaylistForDetail != null ||
                currentTab != MainTab.HOME
    ) {
        when {
            selectedAlbumForDetail != null -> selectedAlbumForDetail = null
            selectedArtistForDetail != null -> selectedArtistForDetail = null
            isGeneratorOpen -> isGeneratorOpen = false
            isLikedSongsOpen -> isLikedSongsOpen = false
            isDownloadsOpen -> isDownloadsOpen = false
            isAiHubOpen -> isAiHubOpen = false
            selectedPlaylistForDetail != null -> selectedPlaylistForDetail = null
            isDiagnosticsOpen -> isDiagnosticsOpen = false
            isLyricsOpen -> isLyricsOpen = false
            isQueueOpen -> isQueueOpen = false
            isNowPlayingExpanded -> isNowPlayingExpanded = false
            isAppearanceOpen -> isAppearanceOpen = false
            isAudioPlaybackOpen -> isAudioPlaybackOpen = false
            isBackupRestoreOpen -> isBackupRestoreOpen = false
            isAppUpdateOpen -> isAppUpdateOpen = false
            isSupportOpen -> isSupportOpen = false
            isYouTubeSyncOpen -> isYouTubeSyncOpen = false
            isSpotifySyncOpen -> isSpotifySyncOpen = false
            isSettingsOpen -> isSettingsOpen = false
            isSearchOpen -> isSearchOpen = false
            currentTab != MainTab.HOME -> currentTab = MainTab.HOME
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tab Content filling full screen - LazyColumns can scroll completely underneath the floating dock
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Animated Tab Switching with Spring Physics
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> width / 3 } + fadeOut())
                    }
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    MainTab.HOME -> HomeScreen(
                        player = player,
                        repository = repository,
                        onNavigateToSearch = { currentTab = MainTab.SEARCH },
                        onNavigateToSettings = { isSettingsOpen = true },
                        onNavigateToDownloads = { isDownloadsOpen = true },
                        onNavigateToArtist = { selectedArtistForDetail = it },
                        onNavigateToLikedSongs = { isLikedSongsOpen = true },
                        onNavigateToGenerator = { isGeneratorOpen = true },
                        onPlaylistSelected = { playlist -> selectedPlaylistForDetail = playlist }
                    )
                    MainTab.SEARCH -> SearchScreen(
                        player = player,
                        repository = repository,
                        onBack = { currentTab = MainTab.HOME },
                        onArtistSelected = { selectedArtistForDetail = it },
                        onAlbumSelected = { selectedAlbumForDetail = it },
                        onExpandNowPlaying = { isNowPlayingExpanded = true }
                    )
                    MainTab.LIBRARY -> PlaylistsScreen(
                        player = player,
                        repository = repository,
                        onPlaylistSelected = { playlist ->
                            selectedPlaylistForDetail = playlist
                        },
                        onNavigateToLikedSongs = { isLikedSongsOpen = true },
                        onNavigateToDownloads = { isDownloadsOpen = true },
                        onNavigateToGenerator = { isGeneratorOpen = true }
                    )
                }
            }
        }

        // Floating Dock & MiniPlayer Stack pinned to bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentTrack != null) {
                    MiniPlayerDock(
                        track = currentTrack!!,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onExpand = { isNowPlayingExpanded = true },
                        onPlayPause = { player.togglePlayPause() },
                        onNext = { player.skipToNext() }
                    )
                }

                FloatingNavigationDock(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        }

        // --- Fullscreen Search Overlay ---
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SearchScreen(
                player = player,
                repository = repository,
                onBack = { isSearchOpen = false },
                onArtistSelected = { selectedArtistForDetail = it },
                onAlbumSelected = { selectedAlbumForDetail = it },
                onExpandNowPlaying = { isNowPlayingExpanded = true }
            )
        }

        // --- Fullscreen Settings Overlay ---
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SettingsScreen(
                onBack = { isSettingsOpen = false },
                onNavigateToAppearance = { isAppearanceOpen = true },
                onNavigateToAudioPlayback = { isAudioPlaybackOpen = true },
                onNavigateToAppUpdate = { isAppUpdateOpen = true },
                onNavigateToSupport = { isSupportOpen = true },
                onNavigateToBackupRestore = { isBackupRestoreOpen = true },
                onNavigateToYouTubeSync = { isYouTubeSyncOpen = true },
                onNavigateToSpotifySync = { isSpotifySyncOpen = true }
            )
        }

        // Sub-screen: Backup & Restore (Launcher-grade Snapshot Import & Export)
        AnimatedVisibility(
            visible = isBackupRestoreOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            BackupRestoreScreen(
                player = player,
                repository = repository,
                onBack = { isBackupRestoreOpen = false }
            )
        }

        // Sub-screen: YouTube & Sync
        AnimatedVisibility(
            visible = isYouTubeSyncOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            YouTubeSyncScreen(
                player = player,
                repository = repository,
                onBack = { isYouTubeSyncOpen = false }
            )
        }

        // Sub-screen: Spotify & Sync
        AnimatedVisibility(
            visible = isSpotifySyncOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SpotifySyncScreen(
                player = player,
                repository = repository,
                onBack = { isSpotifySyncOpen = false }
            )
        }

        // Sub-screen: Appearance & Visuals
        AnimatedVisibility(
            visible = isAppearanceOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            AppearanceScreen(onBack = { isAppearanceOpen = false })
        }

        // Sub-screen: Audio & Playback
        AnimatedVisibility(
            visible = isAudioPlaybackOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            AudioPlaybackScreen(onBack = { isAudioPlaybackOpen = false })
        }

        // Sub-screen: App Update
        AnimatedVisibility(
            visible = isAppUpdateOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            AppUpdateScreen(onBack = { isAppUpdateOpen = false })
        }

        // Sub-screen: Support & Community
        AnimatedVisibility(
            visible = isSupportOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SupportCommunityScreen(onBack = { isSupportOpen = false })
        }

        // Sub-screen: Playlist Detail
        AnimatedVisibility(
            visible = selectedPlaylistForDetail != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            selectedPlaylistForDetail?.let { pl ->
                PlaylistDetailScreen(
                    playlist = pl,
                    player = player,
                    repository = repository,
                    onBack = { selectedPlaylistForDetail = null }
                )
            }
        }

        // Sub-screen: Connection Diagnostics
        AnimatedVisibility(
            visible = isDiagnosticsOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            ConnectionDiagnosticsScreen(
                player = player,
                repository = repository,
                onBack = { isDiagnosticsOpen = false }
            )
        }

        // Fullscreen Now Playing Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlayingScreen(
                player = player,
                repository = repository,
                onCollapse = { isNowPlayingExpanded = false },
                onOpenLyrics = { isLyricsOpen = true },
                onOpenQueue = { isQueueOpen = true }
            )
        }

        // Fullscreen Lyrics Overlay
        AnimatedVisibility(
            visible = isLyricsOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            LyricsScreen(
                player = player,
                repository = repository,
                onBack = { isLyricsOpen = false }
            )
        }

        // Fullscreen Queue Overlay
        AnimatedVisibility(
            visible = isQueueOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            QueueScreen(
                player = player,
                repository = repository,
                onBack = { isQueueOpen = false }
            )
        }

        // Sub-screen: AI Hub & Smart Addons
        AnimatedVisibility(
            visible = isAiHubOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            AiHubScreen(onBack = { isAiHubOpen = false })
        }

        // Sub-screen: Downloads Screen
        AnimatedVisibility(
            visible = isDownloadsOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            DownloadsScreen(
                player = player,
                repository = repository,
                onBack = { isDownloadsOpen = false }
            )
        }

        // Sub-screen: Liked Songs Screen
        AnimatedVisibility(
            visible = isLikedSongsOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            LikedSongsScreen(
                player = player,
                repository = repository,
                onBack = { isLikedSongsOpen = false }
            )
        }

        // Sub-screen: Generator Screen (LastWave Page 7)
        AnimatedVisibility(
            visible = isGeneratorOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            GeneratorScreen(
                player = player,
                repository = repository,
                onBack = { isGeneratorOpen = false }
            )
        }

        // Sub-screen: Artist Screen
        AnimatedVisibility(
            visible = selectedArtistForDetail != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            selectedArtistForDetail?.let { artist ->
                ArtistScreen(
                    artistName = artist,
                    player = player,
                    repository = repository,
                    onBack = { selectedArtistForDetail = null },
                    onSelectArtist = { selectedArtistForDetail = it }
                )
            }
        }

        // Sub-screen: Album / Movie Soundtrack Detail Screen
        AnimatedVisibility(
            visible = selectedAlbumForDetail != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            selectedAlbumForDetail?.let { album ->
                AlbumDetailScreen(
                    album = album,
                    player = player,
                    repository = repository,
                    onBack = { selectedAlbumForDetail = null }
                )
            }
        }
    }
}
