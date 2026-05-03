package com.vibeup.android.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibeup.android.Screen
import com.vibeup.android.domain.model.Song
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.ui.theme.*

@Composable
fun PlaylistDetailScreen(
    navController: NavController,
    playlistId: String,
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()
    val playlist = playlists.find { it.id == playlistId }
    val playlistSongs by viewModel.getPlaylistSongs(playlistId)
        .collectAsState(initial = emptyList())
    val sortOrder by viewModel.sortOrder.collectAsState()

    val sortedSongs = remember(playlistSongs, sortOrder) {
        when (sortOrder) {
            SortOrder.DATE_ADDED -> playlistSongs
            SortOrder.ALPHABETICAL -> playlistSongs.sortedBy { it.title }
            SortOrder.ARTIST -> playlistSongs.sortedBy { it.artist }
            SortOrder.DURATION -> playlistSongs.sortedByDescending { it.duration }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredSongs = remember(sortedSongs, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedSongs
        } else {
            sortedSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(playlist?.name ?: "") }
    var isShuffled by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    "Rename Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = Color(0xFF2A2A4A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = PurplePrimary,
                        focusedContainerColor = Color(0xFF12122A),
                        unfocusedContainerColor = Color(0xFF12122A)
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renamePlaylist(playlistId, newName)
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color(0xFF12122A),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 100.dp
            )
        ) {
            // ── Header ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Color(0xFF12122A),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist?.name ?: "Playlist",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${playlistSongs.size} songs",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Color(0xFF12122A),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF1C1C3A))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "✏️ Rename Playlist",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    newName = playlist?.name ?: ""
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "🗑️ Delete Playlist",
                                        color = Color(0xFFEF4444),
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.deletePlaylist(playlistId)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }

            // ── Cover Art ──
            item {
                if (playlistSongs.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        if (playlistSongs.size >= 4) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(playlistSongs[0].imageUrl)
                                            .crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(playlistSongs[1].imageUrl)
                                            .crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(playlistSongs[2].imageUrl)
                                            .crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(playlistSongs[3].imageUrl)
                                            .crossfade(true).build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(playlistSongs[0].imageUrl)
                                    .crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xBB0A0A1A)
                                        ),
                                        startY = 80f
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Action Buttons ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Play All
                    Button(
                        onClick = {
                            if (filteredSongs.isNotEmpty()) {
                                playerViewModel.playSong(
                                    filteredSongs.first(),
                                    filteredSongs
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(PurplePrimary, BluePrimary)
                                    ),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Play All",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Shuffle
                    Button(
                        onClick = {
                            isShuffled = !isShuffled
                            if (filteredSongs.isNotEmpty()) {
                                val shuffled = filteredSongs.shuffled()
                                playerViewModel.playSong(
                                    shuffled.first(),
                                    shuffled
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isShuffled)
                                PurplePrimary.copy(alpha = 0.3f)
                            else
                                Color(0xFF12122A)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = if (isShuffled) PurplePrimary
                                else Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Shuffle",
                                color = if (isShuffled) PurplePrimary
                                else Color(0xFF9CA3AF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Add Songs
                    Button(
                        onClick = {
                            navController.navigate(
                                "${Screen.AddSongs.route}/$playlistId"
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Add",
                                color = PurplePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ── Search Bar ──
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    placeholder = {
                        Text(
                            "Search in playlist...",
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color(0xFF12122A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = PurplePrimary,
                        focusedContainerColor = Color(0xFF12122A),
                        unfocusedContainerColor = Color(0xFF12122A)
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
            }

            // ── Sort Chips ──
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    items(SortOrder.entries) { order ->
                        val label = when (order) {
                            SortOrder.DATE_ADDED -> "Date Added"
                            SortOrder.ALPHABETICAL -> "A → Z"
                            SortOrder.ARTIST -> "Artist"
                            SortOrder.DURATION -> "Duration"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (sortOrder == order)
                                        Brush.linearGradient(
                                            colors = listOf(
                                                PurplePrimary.copy(alpha = 0.4f),
                                                BluePrimary.copy(alpha = 0.4f)
                                            )
                                        )
                                    else
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF12122A),
                                                Color(0xFF12122A)
                                            )
                                        )
                                )
                                .clickable { viewModel.setSortOrder(order) }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                )
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (sortOrder == order)
                                    Color(0xFFE5E7EB)
                                else
                                    Color(0xFF4B5563)
                            )
                        }
                    }
                }
            }

            // ── Songs ──
            if (filteredSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF12122A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (searchQuery.isEmpty()) "🎵" else "🔍", fontSize = 36.sp)
                            Text(
                                if (searchQuery.isEmpty()) "No songs yet!" else "No results found!",
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Tap Add to add songs",
                                    color = Color(0xFF4B5563),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredSongs,
                    key = { it.id }
                ) { song ->
                    PlaylistSongItem(
                        song = song,
                        index = sortedSongs.indexOf(song) + 1,
                        context = context,
                        onClick = {
                            playerViewModel.playSong(song, filteredSongs)
                        },
                        onLike = {
                            viewModel.likeSong(song)
                        },
                        onRemove = {
                            viewModel.removeSongFromPlaylist(
                                playlistId, song.id
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

// ── Playlist Song Item ──
@Composable
fun PlaylistSongItem(
    song: Song,
    index: Int,
    context: android.content.Context,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0D2B))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Index number
        Text(
            text = "$index",
            fontSize = 12.sp,
            color = Color(0xFF4B5563),
            modifier = Modifier.width(20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // Album Art
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.imageUrl)
                .crossfade(true)
                .memoryCacheKey(song.id)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF3F4F6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${formatDuration(song.duration)}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1C1C3A))
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "▶️ Play",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showMenu = false; onClick() }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "💚 Like Song",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showMenu = false; onLike() }
                )
                HorizontalDivider(
                    color = Color(0xFF2A2A4A),
                    thickness = 0.5.dp
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "🗑️ Remove from Playlist",
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showMenu = false; onRemove() }
                )
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}