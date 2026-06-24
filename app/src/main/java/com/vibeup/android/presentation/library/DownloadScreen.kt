package com.vibeup.android.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vibeup.android.data.local.entity.DownloadedSong
import com.vibeup.android.data.repository.DownloadStatus
import com.vibeup.android.presentation.player.PlayerViewModel
import com.vibeup.android.data.repository.DownloadRepository
import com.vibeup.android.ui.theme.*

@Composable
fun DownloadsScreen(
    navController: NavController,
    viewModel: DownloadsViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val downloads by viewModel.downloads.collectAsState(initial = emptyList())
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val message by viewModel.message.collectAsState()
    val totalSize by viewModel.totalSize.collectAsState()

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF12122A), CircleShape)
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
                        "Downloads",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PurpleLight, BlueLight)
                            )
                        )
                    )
                    Text(
                        "${downloads.size} songs • $totalSize",
                        fontSize = 11.sp,
                        color = Color(0xFF4B5563)
                    )
                }
            }

            if (downloads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Color(0xFF2A2A4A),
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            "No downloads yet!",
                            color = Color(0xFF4B5563),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Download songs to play offline",
                            color = Color(0xFF374151),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = downloads,
                        key = { it.id }
                    ) { song ->
                        val progress = downloadProgress[song.id]

                        DownloadedSongItem(
                            song = song,
                            progress = progress?.progress ?: 100,
                            status = progress?.status,
                            context = context,
                            onPlay = {
                                val songModel = viewModel.toSong(song)
                                playerViewModel.playSong(
                                    songModel,
                                    downloads.map { viewModel.toSong(it) }
                                )
                            },
                            onDelete = { viewModel.deleteDownload(song.id) },
                            formatSize = { size ->
                                DownloadRepository.formatSizeStatic(size)
                            }
                        )
                    }
                }
            }
        }

        // Snackbar
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

@Composable
fun DownloadedSongItem(
    song: DownloadedSong,
    progress: Int,
    status: DownloadStatus?,
    context: android.content.Context,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    formatSize: (Long) -> String
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
        // Album Art
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )

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
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quality badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PurplePrimary.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        song.quality,
                        fontSize = 9.sp,
                        color = PurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = formatSize(song.fileSize),
                    fontSize = 10.sp,
                    color = Color(0xFF4B5563)
                )
            }

            // Progress bar if downloading
            if (status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = PurplePrimary,
                    trackColor = Color(0xFF2A2A4A)
                )
            }
        }

        // Offline badge
        Icon(
            Icons.Default.DownloadDone,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(18.dp)
        )

        // Three dots menu
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
                            "▶️ Play",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showMenu = false; onPlay() }
                )
                HorizontalDivider(
                    color = Color(0xFF2A2A4A),
                    thickness = 0.5.dp
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            "🗑️ Delete",
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp
                        )
                    },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}