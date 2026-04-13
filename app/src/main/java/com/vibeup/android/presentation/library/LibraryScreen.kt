package com.vibeup.android.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vibeup.android.Screen
import com.vibeup.android.domain.model.Playlist
import com.vibeup.android.domain.model.Song
import com.vibeup.android.presentation.auth.AuthViewModel
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.ui.theme.*
import androidx.compose.foundation.shape.CircleShape

@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val likedSongs by viewModel.likedSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var playlistDesc by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessage()
        }
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                playlistName = ""
                playlistDesc = ""
            },
            title = {
                Text(
                    "New Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        placeholder = {
                            Text("Playlist name", color = Color(0xFF4B5563))
                        },
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
                    OutlinedTextField(
                        value = playlistDesc,
                        onValueChange = { playlistDesc = it },
                        placeholder = {
                            Text(
                                "Description (optional)",
                                color = Color(0xFF4B5563)
                            )
                        },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName, playlistDesc)
                            showCreateDialog = false
                            playlistName = ""
                            playlistDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    playlistName = ""
                    playlistDesc = ""
                }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            },
            containerColor = Color(0xFF12122A),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to logout?",
                    color = Color(0xFF9CA3AF)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.logout()
                        showLogoutDialog = false
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFA78BFA),
                                    Color(0xFF60A5FA)
                                )
                            )
                        )
                    )
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // ── Tabs ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0D0D2B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Playlists", "Liked Songs", "Recent").forEachIndexed { index, tab ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedTab == index)
                                        Brush.linearGradient(
                                            colors = listOf(
                                                PurplePrimary.copy(alpha = 0.5f),
                                                BluePrimary.copy(alpha = 0.5f)
                                            )
                                        )
                                    else
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent
                                            )
                                        )
                                )
                                .clickable { selectedTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedTab == index)
                                    Color(0xFFE5E7EB)
                                else
                                    Color(0xFF4B5563)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            when (selectedTab) {
                // ── Playlists Tab ──
                0 -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🎵 Your Playlists",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF3F4F6)
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                PurplePrimary,
                                                BluePrimary
                                            )
                                        )
                                    )
                                    .clickable { showCreateDialog = true }
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "New Playlist",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Liked Songs Card
                    item {
                        LikedSongsCard(
                            count = likedSongs.size,
                            onClick = { selectedTab = 1 }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (playlists.isEmpty()) {
                        item {
                            EmptyPlaylistCard(
                                onClick = { showCreateDialog = true }
                            )
                        }
                    } else {
                        items(playlists) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                songs = emptyList(),
                                onClick = {
                                    navController.navigate(
                                        "${Screen.Playlist.route}/${playlist.id}"
                                    )
                                },
                                onRename = { newName ->
                                    viewModel.renamePlaylist(playlist.id, newName)
                                },
                                onDelete = {
                                    viewModel.deletePlaylist(playlist.id)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // ── Liked Songs Tab ──
                1 -> {
                    item {
                        Text(
                            "💚 Liked Songs",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    if (likedSongs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF12122A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No liked songs yet!\nLike songs to see them here 💚",
                                    color = Color(0xFF4B5563),
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(likedSongs) { song ->
                            LikedSongItem(
                                song = song,
                                onPlay = {
                                    playerViewModel.playSong(song, likedSongs)
                                },
                                onUnlike = {
                                    viewModel.unlikeSong(song.id)
                                }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                // ── Recent Tab ──
                2 -> {
                    item {
                        Text(
                            "🕐 Recently Played",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF12122A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Check Home screen\nfor recently played songs 🎵",
                                color = Color(0xFF4B5563),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Message Snackbar
        message?.let {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PurplePrimary
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(14.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ── Liked Songs Card ──
@Composable
fun LikedSongsCard(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFEC4899).copy(alpha = 0.2f),
                        Color(0xFF8B5CF6).copy(alpha = 0.2f)
                    )
                )
            )
            .then(
                Modifier.background(
                    Color.Transparent,
                    RoundedCornerShape(16.dp)
                )
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFEC4899),
                            Color(0xFF8B5CF6)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Liked Songs",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF3F4F6)
            )
            Text(
                "$count songs",
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PurplePrimary, BluePrimary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Playlist Card ──
@Composable
fun PlaylistCard(
    playlist: Playlist,
    songs: List<Song>,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(playlist.name) }

    val gradients = listOf(
        listOf(Color(0xFF4C1D95), Color(0xFF7C3AED)),
        listOf(Color(0xFF1E3A8A), Color(0xFF2563EB)),
        listOf(Color(0xFF064E3B), Color(0xFF059669)),
        listOf(Color(0xFF7C2D12), Color(0xFFDC2626))
    )

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
                            onRename(newName)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D0D2B))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2x2 cover grid
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = gradients[0]
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♪", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = gradients[2]
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♩", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = gradients[1]
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♫", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = gradients[3]
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♬", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF3F4F6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (playlist.description.isNotEmpty()) {
                Text(
                    text = playlist.description,
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Three dots menu
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFF6B7280),
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
                            "✏️ Rename",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        showMenu = false
                        newName = playlist.name
                        showRenameDialog = true
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "🗑️ Delete",
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF374151),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Empty Playlist Card ──
@Composable
fun EmptyPlaylistCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D2B))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PurplePrimary, BluePrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                "Create your first playlist!",
                color = Color(0xFF4B5563),
                fontSize = 13.sp
            )
        }
    }
}

// ── Liked Song Item ──
@Composable
fun LikedSongItem(
    song: Song,
    onPlay: () -> Unit,
    onUnlike: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0D2B))
            .clickable { onPlay() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(PurplePrimary, BluePrimary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
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
                text = song.artist,
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
                            "Play",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showMenu = false; onPlay() }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "Unlike",
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showMenu = false; onUnlike() }
                )
            }
        }
    }
}