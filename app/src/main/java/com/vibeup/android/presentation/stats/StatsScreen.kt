package com.vibeup.android.presentation.stats

import com.vibeup.android.ui.theme.AppTheme

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import kotlin.math.roundToInt

@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Animated gradient offset.
    // Kept as a State (not read here) — see the drawBehind at the usage site.
    // Reading `.value` at this scope invalidated the whole StatsScreen at display
    // refresh rate, permanently, for a decorative background.
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animOffsetState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (stats.isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Loading your stats...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // ── Hero Header ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    // Animated gradient background.
                    // The animation value is read inside drawBehind so each frame
                    // only repaints this Box — it no longer recomposes the screen.
                    val gradientColors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                        AppTheme.colors.background
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = gradientColors,
                                        radius = 600f + animOffsetState.value / 5
                                    )
                                )
                            }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back + Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        AppTheme.colors.textPrimary.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    tint = AppTheme.colors.textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Your Vibes 🎵",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppTheme.colors.textPrimary
                            )
                        }

                        // Big stats
                        Column {
                            Text(
                                text = "%.1f".format(stats.totalHoursListened),
                                fontSize = 72.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppTheme.colors.textPrimary,
                                lineHeight = 72.sp
                            )
                            Text(
                                "hours of music 🎵",
                                fontSize = 16.sp,
                                color = AppTheme.colors.textPrimary.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${stats.totalPlays} songs played",
                                fontSize = 14.sp,
                                color = AppTheme.colors.textPrimary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Quick Stats Row ──
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        QuickStatCard(
                            emoji = "🔥",
                            value = "${stats.streak}",
                            label = "Day Streak",
                            gradient = listOf(
                                Color(0xFFBE185D),
                                Color(0xFFF97316)
                            )
                        )
                    }
                    item {
                        QuickStatCard(
                            emoji = "🌙",
                            value = if (stats.peakHour >= 0)
                                viewModel.formatHour(stats.peakHour)
                            else "—",
                            label = "Peak Hour",
                            gradient = listOf(
                                Color(0xFF4C1D95),
                                Color(0xFF1E3A8A)
                            )
                        )
                    }
                    item {
                        QuickStatCard(
                            emoji = "💿",
                            value = if (stats.topAlbum.isNotEmpty())
                                stats.topAlbum.take(8) + "…"
                            else "—",
                            label = "Top Album",
                            gradient = listOf(
                                Color(0xFF065F46),
                                Color(0xFF0E7490)
                            )
                        )
                    }
                }

                // ── Weekly Chart ──
                if (stats.weeklyPlays.isNotEmpty()) {
                    StatCard(title = "📊 This Week") {
                        WeeklyChart(
                            plays = stats.weeklyPlays,
                            viewModel = viewModel
                        )
                    }
                }

                // ── Top Songs ──
                if (stats.topSongs.isNotEmpty()) {
                    StatCard(title = "🏆 Top Songs") {
                        stats.topSongs.forEachIndexed { index, song ->
                            TopSongRow(
                                rank = index + 1,
                                song = song,
                                context = context,
                                maxPlays = stats.topSongs.firstOrNull()
                                    ?.playCount ?: 1
                            )
                            if (index < stats.topSongs.size - 1) {
                                HorizontalDivider(
                                    color = AppTheme.divider,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(
                                        vertical = 6.dp
                                    )
                                )
                            }
                        }
                    }
                }

                // ── Top Artists ──
                if (stats.topArtists.isNotEmpty()) {
                    StatCard(title = "🎤 Top Artists") {
                        stats.topArtists.forEachIndexed { index, artist ->
                            TopArtistRow(
                                rank = index + 1,
                                artist = artist.artist,
                                plays = artist.playCount,
                                maxPlays = stats.topArtists.firstOrNull()
                                    ?.playCount ?: 1
                            )
                            if (index < stats.topArtists.size - 1) {
                                HorizontalDivider(
                                    color = AppTheme.divider,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(
                                        vertical = 6.dp
                                    )
                                )
                            }
                        }
                    }
                }

                // ── Empty State ──
                if (stats.totalPlays == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🎵", fontSize = 56.sp)
                            Text(
                                "No stats yet!",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Start playing music to\nbuild your listening stats",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

// ── Quick Stat Card ──
@Composable
fun QuickStatCard(
    emoji: String,
    value: String,
    label: String,
    gradient: List<Color>
) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors = gradient))
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(emoji, fontSize = 20.sp)
            Column {
                Text(
                    value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    label,
                    fontSize = 10.sp,
                    color = AppTheme.colors.textPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── Stat Card Container ──
@Composable
fun StatCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface)
            .padding(16.dp)
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        content()
    }
}

// ── Weekly Chart ──
@Composable
fun WeeklyChart(
    plays: List<com.vibeup.android.data.local.dao.DayPlayCount>,
    viewModel: StatsViewModel
) {
    val maxPlays = plays.maxOfOrNull { it.playCount } ?: 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        plays.forEach { day ->
            val heightFraction = day.playCount.toFloat() / maxPlays
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "${day.playCount}",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height((heightFraction * 60).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            AppTheme.brandBrushVertical
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    viewModel.getDayLabel(day.dayOfYear, day.year),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Top Song Row ──
@Composable
fun TopSongRow(
    rank: Int,
    song: com.vibeup.android.data.local.dao.SongPlayCount,
    context: android.content.Context,
    maxPlays: Int
) {
    val rankColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFC0C0C0), // Silver
        Color(0xFFCD7F32)  // Bronze
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Rank
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    if (rank <= 3)
                        rankColors[rank - 1].copy(alpha = 0.2f)
                    else
                        AppTheme.colors.card,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$rank",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) rankColors[rank - 1]
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Album art
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Play count bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.colors.card)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            song.playCount.toFloat() / maxPlays
                        )
                        .fillMaxHeight()
                        .background(
                            AppTheme.brandBrush
                        )
                )
            }
        }

        // Play count badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "${song.playCount}×",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Top Artist Row ──
@Composable
fun TopArtistRow(
    rank: Int,
    artist: String,
    plays: Int,
    maxPlays: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "#$rank",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp)
        )

        // Artist avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    AppTheme.brandBrush,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                artist.first().uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                artist,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.colors.card)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(plays.toFloat() / maxPlays)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "$plays×",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
