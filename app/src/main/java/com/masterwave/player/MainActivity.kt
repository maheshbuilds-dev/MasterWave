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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.masterwave.player.data.model.Playlist
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlaybackService
import com.masterwave.player.ui.components.FloatingNavigationDock
import com.masterwave.player.ui.components.MainTab
import com.masterwave.player.ui.components.MiniPlayerDock
import com.masterwave.player.ui.screens.AppUpdateScreen
import com.masterwave.player.ui.screens.AppearanceScreen
import com.masterwave.player.ui.screens.AudioPlaybackScreen
import com.masterwave.player.ui.screens.HomeScreen
import com.masterwave.player.ui.screens.LyricsScreen
import com.masterwave.player.ui.screens.NowPlayingScreen
import com.masterwave.player.ui.screens.PlaylistsScreen
import com.masterwave.player.ui.screens.QueueScreen
import com.masterwave.player.ui.screens.SearchScreen
import com.masterwave.player.ui.screens.SettingsScreen
import com.masterwave.player.ui.screens.StatsScreen
import com.masterwave.player.ui.screens.SupportCommunityScreen
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
    val repository = remember { MusicRepository.getInstance(MasterWaveApp.instance) }

    // Request permissions on startup
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
        repository.loadLocalTracks()
    }

    if (playbackService == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBackground)
        )
        return
    }

    val player = playbackService.player
    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val isLoading by player.isLoading.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()

    // Navigation States
    var currentTab by remember { mutableStateOf(MainTab.FEED) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var isLyricsOpen by remember { mutableStateOf(false) }
    var isQueueOpen by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isAppearanceOpen by remember { mutableStateOf(false) }
    var isAudioPlaybackOpen by remember { mutableStateOf(false) }
    var isAppUpdateOpen by remember { mutableStateOf(false) }
    var isSupportOpen by remember { mutableStateOf(false) }

    // Back handling for nested views
    BackHandler(
        enabled = isNowPlayingExpanded || isLyricsOpen || isQueueOpen ||
                isSearchOpen || isSettingsOpen || isAppearanceOpen ||
                isAudioPlaybackOpen || isAppUpdateOpen || isSupportOpen
    ) {
        when {
            isLyricsOpen -> isLyricsOpen = false
            isQueueOpen -> isQueueOpen = false
            isNowPlayingExpanded -> isNowPlayingExpanded = false
            isAppearanceOpen -> isAppearanceOpen = false
            isAudioPlaybackOpen -> isAudioPlaybackOpen = false
            isAppUpdateOpen -> isAppUpdateOpen = false
            isSupportOpen -> isSupportOpen = false
            isSettingsOpen -> isSettingsOpen = false
            isSearchOpen -> isSearchOpen = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                // Dock stack: MiniPlayer immediately above FloatingNavigationDock
                Column(
                    modifier = Modifier.navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
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
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                        MainTab.FEED -> HomeScreen(
                            player = player,
                            repository = repository,
                            onNavigateToSearch = { isSearchOpen = true },
                            onNavigateToSettings = { isSettingsOpen = true }
                        )
                        MainTab.STATS -> StatsScreen(
                            player = player,
                            repository = repository,
                            onNavigateToSearch = { isSearchOpen = true },
                            onNavigateToSettings = { isSettingsOpen = true }
                        )
                        MainTab.PLAYLISTS -> PlaylistsScreen(
                            player = player,
                            repository = repository,
                            onPlaylistSelected = { playlist ->
                                if (playlist.tracks.isNotEmpty()) {
                                    player.playTrack(playlist.tracks.first(), playlist.tracks)
                                }
                            }
                        )
                    }
                }
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
                onBack = { isSearchOpen = false }
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
                onNavigateToSupport = { isSupportOpen = true }
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
                onBack = { isQueueOpen = false }
            )
        }
    }
}
