package com.masterwave.player.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.masterwave.player.data.listentogether.QrCodeHelper
import com.masterwave.player.data.listentogether.UserIdentityManager
import com.masterwave.player.data.webrtc.ListenTogetherManager
import com.masterwave.player.data.webrtc.RoomConnectionState
import com.masterwave.player.playback.MasterWavePlayer
import com.masterwave.player.ui.animation.applePressEffect
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.WebAccentEmerald
import com.masterwave.player.ui.theme.WebAccentPeach
import com.masterwave.player.ui.theme.WebAccentPink
import com.masterwave.player.ui.theme.WebAccentSky
import com.masterwave.player.ui.theme.rememberAppThemeColors
import kotlinx.coroutines.launch

/**
 * MasterWave Listen Together Screen:
 * Production-ready real-time group listening with 6-digit numeric room codes,
 * Google Code Scanner integration, QR code generation, strict capacity enforcement,
 * sub-second Media3 playback synchronization, and host authorization tokens.
 */
@Composable
fun ListenTogetherScreen(
    onBack: () -> Unit,
    player: MasterWavePlayer? = null,
    initialRoomCode: String? = null,
    onConsumeInitialRoomCode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = rememberAppThemeColors()
    val accentColor = WebAccentPink
    val inkSecondary = colors.body
    val identityManager = remember { UserIdentityManager.getInstance(context) }
    val isUserRegistered by identityManager.isRegistered.collectAsState()
    val userDisplayName by identityManager.displayName.collectAsState()

    val listenManager = remember(player) {
        player?.let { ListenTogetherManager.getInstance(context, it) }
    }

    val currentRoomCode by listenManager?.currentRoomCode?.collectAsState() ?: remember { mutableStateOf(null) }
    val roomName by listenManager?.roomName?.collectAsState() ?: remember { mutableStateOf("Listening Room") }
    val hostName by listenManager?.hostName?.collectAsState() ?: remember { mutableStateOf("Host") }
    val maxCapacity by listenManager?.maxCapacity?.collectAsState() ?: remember { mutableIntStateOf(5) }
    val isHost by listenManager?.isHost?.collectAsState() ?: remember { mutableStateOf(false) }
    val isJoined by listenManager?.isJoined?.collectAsState() ?: remember { mutableStateOf(false) }
    val connectionState by listenManager?.connectionState?.collectAsState() ?: remember { mutableStateOf(RoomConnectionState.DISCONNECTED) }
    val errorMessage by listenManager?.errorMessage?.collectAsState() ?: remember { mutableStateOf(null) }

    val currentLocalTrack by player?.currentTrack?.collectAsState() ?: remember { mutableStateOf(null) }
    val isLocalPlaying by player?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val hostTrack by listenManager?.hostTrack?.collectAsState() ?: remember { mutableStateOf(null) }
    val isHostPlaying by listenManager?.isHostPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val syncDriftMs by listenManager?.syncDriftMs?.collectAsState() ?: remember { mutableStateOf(0L) }
    val connectedPeers by listenManager?.connectedPeers?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val liveReactions by listenManager?.liveReactions?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    // UI States
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Join, 1: Create
    var roomCodeDigits by remember { mutableStateOf(initialRoomCode?.take(6) ?: "") }
    var createRoomNameInput by remember { mutableStateOf("") }
    var selectedCapacity by remember { mutableIntStateOf(5) }
    var customCapacityInput by remember { mutableStateOf("5") }
    var isCustomCapacitySelected by remember { mutableStateOf(false) }

    var showNameDialog by remember { mutableStateOf(!isUserRegistered) }
    var nameInput by remember { mutableStateOf(userDisplayName ?: "") }
    var showQrDialog by remember { mutableStateOf(false) }
    var showCloseConfirmDialog by remember { mutableStateOf(false) }

    val reactionEmojis = listOf("🔥", "❤️", "⚡", "🎵", "🚀", "💯")

    // Handle deep link / incoming initial room code
    LaunchedEffect(initialRoomCode, isUserRegistered) {
        if (!initialRoomCode.isNullOrBlank() && isUserRegistered && listenManager != null && !isJoined) {
            val code = QrCodeHelper.extractRoomCode(initialRoomCode)
            if (code != null) {
                roomCodeDigits = code
                listenManager.joinRoom(code) { success, _ ->
                    if (success) onConsumeInitialRoomCode()
                }
            }
        }
    }

    // Helper functions
    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareInvitation(code: String) {
        val inviteUrl = QrCodeHelper.buildInviteUrl(code)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "🎵 Join my Listen Together music room on MasterWave!\n\nRoom Code: $code\nTap to join: $inviteUrl"
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Listening Room")
        context.startActivity(shareIntent)
    }

    fun launchGoogleQrScanner() {
        try {
            val scanner = GmsBarcodeScanning.getClient(context)
            scanner.startScan()
                .addOnSuccessListener { barcode: Barcode ->
                    val raw = barcode.rawValue
                    val extracted = QrCodeHelper.extractRoomCode(raw)
                    if (extracted != null && extracted.length == 6) {
                        roomCodeDigits = extracted
                        listenManager?.joinRoom(extracted) { success, _ ->
                            if (!success) {
                                Toast.makeText(context, "Could not join room $extracted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "No valid MasterWave 6-digit room code found in QR.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Toast.makeText(context, "Scanner unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open QR scanner: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Onboarding / Display Name Dialog ---
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = {
                if (isUserRegistered) showNameDialog = false
            },
            containerColor = colors.surfaceSoft,
            titleContentColor = colors.ink,
            textContentColor = inkSecondary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = if (isUserRegistered) "Edit Display Name" else "Welcome to Listen Together",
                        fontFamily = DisplayFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter a display name so friends can recognize you in listening rooms. Zero passwords or account required.",
                        fontSize = 13.sp,
                        color = inkSecondary,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(25) },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.ink,
                            unfocusedTextColor = colors.ink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = nameInput.trim()
                        if (trimmed.length >= 2) {
                            identityManager.setDisplayName(trimmed)
                            showNameDialog = false
                            // If user had clicked an invite link before registering
                            if (!roomCodeDigits.isNullOrBlank() && roomCodeDigits.length == 6) {
                                listenManager?.joinRoom(roomCodeDigits) { success, _ ->
                                    if (success) onConsumeInitialRoomCode()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter at least 2 characters.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Save & Continue", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = if (isUserRegistered) {
                {
                    TextButton(onClick = { showNameDialog = false }) {
                        Text("Cancel", color = inkSecondary)
                    }
                }
            } else null
        )
    }

    // --- QR Code Modal Dialog ---
    if (showQrDialog && !currentRoomCode.isNullOrBlank()) {
        val inviteUrl = QrCodeHelper.buildInviteUrl(currentRoomCode!!)
        val qrBitmap = remember(currentRoomCode) {
            try {
                QrCodeHelper.generateQrBitmap(inviteUrl, sizePx = 600, darkColor = android.graphics.Color.WHITE)
            } catch (_: Exception) {
                null
            }
        }

        Dialog(onDismissRequest = { showQrDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.surfaceSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Scan to Join Party",
                        fontFamily = DisplayFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Room QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(color = accentColor)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ROOM CODE", fontSize = 11.sp, color = inkSecondary, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = currentRoomCode!!.chunked(3).joinToString(" "),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            letterSpacing = 3.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { shareInvitation(currentRoomCode!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Share Link", color = Color.White)
                        }
                        Button(
                            onClick = { showQrDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.border.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", color = colors.ink)
                        }
                    }
                }
            }
        }
    }

    // --- Host Close Room Confirmation ---
    if (showCloseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCloseConfirmDialog = false },
            containerColor = colors.surfaceSoft,
            title = { Text("Close Listening Room?", color = colors.ink, fontWeight = FontWeight.Bold) },
            text = { Text("This will end the synchronized party and disconnect all participants.", color = inkSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showCloseConfirmDialog = false
                        listenManager?.closeRoom()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Close Room", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirmDialog = false }) {
                    Text("Cancel", color = inkSecondary)
                }
            }
        )
    }

    // --- Main Screen Layout ---
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, colors.border, CircleShape)
                        .background(colors.surfaceSoft, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Listen Together",
                        fontFamily = DisplayFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    Text(
                        text = if (isJoined) "Live Session: $roomName" else "Real-Time Group Audio Sync",
                        fontSize = 12.sp,
                        color = inkSecondary
                    )
                }

                // User Profile Chip (Tap to edit)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .applePressEffect()
                        .clickable { showNameDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(accentColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (userDisplayName ?: "U").take(1).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Text(
                            text = userDisplayName ?: "Set Name",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit name",
                            modifier = Modifier.size(12.dp),
                            tint = inkSecondary
                        )
                    }
                }
            }
        }

        // Error Banner
        if (!errorMessage.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFE53935).copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935))
                        Text(
                            text = errorMessage!!,
                            color = colors.ink,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { listenManager?.clearError() }, modifier = Modifier.size(24.dp)) {
                            Text("✕", color = inkSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // If NOT in a room -> Show Join/Create Tabs
        if (!isJoined) {
            item {
                // Segmented Tab Selector
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selectedTab == 0) accentColor else Color.Transparent)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Join a Room",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) Color.White else inkSecondary,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selectedTab == 1) accentColor else Color.Transparent)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Create a Room",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) Color.White else inkSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Tab 0: Join Room Content
            if (selectedTab == 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surfaceSoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enter 6-Digit Room Code",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = accentColor.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .applePressEffect()
                                        .clickable { launchGoogleQrScanner() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                        Text("Scan QR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                    }
                                }
                            }

                            // 6 Distinct PIN Digits Input Box
                            SixDigitPinInput(
                                value = roomCodeDigits,
                                onValueChange = { digits ->
                                    roomCodeDigits = digits
                                    if (digits.length == 6) {
                                        listenManager?.joinRoom(digits) { _, _ -> }
                                    }
                                },
                                borderColor = colors.border,
                                activeBorderColor = accentColor,
                                textColor = colors.ink
                            )

                            Button(
                                onClick = {
                                    if (roomCodeDigits.length == 6) {
                                        listenManager?.joinRoom(roomCodeDigits) { _, _ -> }
                                    } else {
                                        Toast.makeText(context, "Please enter all 6 digits.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = roomCodeDigits.length == 6 && connectionState != RoomConnectionState.CONNECTING,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    disabledContainerColor = colors.border
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                if (connectionState == RoomConnectionState.CONNECTING) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Connecting to Room...", color = Color.White, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Join Listening Room", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Create Room Content
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.surfaceSoft,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Room Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink)

                            OutlinedTextField(
                                value = createRoomNameInput,
                                onValueChange = { createRoomNameInput = it },
                                label = { Text("Room Name (Optional)") },
                                placeholder = { Text("${userDisplayName ?: "My"}'s Music Room") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = colors.border,
                                    focusedTextColor = colors.ink,
                                    unfocusedTextColor = colors.ink
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Participant Capacity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.ink)
                                    Text(
                                        "Max: ${if (isCustomCapacitySelected) customCapacityInput else selectedCapacity} Seats (Host + Guests)",
                                        fontSize = 12.sp,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(2, 5, 10, 20).forEach { cap ->
                                        FilterChip(
                                            selected = !isCustomCapacitySelected && selectedCapacity == cap,
                                            onClick = {
                                                isCustomCapacitySelected = false
                                                selectedCapacity = cap
                                            },
                                            label = { Text("$cap Seats") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = accentColor,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val finalCap = if (isCustomCapacitySelected) {
                                        customCapacityInput.toIntOrNull() ?: 5
                                    } else {
                                        selectedCapacity
                                    }
                                    listenManager?.createRoom(
                                        customRoomName = createRoomNameInput,
                                        capacity = finalCap
                                    ) { success, code ->
                                        if (success && code != null) {
                                            Toast.makeText(context, "Room $code created!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = connectionState != RoomConnectionState.CONNECTING,
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                if (connectionState == RoomConnectionState.CONNECTING) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generating Room...", color = Color.White, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Create Room", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // --- ACTIVE LISTENING ROOM DASHBOARD ---
            // ==========================================
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = colors.surfaceSoft,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Room Code & Share Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(WebAccentEmerald, CircleShape)
                                    )
                                    Text("ACTIVE ROOM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WebAccentEmerald, letterSpacing = 1.sp)
                                }
                                Text(
                                    text = (currentRoomCode ?: "------").chunked(3).joinToString(" "),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.ink,
                                    letterSpacing = 2.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { currentRoomCode?.let { copyToClipboard(it, "Room Code") } },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(colors.canvas, CircleShape)
                                        .border(1.dp, colors.border, CircleShape)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = colors.ink, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { currentRoomCode?.let { shareInvitation(it) } },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(accentColor, CircleShape)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { showQrDialog = true },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(colors.canvas, CircleShape)
                                        .border(1.dp, colors.border, CircleShape)
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = "Show QR", tint = colors.ink, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Room Info Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.canvas.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Host: $hostName ${if (isHost) "(You)" else ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.ink
                            )

                            Text(
                                text = "Capacity: ${connectedPeers.size} / $maxCapacity Seats",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (connectedPeers.size >= maxCapacity) Color(0xFFE53935) else accentColor
                            )
                        }
                    }
                }
            }

            // Live Synchronized Playback Card
            item {
                val activeTrack = if (isHost) currentLocalTrack else hostTrack
                val isCurrentlyPlaying = if (isHost) isLocalPlaying else isHostPlaying

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surfaceSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Artwork
                            AsyncImage(
                                model = activeTrack?.artworkUrl,
                                contentDescription = "Artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.canvas)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeTrack?.title ?: "Waiting for Host to play music...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeTrack?.artist ?: "MasterWave Stream",
                                    fontSize = 13.sp,
                                    color = inkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(if (isCurrentlyPlaying) WebAccentEmerald else inkSecondary, CircleShape)
                                    )
                                    Text(
                                        text = if (isCurrentlyPlaying) "Synchronized • Sub-second Latency" else "Playback Paused",
                                        fontSize = 11.sp,
                                        color = if (isCurrentlyPlaying) WebAccentEmerald else inkSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Host Playback Controls
                            if (isHost && player != null) {
                                IconButton(
                                    onClick = {
                                        if (isLocalPlaying) player.pause() else player.resume()
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(accentColor, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isLocalPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-Time Reactions Bar
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Send Live Reaction", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = inkSecondary)

                        LazyRow(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(reactionEmojis) { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .applePressEffect()
                                        .clip(CircleShape)
                                        .background(colors.canvas)
                                        .clickable {
                                            listenManager?.sendReaction(emoji)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                }
                            }
                        }

                        // Recent live reactions ticker
                        if (liveReactions.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                liveReactions.takeLast(3).forEach { rx ->
                                    Text(
                                        text = "${rx.senderName}: ${rx.emoji}  ",
                                        fontSize = 12.sp,
                                        color = inkSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Participants List
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surfaceSoft,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Participants", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.ink)
                            Text("${connectedPeers.size} / $maxCapacity", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }

                        connectedPeers.forEach { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (peer.isHost) accentColor else colors.canvas, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = peer.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (peer.isHost) Color.White else colors.ink
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = peer.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = colors.ink
                                    )
                                }

                                if (peer.isHost) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = accentColor.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "HOST",
                                            color = accentColor,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Actions (Close or Leave Room)
            item {
                if (isHost) {
                    Button(
                        onClick = { showCloseConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("End Session (Close Room)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { listenManager?.leaveRoom() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceSoft),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = colors.ink)
                        Spacer(Modifier.width(8.dp))
                        Text("Leave Room", fontWeight = FontWeight.Bold, color = colors.ink)
                    }
                }
            }
        }
    }
}

/**
 * Clean separated 6-digit numeric input with auto-advance and paste handling.
 */
@Composable
fun SixDigitPinInput(
    value: String,
    onValueChange: (String) -> Unit,
    borderColor: Color,
    activeBorderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
        contentAlignment = Alignment.Center
    ) {
        // Invisible input field that catches all typing and pasting
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                val digitsOnly = raw.filter { it.isDigit() }.take(6)
                onValueChange(digitsOnly)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
        )

        // 6 Visual Digit Slots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 6) {
                val char = value.getOrNull(i)?.toString() ?: ""
                val isFocused = value.length == i || (value.length == 6 && i == 5)

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) activeBorderColor else borderColor,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}
