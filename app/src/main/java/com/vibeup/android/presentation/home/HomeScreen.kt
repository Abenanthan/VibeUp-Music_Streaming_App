package com.vibeup.android.presentation.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.Screen
import com.vibeup.android.domain.model.Playlist
import com.vibeup.android.domain.model.Song
import com.vibeup.android.presentation.library.LibraryViewModel
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.ui.theme.*
import androidx.compose.foundation.gestures.ScrollableDefaults

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val favouriteSongs by viewModel.favouriteSongs.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val tamilSongs by viewModel.tamilSongs.collectAsState()
    val teluguSongs by viewModel.teluguSongs.collectAsState()
    val hindiSongs by viewModel.hindiSongs.collectAsState()
    val error by viewModel.error.collectAsState()
    val playlists by libraryViewModel.playlists.collectAsState()
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D2B),
                        Color(0xFF0A0A1A)
                    )
                )
            )
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = PurplePrimary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading your vibes...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "😔", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Something went wrong!",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.refresh() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)

                ) {
                    // Header
                    item { HomeHeader(navController = navController) }

                    // Abe's Favourites
                    item {
                        SectionTitle(
                            title = "❤️ Abe's Favourites",
                            subtitle = "Your personal picks"
                        )
                        if (favouriteSongs.isEmpty()) {
                            LoadingRow()
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp
                                ),
                                horizontalArrangement = Arrangement
                                    .spacedBy(12.dp)
                            ) {
                                items(
                                    items = favouriteSongs,
                                    key = { it.id }
                                ) { song ->
                                    GlassSongCard(
                                        song = song,
                                        playlists = playlists,
                                        onClick = {
                                            playerViewModel.playSong(
                                                song, favouriteSongs
                                            )
                                        },
                                        onLike = {
                                            libraryViewModel.likeSong(song)
                                        },
                                        onAddToPlaylist = { id ->
                                            libraryViewModel
                                                .addSongToPlaylist(id, song)
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Recently Played
                    item {
                        SectionTitle(
                            title = "🕐 Recently Played",
                            subtitle = "Continue where you left off"
                        )
                        if (recentlyPlayed.isEmpty()) {
                            EmptySection(
                                message = "No recently played songs yet!\nStart playing some music 🎵"
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp
                                ),
                                horizontalArrangement = Arrangement
                                    .spacedBy(12.dp)
                            ) {
                                items(
                                    items = recentlyPlayed,
                                    key = { it.id }
                                ) { song ->
                                    GlassSongCard(
                                        song = song,
                                        playlists = playlists,
                                        onClick = {
                                            playerViewModel.playSong(
                                                song, recentlyPlayed
                                            )
                                        },
                                        onLike = {
                                            libraryViewModel.likeSong(song)
                                        },
                                        onAddToPlaylist = { id ->
                                            libraryViewModel
                                                .addSongToPlaylist(id, song)
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Trending Now
                    item {
                        SectionTitle(
                            title = "🔥 Trending Now",
                            subtitle = "What everyone's listening to"
                        )
                        if (trendingSongs.isEmpty()) {
                            LoadingRow()
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp
                                ),
                                horizontalArrangement = Arrangement
                                    .spacedBy(12.dp)
                            ) {
                                items(
                                    items = trendingSongs,
                                    key = { it.id }
                                ) { song ->
                                    GlassSongCard(
                                        song = song,
                                        playlists = playlists,
                                        onClick = {
                                            playerViewModel.playSong(
                                                song, trendingSongs
                                            )
                                        },
                                        onLike = {
                                            libraryViewModel.likeSong(song)
                                        },
                                        onAddToPlaylist = { id ->
                                            libraryViewModel
                                                .addSongToPlaylist(id, song)
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Tamil Hits
                    item {
                        SectionTitle(
                            title = "🎵 Tamil Hits",
                            subtitle = "Best of Tamil music"
                        )
                        if (tamilSongs.isEmpty()) {
                            LoadingRow()
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp
                                ),
                                horizontalArrangement = Arrangement
                                    .spacedBy(12.dp)
                            ) {
                                items(
                                    items = tamilSongs,
                                    key = { it.id }
                                ) { song ->
                                    GlassSongCard(
                                        song = song,
                                        playlists = playlists,
                                        onClick = {
                                            playerViewModel.playSong(
                                                song, tamilSongs
                                            )
                                        },
                                        onLike = {
                                            libraryViewModel.likeSong(song)
                                        },
                                        onAddToPlaylist = { id ->
                                            libraryViewModel
                                                .addSongToPlaylist(id, song)
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Telugu Hits
                    item {
                        SectionTitle(
                            title = "🎶 Telugu Hits",
                            subtitle = "Best of Telugu music"
                        )
                        if (teluguSongs.isEmpty()) {
                            LoadingRow()
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp
                                ),
                                horizontalArrangement = Arrangement
                                    .spacedBy(12.dp)
                            ) {
                                items(
                                    items = teluguSongs,
                                    key = { it.id }
                                ) { song ->
                                    GlassSongCard(
                                        song = song,
                                        playlists = playlists,
                                        onClick = {
                                            playerViewModel.playSong(
                                                song, teluguSongs
                                            )
                                        },
                                        onLike = {
                                            libraryViewModel.likeSong(song)
                                        },
                                        onAddToPlaylist = { id ->
                                            libraryViewModel
                                                .addSongToPlaylist(id, song)
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Hindi Hits
                    item {
                        SectionTitle(
                            title = "🎼 Hindi Hits",
                            subtitle = "Best of Hindi music"
                        )
                        if (hindiSongs.isEmpty()) {
                            LoadingRow()
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(
                                    horizontal = 16.dp
                                ),
                                horizontalArrangement = Arrangement
                                    .spacedBy(12.dp)
                            ) {
                                items(
                                    items = hindiSongs,
                                    key = { it.id }
                                ) { song ->
                                    GlassSongCard(
                                        song = song,
                                        playlists = playlists,
                                        onClick = {
                                            playerViewModel.playSong(
                                                song, hindiSongs
                                            )
                                        },
                                        onLike = {
                                            libraryViewModel.likeSong(song)
                                        },
                                        onAddToPlaylist = { id ->
                                            libraryViewModel
                                                .addSongToPlaylist(id, song)
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "VibeUp 🎵",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PurpleLight, BlueLight)
                    )
                )
            )
            Text(
                text = "Feel the music 🎧",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PurplePrimary, BluePrimary)
                    ),
                    CircleShape
                )
                .clip(CircleShape)
                .clickable {
                    navController.navigate(Screen.Profile.route)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String = "") {
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 12.dp
        )
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun LoadingRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .size(155.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    color = PurplePrimary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun EmptySection(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassSongCard(
    song: Song,
    playlists: List<Playlist> = emptyList(),
    onClick: () -> Unit,
    onLike: () -> Unit = {},
    onAddToPlaylist: (String) -> Unit = {}
) {
    var showOptions by remember { mutableStateOf(false) }

    if (showOptions) {
        SongOptionsDialog(
            song = song,
            playlists = playlists,
            onDismiss = { showOptions = false },
            onLike = onLike,
            onAddToPlaylist = onAddToPlaylist
        )
    }

    Column(
        modifier = Modifier
            .width(155.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showOptions = true }
            )
    ) {
        Box(
            modifier = Modifier
                .size(155.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            // Album Art
            AsyncImage(
                model = song.imageUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0A0A1A).copy(alpha = 0.7f)
                            ),
                            startY = 80f
                        )
                    )
            )

            // Glass effect border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Options button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { showOptions = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Purple glow at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                PurplePrimary.copy(alpha = 0.8f),
                                BluePrimary.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = song.artist,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun SongOptionsDialog(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onLike: () -> Unit,
    onAddToPlaylist: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                TextButton(
                    onClick = { onLike(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = PurplePrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Liked Songs", color = Color.White)
                    }
                }
                HorizontalDivider(color = DarkElevated)
                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists yet!\nCreate one in Library",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Text(
                        text = "Add to playlist:",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(
                            top = 8.dp, start = 8.dp
                        )
                    )
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = {
                                onAddToPlaylist(playlist.id)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(playlist.name, color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkCard
    )
}
