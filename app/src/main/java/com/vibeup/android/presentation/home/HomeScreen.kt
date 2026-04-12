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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibeup.android.Screen
import com.vibeup.android.domain.model.Playlist
import com.vibeup.android.domain.model.Song
import com.vibeup.android.presentation.library.LibraryViewModel
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val favouriteSongs by viewModel.favouriteSongs.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val newReleases by viewModel.newReleases.collectAsState()
    val romanticSongs by viewModel.romanticSongs.collectAsState()
    val partySongs by viewModel.partySongs.collectAsState()
    val chillSongs by viewModel.chillSongs.collectAsState()
    val sadSongs by viewModel.sadSongs.collectAsState()
    val arRahmanSongs by viewModel.arRahmanSongs.collectAsState()
    val anirudhSongs by viewModel.anirudhSongs.collectAsState()
    val sidSriramSongs by viewModel.sidSriramSongs.collectAsState()
    val arijitSongs by viewModel.arijitSongs.collectAsState()
    val gvPrakashSongs by viewModel.gvPrakashSongs.collectAsState()
    val hipHopSongs by viewModel.hipHopSongs.collectAsState()
    val tamilSongs by viewModel.tamilSongs.collectAsState()
    val teluguSongs by viewModel.teluguSongs.collectAsState()
    val hindiSongs by viewModel.hindiSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val libraryPlaylists by libraryViewModel.playlists.collectAsState()
    val listState = rememberLazyListState()

    fun playSong(song: Song, queue: List<Song>) {
        playerViewModel.playSong(song, queue)
    }

    fun likeSong(song: Song) = libraryViewModel.likeSong(song)

    fun addToPlaylist(playlistId: String, song: Song) =
        libraryViewModel.addSongToPlaylist(playlistId, song)

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
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = PurplePrimary,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Loading your vibes...",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // ── 1. Header ──
                item(key = "header") {
                    HomeHeader(navController = navController)
                }

                // ── 2. Search Shortcut ──
                item(key = "search") {
                    SearchShortcut(
                        onClick = {
                            navController.navigate(Screen.Search.route)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── 3. Playlist Shortcuts ──
                if (playlists.isNotEmpty()) {
                    item(key = "playlists") {
                        SectionTitle(
                            title = "📋 Your Playlists",
                            subtitle = "Jump right in"
                        )
                        PlaylistShortcuts(
                            playlists = playlists,
                            onPlaylistClick = { playlist ->
                                navController.navigate(
                                    "${Screen.Playlist.route}/${playlist.id}"
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // ── 4. Abe's Favourites ──
                item(key = "favourites") {
                    SectionTitle(
                        title = "❤️ Abeeee's Favourites",
                        subtitle = "Your personal picks"
                    )
                    SongRow(
                        songs = favouriteSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { song ->
                            playSong(song, favouriteSongs)
                        },
                        onLike = { song -> likeSong(song) },
                        onAddToPlaylist = { id, song ->
                            addToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 5. Recently Played ──
                item(key = "recent") {
                    SectionTitle(
                        title = "🕐 Recently Played",
                        subtitle = "Continue where you left off"
                    )
                    if (recentlyPlayed.isEmpty()) {
                        EmptySection("Play some songs to see them here! 🎵")
                    } else {
                        SongRow(
                            songs = recentlyPlayed,
                            context = context,
                            playlists = libraryPlaylists,
                            onSongClick = { song ->
                                playSong(song, recentlyPlayed)
                            },
                            onLike = { song -> likeSong(song) },
                            onAddToPlaylist = { id, song ->
                                addToPlaylist(id, song)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 6. Moods ──
                item(key = "moods") {
                    SectionTitle(
                        title = "🎭 Moods",
                        subtitle = "Music for every feeling"
                    )
                    MoodsSection(
                        romantic = romanticSongs,
                        party = partySongs,
                        chill = chillSongs,
                        sad = sadSongs,
                        onMoodClick = { songs, song ->
                            playSong(song, songs)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 7. Trending Now ──
                item(key = "trending") {
                    SectionTitle(
                        title = "🔥 Trending Now",
                        subtitle = "What everyone's listening to"
                    )
                    SongRow(
                        songs = trendingSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { song ->
                            playSong(song, trendingSongs)
                        },
                        onLike = { song -> likeSong(song) },
                        onAddToPlaylist = { id, song ->
                            addToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 8. New Releases ──
                item(key = "new_releases") {
                    SectionTitle(
                        title = "🆕 New Releases",
                        subtitle = "Fresh music just for you"
                    )
                    SongRow(
                        songs = newReleases,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { song ->
                            playSong(song, newReleases)
                        },
                        onLike = { song -> likeSong(song) },
                        onAddToPlaylist = { id, song ->
                            addToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 9. Artists ──
                item(key = "artists") {
                    SectionTitle(
                        title = "🎤 Artists",
                        subtitle = "Your favourite musicians"
                    )
                    ArtistsSection(
                        arRahman = arRahmanSongs,
                        anirudh = anirudhSongs,
                        sidSriram = sidSriramSongs,
                        arijit = arijitSongs,
                        gvPrakash = gvPrakashSongs,
                        hipHop = hipHopSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { song, queue ->
                            playSong(song, queue)
                        },
                        onLike = { song -> likeSong(song) },
                        onAddToPlaylist = { id, song ->
                            addToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 10. Tamil Hits ──
                item(key = "tamil") {
                    SectionTitle(
                        title = "🎵 Tamil Hits",
                        subtitle = "Best of Tamil music"
                    )
                    SongRow(
                        songs = tamilSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { song ->
                            playSong(song, tamilSongs)
                        },
                        onLike = { song -> likeSong(song) },
                        onAddToPlaylist = { id, song ->
                            addToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 11. Telugu Hits ──
                item(key = "telugu") {
                    SectionTitle(
                        title = "🎶 Telugu Hits",
                        subtitle = "Best of Telugu music"
                    )
                    SongRow(
                        songs = teluguSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { song ->
                            playSong(song, teluguSongs)
                        },
                        onLike = { song -> likeSong(song) },
                        onAddToPlaylist = { id, song ->
                            addToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── 12. Hindi Hits ──
                item(key = "hindi") {
                    SectionTitle(
                        title = "🎼 Hindi Hits",
                        subtitle = "Best of Hindi music"
                    )
                    SongRow(
                        songs = hindiSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { song ->
                            playSong(song, hindiSongs)
                        },
                        onLike = { song -> likeSong(song) },
                        onAddToPlaylist = { id, song ->
                            addToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ── Header ──
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
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PurpleLight, BlueLight)
                    )
                )
            )
            Text(
                text = "Feel the music 🎧",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
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
                Icons.Default.Person,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ── Search Shortcut ──
@Composable
fun SearchShortcut(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkCard)
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Search songs, artists...",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

// ── Playlist Shortcuts ──
@Composable
fun PlaylistShortcuts(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items = playlists, key = { it.id }) { playlist ->
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PurplePrimary.copy(alpha = 0.4f),
                                BluePrimary.copy(alpha = 0.4f)
                            )
                        )
                    )
                    .clickable { onPlaylistClick(playlist) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎵", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = playlist.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Moods Section ──
@Composable
fun MoodsSection(
    romantic: List<Song>,
    party: List<Song>,
    chill: List<Song>,
    sad: List<Song>,
    onMoodClick: (List<Song>, Song) -> Unit
) {
    val moods = listOf(
        Triple("💕 Romantic", romantic, listOf(Color(0xFFFF6B9D), Color(0xFFFF8E53))),
        Triple("🎉 Party", party, listOf(Color(0xFFFFD93D), Color(0xFFFF6B6B))),
        Triple("😌 Chill", chill, listOf(Color(0xFF4FC3F7), Color(0xFF00B4D8))),
        Triple("😢 Sad", sad, listOf(Color(0xFF667EEA), Color(0xFF764BA2)))
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(moods) { (label, songs, colors) ->
            MoodCard(
                label = label,
                songs = songs,
                gradientColors = colors,
                onClick = {
                    if (songs.isNotEmpty()) {
                        onMoodClick(songs, songs.first())
                    }
                }
            )
        }
    }
}

@Composable
fun MoodCard(
    label: String,
    songs: List<Song>,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(colors = gradientColors)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (songs.isNotEmpty()) {
                Text(
                    text = "${songs.size} songs",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "Loading...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ── Artists Section ──
@Composable
fun ArtistsSection(
    arRahman: List<Song>,
    anirudh: List<Song>,
    sidSriram: List<Song>,
    arijit: List<Song>,
    gvPrakash: List<Song>,
    hipHop: List<Song>,
    context: android.content.Context,
    playlists: List<Playlist>,
    onSongClick: (Song, List<Song>) -> Unit,
    onLike: (Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit
) {
    val artists = listOf(
        Triple("AR Rahman 🎹", arRahman, "🎹"),
        Triple("Anirudh 🎸", anirudh, "🎸"),
        Triple("Sid Sriram 🎤", sidSriram, "🎤"),
        Triple("Arijit Singh 💙", arijit, "💙"),
        Triple("GV Prakash 🎵", gvPrakash, "🎵"),
        Triple("Hip Hop Tamizha 🔥", hipHop, "🔥")
    )

    Column {
        artists.forEach { (name, songs, _) ->
            if (songs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        PurplePrimary,
                                        BluePrimary
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎵",
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = name,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = songs,
                        key = { "${name}_${it.id}" }
                    ) { song ->
                        GlassSongCard(
                            song = song,
                            context = context,
                            playlists = playlists,
                            onClick = { onSongClick(song, songs) },
                            onLike = { onLike(song) },
                            onAddToPlaylist = { id ->
                                onAddToPlaylist(id, song)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Song Row ──
@Composable
fun SongRow(
    songs: List<Song>,
    context: android.content.Context,
    playlists: List<Playlist>,
    onSongClick: (Song) -> Unit,
    onLike: (Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit
) {
    if (songs.isEmpty()) {
        LoadingRow()
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = songs, key = { it.id }) { song ->
                GlassSongCard(
                    song = song,
                    context = context,
                    playlists = playlists,
                    onClick = { onSongClick(song) },
                    onLike = { onLike(song) },
                    onAddToPlaylist = { id ->
                        onAddToPlaylist(id, song)
                    }
                )
            }
        }
    }
}

// ── Glass Song Card ──
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassSongCard(
    song: Song,
    context: android.content.Context,
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
            .width(140.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showOptions = true }
            )
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(song.imageUrl)
                    .crossfade(300)
                    .memoryCacheKey(song.id)
                    .diskCacheKey(song.id)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient
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

            // Options
            IconButton(
                onClick = { showOptions = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Purple/Blue bottom line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                PurplePrimary,
                                BluePrimary
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = song.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Song Options Dialog ──
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
                        Text(
                            "Add to Liked Songs",
                            color = Color.White
                        )
                    }
                }
                HorizontalDivider(color = DarkElevated)
                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists yet!\nCreate one in Library 📚",
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
                                Text(
                                    playlist.name,
                                    color = Color.White
                                )
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

// ── Section Title ──
@Composable
fun SectionTitle(title: String, subtitle: String = "") {
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 10.dp
        )
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

// ── Loading Row ──
@Composable
fun LoadingRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(4) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PurplePrimary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// ── Empty Section ──
@Composable
fun EmptySection(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
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