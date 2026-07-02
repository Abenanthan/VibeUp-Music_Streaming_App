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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun LyricsScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel = hiltViewModel(),
    lyricsViewModel: LyricsViewModel = activityViewModel()
) {
    val currentSong by playerViewModel.currentSong.collectAsState()
    val currentPosition by playerViewModel.currentPosition.collectAsState()
    val lyricsState by lyricsViewModel.lyricsState.collectAsState()
    val currentLineIndex by lyricsViewModel.currentLineIndex.collectAsState()
    val showSynced by lyricsViewModel.showSynced.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Load lyrics when song changes
    LaunchedEffect(currentSong) {
        currentSong?.let { lyricsViewModel.loadLyrics(it) }
    }

    // Update current line with position
    LaunchedEffect(currentPosition) {
        lyricsViewModel.updateCurrentLine(currentPosition)
    }

    // Auto scroll to current line
    LaunchedEffect(currentLineIndex) {
        if (lyricsState is LyricsState.SyncedLoaded && showSynced) {
            scope.launch {
                val index = (currentLineIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(index)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background album art blur effect
        currentSong?.let { song ->
            AsyncImage(
                model = song.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        Modifier.background(
                            Color.Black.copy(alpha = 0.8f)
                        )
                    ),
                contentScale = ContentScale.Crop,
                alpha = 0.15f
            )
        }

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
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
                        text = currentSong?.title ?: "Lyrics",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = currentSong?.artist ?: "",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }

                // Toggle synced/plain
                if (lyricsState is LyricsState.SyncedLoaded) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("Sync" to true, "Full" to false).forEach { (label, sync) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (showSynced == sync)
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.tertiary
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
                                        if (showSynced != sync) {
                                            lyricsViewModel.toggleDisplayMode()
                                        }
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

            // ── Lyrics Content ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                when (val state = lyricsState) {
                    is LyricsState.Idle,
                    is LyricsState.Loading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Fetching lyrics...",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }

                    is LyricsState.SyncedLoaded -> {
                        if (showSynced) {
                            // ✅ Synced karaoke mode
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(
                                    top = 80.dp,
                                    bottom = 200.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(state.lines) { index, line ->
                                    val isCurrentLine = index == currentLineIndex
                                    val isPastLine = index < currentLineIndex

                                    val textColor by animateColorAsState(
                                        targetValue = when {
                                            isCurrentLine -> Color.White
                                            isPastLine -> Color.White.copy(alpha = 0.4f)
                                            else -> Color.White.copy(alpha = 0.25f)
                                        },
                                        animationSpec = tween(300),
                                        label = "lyric_color"
                                    )

                                    val fontSize by remember(isCurrentLine) {
                                        derivedStateOf {
                                            if (isCurrentLine) 22.sp else 18.sp
                                        }
                                    }

                                    Text(
                                        text = line.text,
                                        fontSize = fontSize,
                                        fontWeight = if (isCurrentLine)
                                            FontWeight.ExtraBold
                                        else
                                            FontWeight.Medium,
                                        color = textColor,
                                        textAlign = TextAlign.Start,
                                        lineHeight = 32.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (isCurrentLine) Modifier.background(
                                                    Color.White.copy(alpha = 0.05f),
                                                    RoundedCornerShape(8.dp)
                                                ).padding(
                                                    horizontal = 8.dp,
                                                    vertical = 4.dp
                                                )
                                                else Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                    )
                                }
                            }
                        } else {
                            // Full plain lyrics view
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    top = 16.dp,
                                    bottom = 200.dp
                                )
                            ) {
                                item {
                                    Text(
                                        text = state.lines.joinToString("\n") { it.text },
                                        fontSize = 16.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        lineHeight = 28.sp,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }

                    is LyricsState.PlainLoaded -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                top = 16.dp,
                                bottom = 200.dp
                            )
                        ) {
                            item {
                                Text(
                                    text = state.lyrics,
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    lineHeight = 28.sp,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }

                    is LyricsState.Instrumental -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🎸", fontSize = 56.sp)
                            Text(
                                "Instrumental Track",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "No lyrics for this song",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    is LyricsState.NotFound -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                "Lyrics Not Found",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Lyrics unavailable for\n${currentSong?.title}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is LyricsState.Error -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("😔", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Failed to load lyrics",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    currentSong?.let {
                                        lyricsViewModel.resetLyrics()
                                        lyricsViewModel.loadLyrics(it)
                                    }
                                }
                            ) {
                                Text("Retry", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
