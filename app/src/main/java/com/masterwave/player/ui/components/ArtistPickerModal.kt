package com.masterwave.player.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.data.model.Artist
import com.masterwave.player.data.recommendation.ArtistPhotoProvider
import com.masterwave.player.data.repository.MusicRepository
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.ThemeState
import kotlinx.coroutines.launch

/**
 * Interactive Favorite Artists Manager Sheet:
 * Allows user to view all followed artists, search new artists,
 * and follow/unfollow with instant real-time persistence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPickerSheet(
    repository: MusicRepository,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val followedArtists by repository.followedArtists.collectAsState()
    val accentColor by ThemeState.accentColor.collectAsState()
    val isAmoled = ThemeState.isAmoledTheme()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val bgColor = if (isAmoled) Color(0xFF0C0D14) else Color(0xFFF7F5F8)
    val cardBg = if (isAmoled) Color(0xFF161824) else Color(0xFFFFFFFF)
    val textPrimary = if (isAmoled) Color.White else Color(0xFF14171E)
    val textSecondary = if (isAmoled) Color(0xFFA0A5B8) else Color(0xFF6B7285)

    // Curated suggested artists pool
    val suggestedCatalog = remember {
        listOf(
            "Arijit Singh", "Anirudh Ravichander", "Sai Abhyankkar", "Diljit Dosanjh",
            "A.R. Rahman", "The Weeknd", "Sid Sriram", "Shreya Ghoshal", "Dua Lipa",
            "Karan Aujla", "Pritam", "Taylor Swift", "Badshah", "KR\$NA", "DIVINE",
            "Billie Eilish", "Sidhu Moose Wala", "AP Dhillon", "Yuvan Shankar Raja",
            "Harris Jayaraj", "Ed Sheeran", "Drake", "Post Malone", "Atif Aslam"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgColor,
        tonalElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manage Favorite Artists",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textPrimary
                    )
                    Text(
                        text = "${followedArtists.size} artists followed",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }

                IconButton(onClick = {
                    scope.launch { sheetState.hide() }
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    if (q.isNotBlank()) {
                        isSearching = true
                        scope.launch {
                            try {
                                val res = repository.search(q)
                                val distinctArtists = res.songs.map { it.artist }
                                    .distinct()
                                    .filter { it.isNotBlank() && it != "Unknown" }
                                    .take(8)
                                    .map { name ->
                                        Artist(
                                            id = name,
                                            name = name,
                                            thumbnailUrl = ArtistPhotoProvider.getPhotoUrlOrNull(name)
                                        )
                                    }
                                searchResults = distinctArtists
                            } catch (_: Exception) {
                                searchResults = emptyList()
                            } finally {
                                isSearching = false
                            }
                        }
                    } else {
                        searchResults = emptyList()
                    }
                },
                placeholder = {
                    Text(
                        text = "Search artist to follow...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
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
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = if (isAmoled) Color(0x30FFFFFF) else Color(0x20000000),
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Search Results if active
                if (searchQuery.isNotBlank()) {
                    item {
                        Text(
                            text = "Search Results",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = accentColor
                        )
                    }

                    if (searchResults.isEmpty() && !isSearching) {
                        item {
                            Text(
                                text = "No matching artists found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )
                        }
                    }

                    items(searchResults) { artist ->
                        val isFollowing = followedArtists.any { it.name.equals(artist.name, ignoreCase = true) }
                        ArtistListItemRow(
                            artist = artist,
                            isFollowing = isFollowing,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentColor = accentColor,
                            onToggleFollow = {
                                repository.toggleFollowArtist(artist)
                            }
                        )
                    }
                } else {
                    // 1. Followed Artists Section
                    item {
                        Text(
                            text = "Your Followed Artists",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = textPrimary
                        )
                    }

                    if (followedArtists.isEmpty()) {
                        item {
                            Text(
                                text = "No followed artists yet. Search or select from below to personalize your recommendations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondary
                            )
                        }
                    } else {
                        items(followedArtists) { artist ->
                            ArtistListItemRow(
                                artist = artist,
                                isFollowing = true,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                accentColor = accentColor,
                                onToggleFollow = {
                                    repository.unfollowArtist(artist.name)
                                }
                            )
                        }
                    }

                    // 2. Discover & Follow Suggestions
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Discover More Artists",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = textPrimary
                        )
                    }

                    val unFollowedSuggestions = suggestedCatalog.filter { name ->
                        followedArtists.none { it.name.equals(name, ignoreCase = true) }
                    }

                    items(unFollowedSuggestions) { name ->
                        val artist = Artist(
                            id = name,
                            name = name,
                            thumbnailUrl = ArtistPhotoProvider.getPhotoUrlOrNull(name)
                        )
                        ArtistListItemRow(
                            artist = artist,
                            isFollowing = false,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentColor = accentColor,
                            onToggleFollow = {
                                repository.followArtist(artist)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistListItemRow(
    artist: Artist,
    isFollowing: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color,
    accentColor: Color,
    onToggleFollow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtistAvatar(
                name = artist.name,
                photoUrl = artist.thumbnailUrl ?: ArtistPhotoProvider.getPhotoUrlOrNull(artist.name),
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isFollowing) "In Your Favorites" else "Suggested Artist",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = if (isFollowing) accentColor else textSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = if (isFollowing) accentColor.copy(alpha = 0.2f) else accentColor,
                border = if (isFollowing) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else null,
                modifier = Modifier.clickable(onClick = onToggleFollow)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null,
                        tint = if (isFollowing) accentColor else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = if (isFollowing) accentColor else Color.White
                    )
                }
            }
        }
    }
}
