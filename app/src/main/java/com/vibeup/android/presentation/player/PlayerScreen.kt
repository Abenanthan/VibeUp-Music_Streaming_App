package com.vibeup.android.presentation.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.vibeup.android.Screen
import com.vibeup.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = activityViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val isSmartShuffle by viewModel.isSmartShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isResolvingUrl by viewModel.isResolvingUrl.collectAsState()
    val lyricsState by lyricsViewModel.lyricsState.collectAsState()
    val currentLineIndex by lyricsViewModel.currentLineIndex.collectAsState()
    val showSynced by lyricsViewModel.showSynced.collectAsState()

    val isLiked by viewModel.isLiked.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val activeQueue by viewModel.activeQueue.collectAsState()

    // ✅ Load lyrics as soon as song is available
    /*LaunchedEffect(currentSong?.id) {
        currentSong?.let { lyricsViewModel.loadLyrics(it) }
    }*/

    LaunchedEffect(currentSong?.id) {
        currentSong?.let { song ->
            val state = lyricsViewModel.lyricsState.value
            if (state is LyricsState.Idle ||
                state is LyricsState.Error
            ) {
                lyricsViewModel.loadLyrics(song)
            } else if (lyricsViewModel.lastLoadedSongId != song.id) {
                lyricsViewModel.loadLyrics(song)
            }
        }
    }

    LaunchedEffect(currentSong?.id, activeQueue) {
        val currentIndex = activeQueue.indexOfFirst {
            it.id == currentSong?.id
        }
        if (currentIndex >= 0) {
            val upcoming = activeQueue.drop(currentIndex + 1).take(3)
            if (upcoming.isNotEmpty()) {
                lyricsViewModel.prefetchLyrics(upcoming)
            }
        }
    }

    // ✅ Update current lyric line
    LaunchedEffect(currentPosition) {
        lyricsViewModel.updateCurrentLine(currentPosition)
    }

    // ✅ Track if user is manually scrolling
    val isUserScrolling = remember { mutableStateOf(false) }

    // Detect user scroll interaction
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserScrolling.value = true

        }
    }

    // Resume auto scroll after 5 seconds of no interaction
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && isUserScrolling.value) {
            kotlinx.coroutines.delay(7000)
            isUserScrolling.value = false
        }
    }

    // ✅ Auto scroll ONLY if user is not scrolling
    LaunchedEffect(currentLineIndex) {
        if (lyricsState is LyricsState.SyncedLoaded &&
            showSynced &&
            !isUserScrolling.value
        ) {
            scope.launch {
                val scrollTo = (currentLineIndex - 1).coerceAtLeast(0)
                listState.animateScrollToItem(scrollTo)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        currentSong?.let { song ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // ── Top Bar ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "NOW PLAYING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = song.album.ifEmpty { "VibeUp" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    navController.navigate(
                                        Screen.AudioEffects.route
                                    )
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.Equalizer,
                                    contentDescription = "Equalizer",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // ── Album Art ──
                item {
                    AsyncImage(
                        model = song.imageUrl,
                        contentDescription = song.title,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .shadow(
                                elevation = 32.dp,
                                shape = RoundedCornerShape(24.dp),
                                clip = false
                            )
                            .clip(RoundedCornerShape(24.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // ── Song Info + Like ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 16.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.toggleLike()}) {
                            Icon(
                                imageVector = if (isLiked)
                                    Icons.Default.Favorite
                                else
                                    Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked)
                                    PinkAccent
                                else
                                    Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── Progress Bar ──
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Slider(
                            value = if (duration > 0)
                                currentPosition.toFloat() / duration.toFloat()
                            else 0f,
                            onValueChange = { progress ->
                                viewModel.seekTo((progress * duration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = PurplePrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(currentPosition),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = formatDuration(duration),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Controls ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ✅ Smart Shuffle button with 3 states
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = when {
                                        isSmartShuffle -> Color(0xFF10B981) // green = smart
                                        isShuffleEnabled -> PurplePrimary   // purple = normal
                                        else -> Color.White                  // white = off
                                    },
                                    modifier = Modifier.size(26.dp)
                                )
                                // ✅ Show mode label
                                if (isSmartShuffle || isShuffleEnabled) {
                                    Text(
                                        text = if (isSmartShuffle) "Smart" else "Shuffle",
                                        fontSize = 8.sp,
                                        color = if (isSmartShuffle)
                                            Color(0xFF10B981)
                                        else
                                            PurplePrimary
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.playPrevious() },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Surface(
                            onClick = { viewModel.togglePlayPause() },
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp),
                            color = Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
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
                                if (isResolvingUrl) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying)
                                            Icons.Default.Pause
                                        else
                                            Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.playNext() },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleRepeatMode() }
                        ) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    Player.REPEAT_MODE_ONE ->
                                        Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (repeatMode != Player.REPEAT_MODE_OFF)
                                    PurplePrimary
                                else
                                    Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                // ── Lyrics Section ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lyrics",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(PurpleLight, BlueLight)
                                )
                            )
                        )
                        // Toggle synced/full
                        if (lyricsState is LyricsState.SyncedLoaded) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF12122A))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("Sync" to true, "Full" to false)
                                    .forEach { (label, sync) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (showSynced == sync)
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                PurplePrimary,
                                                                BluePrimary
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
                                                .clickable {
                                                    if (showSynced != sync)
                                                        lyricsViewModel
                                                            .toggleDisplayMode()
                                                }
                                                .padding(
                                                    horizontal = 10.dp,
                                                    vertical = 4.dp
                                                )
                                        ) {
                                            Text(
                                                label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Lyrics Content ──
                when (val state = lyricsState) {
                    is LyricsState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = PurplePrimary,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "Loading lyrics...",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    is LyricsState.SyncedLoaded -> {
                        if (showSynced) {
                            // ✅ Synced lyrics with tap to seek
                            itemsIndexed(state.lines) { index, line ->
                                val isCurrentLine = index == currentLineIndex
                                val isPastLine = index < currentLineIndex

                                val textColor by animateColorAsState(
                                    targetValue = when {
                                        isCurrentLine -> Color.White
                                        isPastLine -> Color.White.copy(alpha = 0.35f)
                                        else -> Color.White.copy(alpha = 0.2f)
                                    },
                                    animationSpec = tween(300),
                                    label = "lyric_color_$index"
                                )

                                Text(
                                    text = line.text,
                                    fontSize = if (isCurrentLine) 22.sp else 18.sp,
                                    fontWeight = if (isCurrentLine)
                                        FontWeight.ExtraBold
                                    else
                                        FontWeight.Medium,
                                    color = textColor,
                                    lineHeight = 30.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                        .then(
                                            if (isCurrentLine)
                                                Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        PurplePrimary.copy(
                                                            alpha = 0.15f
                                                        )
                                                    )
                                                    .padding(
                                                        horizontal = 12.dp,
                                                        vertical = 6.dp
                                                    )
                                            else
                                                Modifier.padding(
                                                    horizontal = 4.dp,
                                                    vertical = 4.dp
                                                )
                                        )
                                        // ✅ Tap to seek!
                                        .clickable {
                                            viewModel.seekTo(line.timeMs)
                                        }
                                )
                            }
                        } else {
                            // Full lyrics view
                            item {
                                Text(
                                    text = state.lines.joinToString("\n") { it.text },
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    lineHeight = 28.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }

                    is LyricsState.PlainLoaded -> {
                        item {
                            Text(
                                text = state.lyrics,
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 28.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            )
                        }
                    }

                    is LyricsState.NotFound -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF12122A))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎵", fontSize = 32.sp)
                                    Text(
                                        "Lyrics not available",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "for ${song.title}",
                                        color = TextSecondary.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    is LyricsState.Instrumental -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF12122A))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🎸", fontSize = 32.sp)
                                    Text(
                                        "Instrumental Track",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    else -> {}
                }

                // Bottom padding
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("🎵", fontSize = 64.sp)
                    Text(
                        "No track selected",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Pick a song to start the vibe!",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Explore Music",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}