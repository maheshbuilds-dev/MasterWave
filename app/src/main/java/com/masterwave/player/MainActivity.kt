package com.masterwave.player

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.masterwave.player.data.update.AppUpdateManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.masterwave.player.data.cloud.CloudAccountManager
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
import com.masterwave.player.ui.screens.AlbumDetailScreen
import com.masterwave.player.ui.screens.AppUpdateScreen
import com.masterwave.player.ui.screens.AppearanceScreen
import com.masterwave.player.ui.screens.ArtistScreen
import com.masterwave.player.ui.screens.AudioPlaybackScreen
import com.masterwave.player.ui.screens.BackupRestoreScreen
import com.masterwave.player.ui.screens.ConnectionDiagnosticsScreen
import com.masterwave.player.ui.screens.DownloadsScreen
import com.masterwave.player.ui.screens.HomeScreen
import com.masterwave.player.ui.screens.LikedSongsScreen
import com.masterwave.player.ui.screens.ListenTogetherScreen
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
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState

class MainActivity : ComponentActivity() {

    companion object {
        val pendingListenTogetherRoomCode = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    }

    private var playbackService: MasterWavePlaybackService? = null
    private val isServiceBoundState = mutableStateOf(false)

    private val closeForUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AppUpdateManager.ACTION_CLOSE_FOR_UPDATE) {
                finishAffinity()
            }
        }
    }

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

        try {
            ContextCompat.registerReceiver(
                this,
                closeForUpdateReceiver,
                IntentFilter(AppUpdateManager.ACTION_CLOSE_FOR_UPDATE),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {}

        // Enable Hardware Window-Level Blur on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes.blurBehindRadius = 40
        }

        // Apply High Refresh Rate (120Hz display target)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                @Suppress("DEPRECATION")
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else windowManager.defaultDisplay
                val maxMode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                if (maxMode != null && ThemeState.highRefreshRateEnabled.value) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            } catch (_: Exception) {}
        }

        val serviceIntent = Intent(this, MasterWavePlaybackService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        handleDeepLinks(intent)

        setContent {
            MasterWaveTheme {
                MasterWaveRoot(
                    playbackService = if (isServiceBoundState.value) playbackService else null
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinks(intent)
    }

    private fun handleDeepLinks(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "masterwave" && uri.host == "spotify-callback") {
            lifecycleScope.launch {
                val authManager = com.masterwave.player.data.spotify.SpotifyAuthManager.getInstance(this@MainActivity)
                val success = authManager.handleAuthCallback(uri)
                if (success) {
                    Toast.makeText(this@MainActivity, "Connected to Spotify!", Toast.LENGTH_SHORT).show()
                } else {
                    val err = authManager.authError.value
                    if (!err.isNullOrBlank()) {
                        Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                    }
                }
            }
            return
        }

        // Listen Together Room Deep Link Handling
        val code = com.masterwave.player.data.listentogether.QrCodeHelper.extractRoomCode(uri.toString())
        if (!code.isNullOrBlank()) {
            pendingListenTogetherRoomCode.value = code
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(closeForUpdateReceiver)
        } catch (_: Exception) {}
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
        CloudAccountManager.getInstance(context, repository).triggerAutoSync()
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

    // Dynamic Artwork Color Extraction - Isolated to Now Playing ambient backdrop & dynamic theme
    LaunchedEffect(currentTrack?.id, currentTrack?.artworkUrl) {
        if (currentTrack != null) {
            val palette = com.masterwave.player.ui.theme.ArtworkColorExtractor.extractArtworkPalette(context, currentTrack?.artworkUrl)
            com.masterwave.player.ui.theme.ThemeState.setArtworkPalette(palette)
        } else {
            com.masterwave.player.ui.theme.ThemeState.setArtworkPalette(null)
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
    var isListenTogetherOpen by remember { mutableStateOf(false) }
    val pendingDeepLinkCode by MainActivity.pendingListenTogetherRoomCode.collectAsState()
    LaunchedEffect(pendingDeepLinkCode) {
        if (!pendingDeepLinkCode.isNullOrBlank()) {
            isListenTogetherOpen = true
        }
    }
    var isDownloadsOpen by remember { mutableStateOf(false) }
    var isLikedSongsOpen by remember { mutableStateOf(false) }
    var selectedArtistForDetail by remember { mutableStateOf<String?>(null) }
    var selectedAlbumForDetail by remember { mutableStateOf<Album?>(null) }
    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }
    val homeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val liquidGlassEnabled by ThemeState.liquidGlassEnabled.collectAsState()
    val liquidState = rememberLiquidState()
    val useLiquidShader = liquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    // True while the visible song feed is moving: the bottom dock splits into
    // two compact, detached glass islands so it stops covering the list.
    var isDockSplit by remember { mutableStateOf(false) }
    var scrollToTopTrigger by remember { mutableStateOf(0L) }

    // Live In-App Update System
    val updateManager = remember {
        com.masterwave.player.data.update.AppUpdateManager.getInstance(
            context,
            com.masterwave.player.MasterWaveApp.instance.okHttpClient
        )
    }
    val availableUpdate by updateManager.availableUpdate.collectAsState()
    val isUpdateDismissed by updateManager.isDismissed.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateManager.checkForUpdate(silent = false)
    }

    LaunchedEffect(currentTab) {
        isDockSplit = false
    }

    // Back handling for nested views
    BackHandler(
        enabled = isNowPlayingExpanded || isLyricsOpen || isQueueOpen ||
                isSearchOpen || isSettingsOpen || isAppearanceOpen ||
                isAudioPlaybackOpen || isBackupRestoreOpen || isAppUpdateOpen || isSupportOpen ||
                isYouTubeSyncOpen || isSpotifySyncOpen ||
                isDiagnosticsOpen || isListenTogetherOpen || isDownloadsOpen ||
                isLikedSongsOpen || selectedArtistForDetail != null ||
                selectedAlbumForDetail != null ||
                selectedPlaylistForDetail != null ||
                currentTab != MainTab.HOME
    ) {
        when {
            selectedAlbumForDetail != null -> selectedAlbumForDetail = null
            selectedArtistForDetail != null -> selectedArtistForDetail = null
            isLikedSongsOpen -> isLikedSongsOpen = false
            isDownloadsOpen -> isDownloadsOpen = false
            isListenTogetherOpen -> isListenTogetherOpen = false
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

    val isOverlayOpen = isNowPlayingExpanded || isLyricsOpen || isQueueOpen
    val isSettingsRelatedOpen = isSettingsOpen || isAppearanceOpen || isAudioPlaybackOpen ||
            isBackupRestoreOpen || isAppUpdateOpen || isSupportOpen ||
            isDiagnosticsOpen || isListenTogetherOpen || isYouTubeSyncOpen || isSpotifySyncOpen
    val isSecondaryScreenOpen = isSettingsOpen || isSearchOpen || isBackupRestoreOpen ||
            isYouTubeSyncOpen || isSpotifySyncOpen || isAppearanceOpen ||
            isAudioPlaybackOpen || isAppUpdateOpen || isSupportOpen ||
            selectedPlaylistForDetail != null || isDiagnosticsOpen ||
            isListenTogetherOpen || isDownloadsOpen || isLikedSongsOpen ||
            selectedArtistForDetail != null || selectedAlbumForDetail != null

    val liquidHorizontalEnter = remember {
        slideInHorizontally(
            animationSpec = spring(
                dampingRatio = 0.84f,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetX = { it / 3 }
        ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + scaleIn(
            initialScale = 0.97f,
            animationSpec = spring(
                dampingRatio = 0.84f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    val liquidHorizontalExit = remember {
        slideOutHorizontally(
            animationSpec = spring(
                dampingRatio = 0.88f,
                stiffness = Spring.StiffnessMediumLow
            ),
            targetOffsetX = { it / 3 }
        ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + scaleOut(
            targetScale = 0.97f,
            animationSpec = spring(
                dampingRatio = 0.88f,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val liquidVerticalEnter = remember {
        slideInVertically(
            animationSpec = spring(
                dampingRatio = 0.84f,
                stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing))
    }
    val liquidVerticalExit = remember {
        slideOutVertically(
            animationSpec = spring(
                dampingRatio = 0.88f,
                stiffness = Spring.StiffnessMediumLow
            ),
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // The sampled content and the shader controls must remain sibling layers.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (useLiquidShader) Modifier.liquefiable(liquidState) else Modifier)
        ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(currentTab, isSecondaryScreenOpen, isOverlayOpen) {
                    if (isSecondaryScreenOpen || isOverlayOpen) return@pointerInput
                    val swipeThreshold = 85.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        var totalX = 0f
                        var totalY = 0f
                        var isHorizontal: Boolean? = null
                        var isCancelled = false

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            // If any child component (track item, button, slider, carousel) consumed the event, abort immediately!
                            if (change.isConsumed) {
                                isCancelled = true
                                break
                            }

                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            totalX += dx
                            totalY += dy

                            if (isHorizontal == null) {
                                val absX = kotlin.math.abs(totalX)
                                val absY = kotlin.math.abs(totalY)
                                if (absX > 36f || absY > 36f) {
                                    if (absX > absY * 2.2f) {
                                        isHorizontal = true
                                    } else {
                                        isCancelled = true
                                        break
                                    }
                                }
                            }
                        }

                        if (!isCancelled && isHorizontal == true && kotlin.math.abs(totalX) > swipeThreshold) {
                            val tabs = listOf(MainTab.HOME, MainTab.STATS, MainTab.LIBRARY, MainTab.SEARCH)
                            val currentIndex = tabs.indexOf(currentTab)
                            if (totalX < 0) {
                                // Swiped left: Advance to next tab (0 -> 1 -> 2 -> 3)
                                if (currentIndex in 0 until tabs.lastIndex) {
                                    currentTab = tabs[currentIndex + 1]
                                }
                            } else {
                                // Swiped right: Return to previous tab (3 -> 2 -> 1 -> 0)
                                if (currentIndex in 1..tabs.lastIndex) {
                                    currentTab = tabs[currentIndex - 1]
                                }
                            }
                        }
                    }
                }
        ) {
            // Animated Tab Switching: Full viewport size with liquid smooth spring physics & scale
            AnimatedContent(
                targetState = currentTab,
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart,
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    val slideDistance = 48
                    if (isForward) {
                        (slideInHorizontally(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                        ) { slideDistance } + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                        )).togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                            ) { -slideDistance } + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + scaleOut(
                                targetScale = 0.98f,
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                            )
                        )
                    } else {
                        (slideInHorizontally(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                        ) { -slideDistance } + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + scaleIn(
                            initialScale = 0.98f,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                        )).togetherWith(
                            slideOutHorizontally(
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                            ) { slideDistance } + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + scaleOut(
                                targetScale = 0.98f,
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                            )
                        )
                    }.using(SizeTransform(clip = false))
                },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    MainTab.HOME -> HomeScreen(
                        player = player,
                        repository = repository,
                        onNavigateToSearch = { isSearchOpen = true },
                        onNavigateToSettings = { isSettingsOpen = true },
                        onNavigateToDownloads = { isDownloadsOpen = true },
                        onNavigateToArtist = { selectedArtistForDetail = it },
                        onNavigateToLikedSongs = { isLikedSongsOpen = true },
                        onPlaylistSelected = { playlist -> selectedPlaylistForDetail = playlist },
                        liquidState = liquidState,
                        isGlassEnabled = liquidGlassEnabled,
                        onScrollStateChanged = { isDockSplit = it },
                        scrollToTopTrigger = scrollToTopTrigger,
                        homeListState = homeListState
                    )
                    MainTab.STATS -> StatsScreen(
                        player = player,
                        repository = repository,
                        onNavigateToSearch = { isSearchOpen = true },
                        onNavigateToSettings = { isSettingsOpen = true },
                        onScrollStateChanged = { isDockSplit = it },
                        scrollToTopTrigger = scrollToTopTrigger
                    )
                    MainTab.LIBRARY -> PlaylistsScreen(
                        player = player,
                        repository = repository,
                        onPlaylistSelected = { playlist ->
                            selectedPlaylistForDetail = playlist
                        },
                        onNavigateToLikedSongs = { isLikedSongsOpen = true },
                        onNavigateToDownloads = { isDownloadsOpen = true },
                        onNavigateToSearch = { isSearchOpen = true },
                        onNavigateToSettings = { isSettingsOpen = true },
                        onNavigateToYouTubeSync = { isYouTubeSyncOpen = true },
                        onNavigateToSpotifySync = { isSpotifySyncOpen = true },
                        onScrollStateChanged = { isDockSplit = it },
                        scrollToTopTrigger = scrollToTopTrigger
                    )
                    MainTab.SEARCH -> SearchScreen(
                        player = player,
                        repository = repository,
                        onBack = { currentTab = MainTab.HOME },
                        onArtistSelected = { selectedArtistForDetail = it },
                        onAlbumSelected = { selectedAlbumForDetail = it },
                        onExpandNowPlaying = { isNowPlayingExpanded = true }
                    )
                }
            }
        }

        // --- Fullscreen Search Overlay ---
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
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
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
        ) {
            SettingsScreen(
                onBack = { isSettingsOpen = false },
                onNavigateToAppearance = { isAppearanceOpen = true },
                onNavigateToAudioPlayback = { isAudioPlaybackOpen = true },
                onNavigateToAppUpdate = { isAppUpdateOpen = true },
                onNavigateToBackupRestore = { isBackupRestoreOpen = true },
                onNavigateToYouTubeSync = { isYouTubeSyncOpen = true },
                onNavigateToSpotifySync = { isSpotifySyncOpen = true },
                onNavigateToListenTogether = { isListenTogetherOpen = true },
                onNavigateToSupport = { isSupportOpen = true },
                player = player
            )
        }

        // Sub-screen: Backup & Restore (Launcher-grade Snapshot Import & Export)
        AnimatedVisibility(
            visible = isBackupRestoreOpen,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
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
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
        ) {
            YouTubeSyncScreen(
                player = player,
                repository = repository,
                onBack = { isYouTubeSyncOpen = false },
                onNavigateToSpotify = {
                    isYouTubeSyncOpen = false
                    isSpotifySyncOpen = true
                }
            )
        }

        // Sub-screen: Spotify & Sync
        AnimatedVisibility(
            visible = isSpotifySyncOpen,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
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
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
        ) {
            AppearanceScreen(onBack = { isAppearanceOpen = false })
        }

        // Sub-screen: Audio & Playback
        AnimatedVisibility(
            visible = isAudioPlaybackOpen,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
        ) {
            AudioPlaybackScreen(
                onBack = { isAudioPlaybackOpen = false },
                player = player
            )
        }

        // Sub-screen: App Update
        AnimatedVisibility(
            visible = isAppUpdateOpen,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
        ) {
            AppUpdateScreen(
                onBack = { isAppUpdateOpen = false },
                updateManager = updateManager
            )
        }

        // Sub-screen: Support & Community
        AnimatedVisibility(
            visible = isSupportOpen,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
        ) {
            SupportCommunityScreen(onBack = { isSupportOpen = false })
        }

        // Sub-screen: Playlist Detail
        AnimatedVisibility(
            visible = selectedPlaylistForDetail != null,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
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
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
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
            enter = liquidVerticalEnter,
            exit = liquidVerticalExit
        ) {
            NowPlayingScreen(
                player = player,
                repository = repository,
                onCollapse = { isNowPlayingExpanded = false },
                onOpenLyrics = { isLyricsOpen = true },
                onOpenQueue = { isQueueOpen = true },
                liquidState = liquidState,
                isGlassEnabled = liquidGlassEnabled
            )
        }

        // Fullscreen Lyrics Overlay
        AnimatedVisibility(
            visible = isLyricsOpen,
            enter = liquidVerticalEnter,
            exit = liquidVerticalExit
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
            enter = liquidVerticalEnter,
            exit = liquidVerticalExit
        ) {
            QueueScreen(
                player = player,
                repository = repository,
                onBack = { isQueueOpen = false }
            )
        }

        // Sub-screen: Listen Together (Real-time Mesh & Room Sync)
        AnimatedVisibility(
            visible = isListenTogetherOpen,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
        ) {
            ListenTogetherScreen(
                onBack = {
                    isListenTogetherOpen = false
                    MainActivity.pendingListenTogetherRoomCode.value = null
                },
                player = player,
                initialRoomCode = pendingDeepLinkCode,
                onConsumeInitialRoomCode = {
                    MainActivity.pendingListenTogetherRoomCode.value = null
                }
            )
        }

        // Sub-screen: Downloads Screen
        AnimatedVisibility(
            visible = isDownloadsOpen,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
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
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
        ) {
            LikedSongsScreen(
                player = player,
                repository = repository,
                onBack = { isLikedSongsOpen = false }
            )
        }

        // Sub-screen: Artist Screen
        AnimatedVisibility(
            visible = selectedArtistForDetail != null,
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
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
            enter = liquidHorizontalEnter,
            exit = liquidHorizontalExit
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

        // --- Unified Floating Bottom Layer (MiniPlayer & Dock) ---
        // Stays on top of all screens; hidden when full-screen NowPlaying/Lyrics/Queue is active
        if (!isOverlayOpen) {
            val isAmoledTheme = ThemeState.isAmoledTheme()
            // Subtle edge fade so the content extends cleanly beneath the glass dock
            // without washing out the backdrop with heavy opaque scrim.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.72f to Color.Transparent,
                                1.0f to if (isAmoledTheme) Color(0x40000000) else Color(0x18FFFFFF)
                            )
                        )
                    )
            )
        }
        }

        if (!isOverlayOpen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val dockGap by animateDpAsState(
                    targetValue = if (isDockSplit) 7.dp else 10.dp,
                    animationSpec = spring(
                        dampingRatio = 0.86f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "dock_split_gap"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dockGap)
                ) {
                    if (currentTrack != null && !isSettingsRelatedOpen) {
                        MiniPlayerDock(
                            track = currentTrack!!,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            positionMs = positionMs,
                            durationMs = durationMs,
                            onExpand = { isNowPlayingExpanded = true },
                            onPlayPause = { player.togglePlayPause() },
                            onNext = { player.skipToNext() },
                            liquidState = liquidState,
                            isGlassEnabled = liquidGlassEnabled,
                            isCompact = isDockSplit
                        )
                    }

                    if (!isSecondaryScreenOpen) {
                        FloatingNavigationDock(
                            currentTab = currentTab,
                            onTabSelected = { selected ->
                                if (selected == MainTab.SEARCH) {
                                    isSearchOpen = true
                                } else {
                                    currentTab = selected
                                }
                            },
                            onSearchClick = { isSearchOpen = true },
                            isSearchActive = isSearchOpen || currentTab == MainTab.SEARCH,
                            liquidState = liquidState,
                            isGlassEnabled = liquidGlassEnabled,
                            isMinimized = isDockSplit
                        )
                    }
                }
            }
        }

        // Live In-App Update Floating Notification Banner
        AnimatedVisibility(
            visible = availableUpdate != null && !isUpdateDismissed && !isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            availableUpdate?.let { update ->
                com.masterwave.player.ui.components.InAppUpdateBanner(
                    update = update,
                    onUpdateClick = { showUpdateDialog = true },
                    onDismiss = { updateManager.dismissUpdate() }
                )
            }
        }

        // Live In-App Update Modal Dialog
        if (showUpdateDialog && availableUpdate != null) {
            com.masterwave.player.ui.components.InAppUpdateModal(
                update = availableUpdate!!,
                updateManager = updateManager,
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}
