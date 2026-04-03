package com.vibeup.android.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vibeup.android.Screen
import com.vibeup.android.domain.model.Playlist
import com.vibeup.android.domain.model.Song
import com.vibeup.android.presentation.auth.AuthViewModel
import com.vibeup.android.ui.theme.DarkBackground
import com.vibeup.android.ui.theme.DarkCard
import com.vibeup.android.ui.theme.DarkSurface
import com.vibeup.android.ui.theme.TextSecondary
import com.vibeup.android.ui.theme.VibeUpGreen

@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val likedSongs by viewModel.likedSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }
    var playlistDescription by remember { mutableStateOf("") }

    // Show message snackbar
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                playlistName = ""
                playlistDescription = ""
            },
            title = {
                Text(
                    "Create Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        placeholder = {
                            Text("Playlist name", color = TextSecondary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibeUpGreen,
                            unfocusedBorderColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = VibeUpGreen,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = playlistDescription,
                        onValueChange = { playlistDescription = it },
                        placeholder = {
                            Text(
                                "Description (optional)",
                                color = TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VibeUpGreen,
                            unfocusedBorderColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = VibeUpGreen,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(
                                playlistName,
                                playlistDescription
                            )
                            showCreatePlaylistDialog = false
                            playlistName = ""
                            playlistDescription = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibeUpGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePlaylistDialog = false
                    playlistName = ""
                    playlistDescription = ""
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard
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
                Text("Are you sure you want to logout?", color = TextSecondary)
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
                        containerColor = Color.Red
                    )
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library 📚",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = VibeUpGreen
                    )
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = TextSecondary
                        )
                    }
                }
                currentUser?.let {
                    Text(
                        text = it.email ?: "",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Liked Songs Section
            item {
                Text(
                    text = "💚 Liked Songs",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (likedSongs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkCard
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No liked songs yet!\nLike songs to see them here 🎵",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(likedSongs) { song ->
                    LikedSongItem(
                        song = song,
                        onUnlike = { viewModel.unlikeSong(song.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // Playlists Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎵 Your Playlists",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { showCreatePlaylistDialog = true }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create Playlist",
                            tint = VibeUpGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (playlists.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkCard
                        ),
                        onClick = { showCreatePlaylistDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = VibeUpGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Create your first playlist!",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(playlists) { playlist ->
                    PlaylistItem(
                        playlist = playlist,
                        onClick = {
                            navController.navigate(
                                "${Screen.Playlist.route}/${playlist.id}"
                            )
                        },
                        onDelete = { viewModel.deletePlaylist(playlist.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VibeUpGreen
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(12.dp),
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LikedSongItem(
    song: Song,
    onUnlike: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = VibeUpGreen,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = song.artist,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onUnlike) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Unlike",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(DarkSurface, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎵", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (playlist.description.isNotEmpty()) {
                    Text(
                        text = playlist.description,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextSecondary
                )
            }
        }
    }
}