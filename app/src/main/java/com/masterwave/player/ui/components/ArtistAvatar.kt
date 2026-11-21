package com.masterwave.player.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.masterwave.player.data.recommendation.ArtistPhotoProvider
import com.masterwave.player.ui.theme.DisplayFontFamily
import java.util.Locale
import kotlin.math.abs

/**
 * MasterWave Authentic Artist Avatar:
 * Guarantees zero artificial AI portraits and zero random stock photos of strangers.
 * Loads verified official photography when available, with a graceful, clean
 * typographic monogram (initials) fallback matching Apple Music design standards.
 */
@Composable
fun ArtistAvatar(
    name: String,
    photoUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color(0x33FFFFFF),
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val catalogPhotoUrl = remember(name) { ArtistPhotoProvider.getPhotoUrlOrNull(name) }
    val explicitPhotoUrl = photoUrl?.takeIf(String::isNotBlank)
    var failedPhotoUrls by remember(name, explicitPhotoUrl) {
        mutableStateOf(emptySet<String>())
    }
    var fallbackPhotoUrl by remember(name, explicitPhotoUrl) { mutableStateOf<String?>(null) }
    var fallbackAttempted by remember(name, explicitPhotoUrl) { mutableStateOf(false) }
    val activePhotoUrl = explicitPhotoUrl?.takeUnless(failedPhotoUrls::contains)
        ?: catalogPhotoUrl?.takeUnless(failedPhotoUrls::contains)
        ?: fallbackPhotoUrl?.takeUnless(failedPhotoUrls::contains)

    LaunchedEffect(name, explicitPhotoUrl, activePhotoUrl) {
        if (activePhotoUrl == null && !fallbackAttempted) {
            fallbackAttempted = true
            fallbackPhotoUrl = ArtistPhotoProvider.resolvePhotoOnline(
                context = context,
                artistName = name,
                excludedUrl = failedPhotoUrls.lastOrNull() ?: explicitPhotoUrl
            )
        }
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(clickModifier)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (activePhotoUrl != null) {
            AsyncImage(
                model = activePhotoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                onError = { error ->
                    Log.w("ArtistAvatar", "Photo load failed for '$name'", error.result.throwable)
                    failedPhotoUrls = failedPhotoUrls + activePhotoUrl
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ArtistInitialsAvatar(name = name, size = size)
        }
    }
}

/**
 * Editorial Monogram / Initials Fallback Avatar:
 * Generates an elegant, high-contrast monogram circle using the artist's initials
 * over a deterministic, sophisticated obsidian-tinted gradient.
 */
@Composable
fun ArtistInitialsAvatar(
    name: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val initials = remember(name) { extractArtistInitials(name) }
    val gradient = remember(name) { getArtistDeterministicGradient(name) }
    val fontSize = (size.value * 0.38f).coerceAtLeast(10f).sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            letterSpacing = 0.5.sp,
            color = Color.White
        )
    }
}

private fun extractArtistInitials(name: String): String {
    val clean = name.trim()
        .replace(Regex("[^a-zA-Z0-9\\s]"), "")
        .trim()
    if (clean.isBlank()) return "MW"

    val parts = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size == 1 -> parts[0].take(2).uppercase(Locale.ROOT)
        parts.size >= 2 -> {
            val first = parts[0].first().uppercaseChar()
            val second = parts[1].first().uppercaseChar()
            "$first$second"
        }
        else -> "MW"
    }
}

private val CuratedArtistGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFF1E2235), Color(0xFF10121C))),
    Brush.linearGradient(listOf(Color(0xFF26192E), Color(0xFF140C19))),
    Brush.linearGradient(listOf(Color(0xFF172828), Color(0xFF0C1717))),
    Brush.linearGradient(listOf(Color(0xFF281F1A), Color(0xFF17100D))),
    Brush.linearGradient(listOf(Color(0xFF1C2538), Color(0xFF0E131E))),
    Brush.linearGradient(listOf(Color(0xFF2B1D24), Color(0xFF170E13))),
    Brush.linearGradient(listOf(Color(0xFF1E2922), Color(0xFF0E1611)))
)

private fun getArtistDeterministicGradient(name: String): Brush {
    val idx = abs(name.hashCode()) % CuratedArtistGradients.size
    return CuratedArtistGradients[idx]
}
