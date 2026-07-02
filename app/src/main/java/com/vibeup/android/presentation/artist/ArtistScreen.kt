package com.vibeup.android.presentation.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.domain.model.Artist
import com.vibeup.android.domain.model.Song
import com.vibeup.android.presentation.player.PlayerViewModel
import java.util.Locale

@Composable
fun ArtistScreen(
    navController: NavController,
    viewModel: ArtistViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is ArtistUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ArtistUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.loadArtist() }) {
                            Text("Retry", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            is ArtistUiState.Success -> {
                ArtistContent(
                    artist = state.artist,
                    onPlaySong = { song, queue ->
                        playerViewModel.playSong(song, queue)
                    }
                )
            }
        }

        // Back button overlays the hero image regardless of load state
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(16.dp)
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ArtistContent(
    artist: Artist,
    onPlaySong: (Song, List<Song>) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ── Hero banner ──
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AsyncImage(
                    model = artist.imageUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF0A0A1A).copy(alpha = 0.4f),
                                    Color(0xFF0A0A1A)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        artist.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    if (artist.followerCount.isNotBlank() && artist.followerCount != "0") {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${formatFollowerCount(artist.followerCount)} followers",
                            fontSize = 13.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                }
            }
        }

        // ── Play all button ──
        if (artist.songs.isNotEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Button(
                        onClick = { onPlaySong(artist.songs.first(), artist.songs) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    ),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── Bio ──
        if (artist.bio.isNotBlank() && artist.bio != "No biography available.") {
            item {
                var expanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        "About",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF3F4F6),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        artist.bio,
                        fontSize = 13.sp,
                        color = Color(0xFF9CA3AF),
                        lineHeight = 19.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            }
        }

        // ── Top songs ──
        if (artist.songs.isNotEmpty()) {
            item {
                Text(
                    "Top Songs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            itemsIndexed(artist.songs) { index, song ->
                ArtistSongRow(
                    song = song,
                    index = index + 1,
                    onClick = { onPlaySong(song, artist.songs) }
                )
            }
        }

        // ── More from this artist (recommendation row) ──────────────────────────
        if (artist.songs.size > 5) {
            item {
                Text(
                    "More from ${artist.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            item {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    // Show songs beyond the top 5 already listed above, as recommendations
                    items(artist.songs.drop(5).take(10)) { song ->
                        Column(
                            modifier = Modifier
                                .width(130.dp)
                                .clickable { onPlaySong(song, artist.songs) }
                        ) {
                            AsyncImage(
                                model = song.imageUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                song.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                song.album,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // ── Albums ──
        if (artist.albums.isNotEmpty()) {
            item {
                Text(
                    "Albums",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            item {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    items(artist.albums) { album ->
                        Column(modifier = Modifier.width(120.dp)) {
                            AsyncImage(
                                model = album.imageUrl,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                album.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (album.year.isNotBlank()) {
                                Text(album.year, fontSize = 11.sp, color = Color(0xFF6B7280))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ArtistSongRow(song: Song, index: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "$index",
            fontSize = 13.sp,
            color = Color(0xFF4B5563),
            modifier = Modifier.width(20.dp)
        )
        AsyncImage(
            model = song.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.album,
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Formats raw follower counts like "1234567" into "1.2M", "45.3K", etc. */
private fun formatFollowerCount(raw: String): String {
    val count = raw.toLongOrNull() ?: return raw
    return when {
        count >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}