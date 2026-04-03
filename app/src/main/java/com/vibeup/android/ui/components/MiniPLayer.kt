package com.vibeup.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vibeup.android.domain.model.Song
import com.vibeup.android.ui.theme.DarkCard
import com.vibeup.android.ui.theme.TextSecondary
import com.vibeup.android.ui.theme.VibeUpGreen

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard)
    ) {
        // Progress Bar
        if (duration > 0) {
            LinearProgressIndicator(
                progress = { (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = VibeUpGreen,
                trackColor = DarkCard
            )
        }

        // Player Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpand() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            AsyncImage(
                model = song.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Song Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Previous Button
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            // Play/Pause Button
            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying)
                        Icons.Default.Pause
                    else
                        Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Next Button
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}