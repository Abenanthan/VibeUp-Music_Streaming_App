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

// ── Mood Data Class ──
data class MoodItem(
    val label: String,
    val emoji: String,
    val songs: List<Song>,
    val gradientColors: List<Color>,
    val glowColor: Color
)

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
    val homePlaylists by viewModel.playlists.collectAsState()
    val libraryPlaylists by libraryViewModel.playlists.collectAsState()
    val listState = rememberLazyListState()

    val moods = remember(romanticSongs, partySongs, chillSongs, sadSongs) {
        listOf(
            MoodItem(
                "Romantic", "💕", romanticSongs,
                listOf(Color(0xFFBE185D), Color(0xFFF97316)),
                Color(0xFFFB7185)
            ),
            MoodItem(
                "Party", "🎉", partySongs,
                listOf(Color(0xFFB45309), Color(0xFFDC2626)),
                Color(0xFFFBBF24)
            ),
            MoodItem(
                "Chill", "😌", chillSongs,
                listOf(Color(0xFF0E7490), Color(0xFF1D4ED8)),
                Color(0xFF38BDF8)
            ),
            MoodItem(
                "Sad", "😢", sadSongs,
                listOf(Color(0xFF4C1D95), Color(0xFF1E3A8A)),
                Color(0xFFA78BFA)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = PurplePrimary,
                    modifier = Modifier.size(44.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Loading your vibes...",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // ── Header ──
                item(key = "header") {
                    HomeHeader(navController = navController)
                }

                // ── Search Shortcut ──
                item(key = "search") {
                    SearchBar(
                        onClick = { navController.navigate(Screen.Search.route) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Playlist Shortcuts ──
                if (homePlaylists.isNotEmpty()) {
                    item(key = "playlists") {
                        SectionHeader(
                            title = "📋 Your Playlists",
                            subtitle = "Jump right in"
                        )
                        PlaylistShortcuts(
                            playlists = homePlaylists,
                            onClick = { playlist ->
                                navController.navigate(
                                    "${Screen.Playlist.route}/${playlist.id}"
                                )
                            }
                        )
                        SectionDivider()
                    }
                }

                // ── Abe's Favourites ──
                item(key = "favs") {
                    SectionHeader(
                        title = "❤️ Abe's Favourites",
                        subtitle = "Angel's Favourites"
                    )
                    SongRow(
                        songs = favouriteSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { playerViewModel.playSong(it, favouriteSongs) },
                        onLike = { libraryViewModel.likeSong(it) },
                        onAddToPlaylist = { id, song ->
                            libraryViewModel.addSongToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Recently Played ──
                item(key = "recent") {
                    SectionHeader(
                        title = "🕐 Recently Played",
                        subtitle = "Continue where you left off"
                    )
                    if (recentlyPlayed.isEmpty()) {
                        EmptyRow("Play songs to see them here 🎵")
                    } else {
                        SongRow(
                            songs = recentlyPlayed,
                            context = context,
                            playlists = libraryPlaylists,
                            onSongClick = { playerViewModel.playSong(it, recentlyPlayed) },
                            onLike = { libraryViewModel.likeSong(it) },
                            onAddToPlaylist = { id, song ->
                                libraryViewModel.addSongToPlaylist(id, song)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Moods ──
                item(key = "moods") {
                    SectionHeader(
                        title = "🎭 Moods",
                        subtitle = "Music for every feeling"
                    )
                    MoodsGrid(
                        moods = moods,
                        onMoodClick = { songs ->
                            if (songs.isNotEmpty()) {
                                playerViewModel.playSong(songs.first(), songs)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Trending Now ──
                item(key = "trending") {
                    SectionHeader(
                        title = "🔥 Trending Now",
                        subtitle = "What everyone's listening to"
                    )
                    SongRow(
                        songs = trendingSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { playerViewModel.playSong(it, trendingSongs) },
                        onLike = { libraryViewModel.likeSong(it) },
                        onAddToPlaylist = { id, song ->
                            libraryViewModel.addSongToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── New Releases ──
                item(key = "new") {
                    SectionHeader(
                        title = "🆕 New Releases",
                        subtitle = "Fresh music just for you"
                    )
                    SongRow(
                        songs = newReleases,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { playerViewModel.playSong(it, newReleases) },
                        onLike = { libraryViewModel.likeSong(it) },
                        onAddToPlaylist = { id, song ->
                            libraryViewModel.addSongToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Artists ──
                item(key = "artists") {
                    SectionHeader(
                        title = "🎤 Artists",
                        subtitle = "Your favourite musicians"
                    )
                    val artistsData = remember(anirudhSongs, sidSriramSongs, arijitSongs, gvPrakashSongs, hipHopSongs) {
                        listOf(
                            ArtistData("Anirudh 🎸", anirudhSongs, "🎸", "https://c.saavncdn.com/artists/Anirudh_Ravichander_500x500.jpg"),
                            ArtistData("Sid Sriram 🎤", sidSriramSongs, "🎤", "https://c.saavncdn.com/artists/Sid_Sriram_500x500.jpg"),
                            ArtistData("Arijit Singh 💙", arijitSongs, "💙", "https://c.saavncdn.com/artists/Arijit_Singh_500x500.jpg"),
                            ArtistData("GV Prakash 🎵", gvPrakashSongs, "🎵", "https://c.saavncdn.com/artists/G_V_Prakash_Kumar_500x500.jpg"),
                            ArtistData("HHT 🔥", hipHopSongs, "🔥", "https://c.saavncdn.com/artists/Hiphop_Tamizha_500x500.jpg")
                        )
                    }
                    ArtistsSection(
                        artists = artistsData,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { song, queue ->
                            playerViewModel.playSong(song, queue)
                        },
                        onLike = { libraryViewModel.likeSong(it) },
                        onAddToPlaylist = { id, song ->
                            libraryViewModel.addSongToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Tamil Hits ──
                item(key = "tamil") {
                    SectionHeader(
                        title = "🎵 Tamil Hits",
                        subtitle = "Best of Tamil music"
                    )
                    SongRow(
                        songs = tamilSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { playerViewModel.playSong(it, tamilSongs) },
                        onLike = { libraryViewModel.likeSong(it) },
                        onAddToPlaylist = { id, song ->
                            libraryViewModel.addSongToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Telugu Hits ──
                item(key = "telugu") {
                    SectionHeader(
                        title = "🎶 Telugu Hits",
                        subtitle = "Best of Telugu music"
                    )
                    SongRow(
                        songs = teluguSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { playerViewModel.playSong(it, teluguSongs) },
                        onLike = { libraryViewModel.likeSong(it) },
                        onAddToPlaylist = { id, song ->
                            libraryViewModel.addSongToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Hindi Hits ──
                item(key = "hindi") {
                    SectionHeader(
                        title = "🎼 Hindi Hits",
                        subtitle = "Best of Hindi music"
                    )
                    SongRow(
                        songs = hindiSongs,
                        context = context,
                        playlists = libraryPlaylists,
                        onSongClick = { playerViewModel.playSong(it, hindiSongs) },
                        onLike = { libraryViewModel.likeSong(it) },
                        onAddToPlaylist = { id, song ->
                            libraryViewModel.addSongToPlaylist(id, song)
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// Artist Data Helper
data class ArtistData(
    val name: String,
    val songs: List<Song>,
    val emoji: String,
    val artistImageUrl: String
)

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
                text = "VibeUp",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFA78BFA),
                            Color(0xFF60A5FA)
                        )
                    )
                )
            )
            Text(
                text = "Feel the music 🎧",
                fontSize = 11.sp,
                color = Color(0xFF4B5563)
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
                .clickable { navController.navigate(Screen.Profile.route) },
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

// ── Search Bar ──
@Composable
fun SearchBar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(Color(0xFF12122A))
            .clickable { onClick() }
            .then(
                Modifier.padding(horizontal = 16.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF4B5563),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Search songs, artists, albums...",
                color = Color(0xFF374151),
                fontSize = 13.sp
            )
        }
    }
}

// ── Section Header ──
@Composable
fun SectionHeader(title: String, subtitle: String = "") {
    Column(
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 10.dp
        )
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF3F4F6)
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF4B5563)
            )
        }
    }
}

// ── Section Divider ──
@Composable
fun SectionDivider() {
    Spacer(modifier = Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color(0xFF1a1a3a))
    )
    Spacer(modifier = Modifier.height(16.dp))
}

// ── Playlist Shortcuts ──
@Composable
fun PlaylistShortcuts(
    playlists: List<Playlist>,
    onClick: (Playlist) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = playlists, key = { it.id }) { playlist ->
            val gradients = remember {
                listOf(
                    listOf(Color(0xFF4C1D95), Color(0xFF7C3AED)),
                    listOf(Color(0xFFBE185D), Color(0xFFF97316)),
                    listOf(Color(0xFF0E7490), Color(0xFF1D4ED8)),
                    listOf(Color(0xFF065F46), Color(0xFF059669))
                )
            }
            val grad = gradients[playlists.indexOf(playlist) % gradients.size]

            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                grad[0].copy(alpha = 0.3f),
                                grad[1].copy(alpha = 0.3f)
                            )
                        )
                    )
                    .then(
                        Modifier.background(
                            Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                    )
                    .clickable { onClick(playlist) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            Brush.linearGradient(colors = grad),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎵", fontSize = 12.sp)
                }
                Text(
                    text = playlist.name,
                    color = Color(0xFFD1D5DB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 80.dp)
                )
            }
        }
    }
}

// ── Moods Grid ──
@Composable
fun MoodsGrid(
    moods: List<MoodItem>,
    onMoodClick: (List<Song>) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            moods.take(2).forEach { mood ->
                MoodCard(
                    mood = mood,
                    onClick = { onMoodClick(mood.songs) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            moods.drop(2).forEach { mood ->
                MoodCard(
                    mood = mood,
                    onClick = { onMoodClick(mood.songs) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MoodCard(
    mood: MoodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(colors = mood.gradientColors)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        // Glow circle
        Box(
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
                .background(
                    mood.glowColor.copy(alpha = 0.35f),
                    CircleShape
                )
        )
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = mood.emoji, fontSize = 20.sp)
            Text(
                text = mood.label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (mood.songs.isEmpty()) "Loading..."
                else "${mood.songs.size} songs",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 10.sp
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.CenterEnd)
        )
    }
}

// ── Artists Section ──
@Composable
fun ArtistsSection(
    artists: List<ArtistData>,
    context: android.content.Context,
    playlists: List<Playlist>,
    onSongClick: (Song, List<Song>) -> Unit,
    onLike: (Song) -> Unit,
    onAddToPlaylist: (String, Song) -> Unit
) {
    // Artist avatars row
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        items(artists, key = { it.name }) { artist ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PurplePrimary, BluePrimary)
                            ),
                            CircleShape
                        )
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF12122A), CircleShape)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(artist.artistImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = artist.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = artist.name.substringBefore(" ").trim(),
                    fontSize = 10.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    // Songs per artist
    artists.forEach { artist ->
        if (artist.songs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(PurplePrimary, BluePrimary)
                            ),
                            RoundedCornerShape(2.dp)
                        )
                )
                Text(
                    text = artist.name,
                    color = Color(0xFFE5E7EB),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(
                    items = artist.songs,
                    key = { "${artist.name}_${it.id}" }
                ) { song ->
                    GlassSongCard(
                        song = song,
                        context = context,
                        playlists = playlists,
                        onClick = { onSongClick(song, artist.songs) },
                        onLike = { onLike(song) },
                        onAddToPlaylist = { id -> onAddToPlaylist(id, song) }
                    )
                }
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items = songs, key = { it.id }) { song ->
                GlassSongCard(
                    song = song,
                    context = context,
                    playlists = playlists,
                    onClick = { onSongClick(song) },
                    onLike = { onLike(song) },
                    onAddToPlaylist = { id -> onAddToPlaylist(id, song) }
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
            .width(135.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showOptions = true }
            )
    ) {
        Box(
            modifier = Modifier
                .size(135.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            // Album Art
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

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCC0A0A1A)
                            ),
                            startY = 60f
                        )
                    )
            )

            // Options button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
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
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Purple/Blue gradient bottom line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(PurplePrimary, BluePrimary)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = song.title,
            fontSize = 12.sp,
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
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFEC4899)
                        )
                        Text(
                            "Add to Liked Songs",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
                HorizontalDivider(
                    color = Color(0xFF1F1F3A),
                    thickness = 1.dp
                )
                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists yet!\nCreate one in Library 📚",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Text(
                        text = "Add to playlist:",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(
                            top = 8.dp, start = 8.dp, bottom = 4.dp
                        )
                    )
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { onAddToPlaylist(playlist.id); onDismiss() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = PurplePrimary
                                )
                                Text(
                                    playlist.name,
                                    color = Color.White,
                                    fontSize = 14.sp
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
                Text("Cancel", color = Color(0xFF6B7280))
            }
        },
        containerColor = Color(0xFF12122A),
        shape = RoundedCornerShape(20.dp)
    )
}

// ── Loading Row ──
@Composable
fun LoadingRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(4) {
            Box(
                modifier = Modifier
                    .size(135.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF12122A)),
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

//emotes to be added
@Composable
fun EmptyRow(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF12122A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color(0xFF4B5563),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}