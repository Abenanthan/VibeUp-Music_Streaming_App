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

@Composable
fun PlaylistDetailScreen(
    navController: NavController,
    playlistId: String,
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    downloadsViewModel: DownloadsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()
    val playlist = playlists.find { it.id == playlistId }
    val playlistSongs by viewModel.getPlaylistSongs(playlistId)
        .collectAsState(initial = emptyList())
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isShuffleEnabled by playerViewModel.isShuffleEnabled.collectAsState()
    val isSmartShuffle by playerViewModel.isSmartShuffle.collectAsState()
    val currentQueueId by playerViewModel.currentQueueId.collectAsState()

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
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                                MaterialTheme.colorScheme.surface,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface,
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
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ Spotify-style Circular Play Button (Now on the Left)
                    Surface(
                        onClick = {
                            if (filteredSongs.isNotEmpty()) {
                                val isThisPlaylistPlaying = currentQueueId == playlistId

                                if (isThisPlaylistPlaying) {
                                    playerViewModel.togglePlayPause()
                                } else {
                                    // Start playing based on user preference
                                    val startSong = when {
                                        isSmartShuffle || isShuffleEnabled -> filteredSongs.random()
                                        else -> filteredSongs.first()
                                    }
                                    playerViewModel.playSong(startSong, filteredSongs, playlistId)
                                }
                            }
                        },
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val isPlaying by playerViewModel.isPlaying.collectAsState()
                            val isThisPlaylistPlaying = currentQueueId == playlistId

                            Icon(
                                imageVector = if (isPlaying && isThisPlaylistPlaying) 
                                    Icons.Default.Pause 
                                else 
                                    Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Shuffle Mode Toggle (Setting)
                        Button(
                            onClick = { 
                                playerViewModel.toggleShuffle() 
                                // ✅ Switch context to THIS playlist if not playing
                                if (currentQueueId != playlistId && filteredSongs.isNotEmpty()) {
                                    playerViewModel.playSong(filteredSongs.random(), filteredSongs, playlistId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isSmartShuffle -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    isShuffleEnabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = when {
                                    isSmartShuffle -> Color(0xFF10B981)
                                    isShuffleEnabled -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isSmartShuffle -> "Smart"
                                    isShuffleEnabled -> "Shuffle"
                                    else -> "Off"
                                },
                                color = when {
                                    isSmartShuffle -> Color(0xFF10B981)
                                    isShuffleEnabled -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Add Songs
                        IconButton(
                            onClick = {
                                navController.navigate("${Screen.AddSongs.route}/$playlistId")
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
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
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                            )
                                        )
                                    else
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.surface
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
                                    Color.White
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
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
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(if (searchQuery.isEmpty()) "🎵" else "🔍", fontSize = 36.sp)
                            Text(
                                if (searchQuery.isEmpty()) "No songs yet!" else "No results found!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Tap Add to add songs",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    val isCurrentSong = playerViewModel.currentSong.collectAsState().value?.id == song.id
                    val isPlaying by playerViewModel.isPlaying.collectAsState()

                    PlaylistSongItem(
                        song = song,
                        index = sortedSongs.indexOf(song) + 1,
                        context = context,
                        isCurrent = isCurrentSong,
                        isPlaying = isPlaying && isCurrentSong,
                        onClick = {
                            // ✅ Play full playlist queue starting from this song and mark context
                            playerViewModel.playSong(song, sortedSongs, playlistId)
                        },
                        onLike = {
                            viewModel.likeSong(song)
                        },
                        onRemove = {
                            viewModel.removeSongFromPlaylist(
                                playlistId, song.id
                            )
                        },
                        onDownload = { quality ->
                            downloadsViewModel.downloadSong(song, quality)
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
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onRemove: () -> Unit,
    onDownload: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDownloadQualities by remember { mutableStateOf(false) }
    val ringtoneLauncher = com.vibeup.android.presentation.ringtone.rememberRingtoneLauncher(
        com.vibeup.android.LocalNavController.current
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Index number or Playing Icon
        if (isPlaying) {
            Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = "${index}",
                fontSize = 12.sp,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

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
                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artist} • ${formatDuration(song.duration)}",
                fontSize = 11.sp,
                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { 
                    showMenu = false
                    showDownloadQualities = false 
                },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
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
                
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)

                // Download section
                DropdownMenuItem(
                    text = {
                        Text(
                            "📥 Download",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showDownloadQualities = !showDownloadQualities }
                )

                if (showDownloadQualities) {
                    listOf("320kbps", "160kbps", "96kbps").forEach { quality ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "   • $quality",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = {
                                showMenu = false
                                showDownloadQualities = false
                                onDownload(quality)
                            }
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = 0.5.dp
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "🔔 Set as Ringtone",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showMenu = false; ringtoneLauncher(song) }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
