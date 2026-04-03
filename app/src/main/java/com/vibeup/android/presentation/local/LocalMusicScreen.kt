package com.vibeup.android.presentation.local

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.domain.model.LocalSong
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.ui.theme.DarkBackground
import com.vibeup.android.ui.theme.DarkCard
import com.vibeup.android.ui.theme.TextSecondary
import com.vibeup.android.ui.theme.VibeUpGreen

@Composable
fun LocalMusicScreen(
    navController: NavController,
    viewModel: LocalMusicViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val localSongs by viewModel.filteredSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndScan()
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.onPermissionGranted()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Local Music 📁",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = VibeUpGreen,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!hasPermission) {
                // Permission Request UI
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = VibeUpGreen,
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            text = "Access Local Music",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Allow VibeUp to access your\nlocal music files",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = {
                                val permission =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                        Manifest.permission.READ_MEDIA_AUDIO
                                    else
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                permissionLauncher.launch(permission)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VibeUpGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Grant Permission",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text("Search local songs...", color = TextSecondary)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChange("") }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibeUpGreen,
                        unfocusedBorderColor = DarkCard,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = VibeUpGreen
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = VibeUpGreen
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Scanning music...",
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    localSongs.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "🎵", fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No local songs found!",
                                    color = TextSecondary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Add mp3, flac or m4a files\nto your device",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    else -> {
                        // Songs count
                        Text(
                            text = "${localSongs.size} songs found",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Songs List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = localSongs,
                                key = { it.id }
                            ) { song ->
                                LocalSongItem(
                                    song = song,
                                    onClick = {
                                        playerViewModel.playSong(
                                            song.toSong(),
                                            localSongs.map { it.toSong() }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocalSongItem(
    song: LocalSong,
    onClick: () -> Unit
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
            // Album Art
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = {
                        // Show music note if no album art
                    }
                )
                // Fallback icon
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Format badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = VibeUpGreen.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = song.format,
                            color = VibeUpGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = 4.dp,
                                vertical = 2.dp
                            )
                        )
                    }
                    // Duration
                    Text(
                        text = formatLocalDuration(song.duration),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

fun formatLocalDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}