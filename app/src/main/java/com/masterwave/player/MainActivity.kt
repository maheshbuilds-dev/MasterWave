package com.masterwave.player

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.playback.MasterWavePlaybackService
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.components.MiniPlayerDock
import com.masterwave.player.ui.screens.HomeScreen
import com.masterwave.player.ui.screens.LibraryScreen
import com.masterwave.player.ui.screens.LyricsScreen
import com.masterwave.player.ui.screens.NowPlayingScreen
import com.masterwave.player.ui.screens.QueueScreen
import com.masterwave.player.ui.screens.SearchScreen
import com.masterwave.player.ui.screens.SettingsScreen
import com.masterwave.player.ui.theme.AccentCrimson
import com.masterwave.player.ui.theme.CanvasObsidian
import com.masterwave.player.ui.theme.MasterWaveTheme
import com.masterwave.player.ui.theme.SurfaceElevated
import com.masterwave.player.ui.theme.TextPrimary
import com.masterwave.player.ui.theme.TextTertiary

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
    }

    if (playbackService == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasObsidian)
        )
        return
    }

    val player = playbackService.player
    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val isLoading by player.isLoading.collectAsState()
    val positionMs by player.positionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Search, 2: Library, 3: Settings
    var isNowPlayingExpanded by remember { mutableStateOf(false) }
    var isLyricsOpen by remember { mutableStateOf(false) }
    var isQueueOpen by remember { mutableStateOf(false) }

    // Handle back button for expanded views
    BackHandler(enabled = isLyricsOpen || isQueueOpen || isNowPlayingExpanded) {
        if (isLyricsOpen) {
            isLyricsOpen = false
        } else if (isQueueOpen) {
            isQueueOpen = false
        } else if (isNowPlayingExpanded) {
            isNowPlayingExpanded = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentCrimson,
                            selectedTextColor = AccentCrimson,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentCrimson,
                            selectedTextColor = AccentCrimson,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                        label = { Text("Library") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentCrimson,
                            selectedTextColor = AccentCrimson,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentCrimson,
                            selectedTextColor = AccentCrimson,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        player = player,
                        repository = repository,
                        onNavigateToSearch = { selectedTab = 1 },
                        onNavigateToLibrary = { selectedTab = 2 }
                    )
                    1 -> SearchScreen(
                        player = player,
                        repository = repository,
                        onBack = { selectedTab = 0 }
                    )
                    2 -> LibraryScreen(
                        player = player,
                        repository = repository
                    )
                    3 -> SettingsScreen()
                }

                // Docked MiniPlayer above BottomNavigation
                if (currentTrack != null) {
                    MiniPlayerDock(
                        track = currentTrack!!,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onExpand = { isNowPlayingExpanded = true },
                        onPlayPause = { player.togglePlayPause() },
                        onNext = { player.playNext() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
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
