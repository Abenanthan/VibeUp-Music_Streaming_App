package com.vibeup.android.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.ui.theme.DarkBackground
import com.vibeup.android.ui.theme.TextSecondary
import com.vibeup.android.ui.theme.VibeUpGreen

@Composable
fun PlayerScreen(
    navController: NavController,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    var isLiked by remember { mutableStateOf(false) }
    var isShuffled by remember { mutableStateOf(false) }
    var isRepeating by remember { mutableStateOf(false) }

    currentSong?.let { song ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            DarkBackground
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Now Playing",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = song.album.ifEmpty { "VibeUp" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = { isLiked = !isLiked }) {
                        Icon(
                            imageVector = if (isLiked)
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) VibeUpGreen else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Album Art
                AsyncImage(
                    model = song.imageUrl,
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(300.dp)
                        .shadow(
                            elevation = 32.dp,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Song Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.artist,
                            fontSize = 16.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = if (duration > 0)
                            currentPosition.toFloat() / duration.toFloat()
                        else 0f,
                        onValueChange = { progress ->
                            viewModel.seekTo((progress * duration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = VibeUpGreen,
                            activeTrackColor = VibeUpGreen,
                            inactiveTrackColor = Color.Gray
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

                // Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { isShuffled = !isShuffled }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffled) VibeUpGreen else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = { viewModel.playPrevious() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play/Pause
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = VibeUpGreen,
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            imageVector = if (isPlaying)
                                Icons.Default.Pause
                            else
                                Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = { viewModel.playNext() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Repeat
                    IconButton(onClick = { isRepeating = !isRepeating }) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeating) VibeUpGreen else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    } ?: run {
        // No song playing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🎵", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No song playing",
                    color = TextSecondary,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Go Back")
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