package com.vibeup.android.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.domain.model.Song
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.ui.theme.DarkBackground
import com.vibeup.android.ui.theme.DarkCard
import com.vibeup.android.ui.theme.TextSecondary
import com.vibeup.android.ui.theme.VibeUpGreen

@Composable
fun PlaylistDetailScreen(
    navController: NavController,
    playlistId: String,
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    val playlist = playlists.find { it.id == playlistId }
    val playlistSongs by viewModel.getPlaylistSongs(playlistId)
        .collectAsState(initial = emptyList())

    var isShuffled by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), DarkBackground)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist?.name ?: "Playlist",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (playlist?.description?.isNotEmpty() == true) {
                            Text(
                                text = playlist.description,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "${playlistSongs.size} songs",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Playlist Cover — show first 4 song images
            item {
                if (playlistSongs.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        if (playlistSongs.size >= 4) {
                            // 2x2 grid of album arts
                            Row(modifier = Modifier.fillMaxSize()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    AsyncImage(
                                        model = playlistSongs[0].imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                    AsyncImage(
                                        model = playlistSongs[1].imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    AsyncImage(
                                        model = playlistSongs[2].imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                    AsyncImage(
                                        model = playlistSongs[3].imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            // Single image
                            AsyncImage(
                                model = playlistSongs[0].imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Play All + Shuffle buttons
            if (playlistSongs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Play All Button
                        Button(
                            onClick = {
                                playerViewModel.playSong(
                                    playlistSongs.first(),
                                    playlistSongs
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibeUpGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "▶ Play All",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        // Shuffle Button
                        Button(
                            onClick = {
                                isShuffled = !isShuffled
                                val shuffledList = playlistSongs.shuffled()
                                playerViewModel.playSong(
                                    shuffledList.first(),
                                    shuffledList
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isShuffled)
                                    VibeUpGreen
                                else
                                    DarkCard,
                                contentColor = if (isShuffled)
                                    Color.Black
                                else
                                    Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Shuffle",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Songs List
            if (playlistSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🎵", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No songs in this playlist yet!",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Long press any song to add",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(playlistSongs) { song ->
                    PlaylistSongItem(
                        song = song,
                        onClick = {
                            playerViewModel.playSong(song, playlistSongs)
                        },
                        onRemove = {
                            viewModel.removeSongFromPlaylist(
                                playlistId,
                                song.id
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PlaylistSongItem(
    song: Song,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Song Image
            AsyncImage(
                model = song.imageUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = song.artist,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = song.album,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = TextSecondary
                )
            }
        }
    }
}
/*```

---

## What's Fixed & Added
```
✅ Song album art showing in playlist
✅ Playlist cover — 2x2 grid of album arts
(or single image if less than 4 songs)
✅ Shuffle button — shuffles and plays
✅ Shuffle button turns green when active
✅ Song title + artist + album in list
✅ Better overall UI*/