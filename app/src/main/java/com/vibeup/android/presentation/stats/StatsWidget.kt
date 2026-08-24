package com.vibeup.android.presentation.stats

import com.vibeup.android.ui.theme.AppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StatsWidget(
    onOpenStats: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        AppTheme.colors.card,
                        AppTheme.colors.surface
                    )
                )
            )
            .clickable { onOpenStats() }
    ) {
        // Decorative glow circles
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = (-20).dp, y = (-20).dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    androidx.compose.foundation.shape.CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.TopEnd)
                .offset(x = 20.dp, y = (-10).dp)
                .background(
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    androidx.compose.foundation.shape.CircleShape
                )
        )

        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                AppTheme.brandBrush
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = AppTheme.colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "Your Vibes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "See all",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (stats.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (stats.totalPlays == 0) {
                // Empty state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🎵", fontSize = 32.sp)
                    Column {
                        Text(
                            "No stats yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            "Play songs to build your listening history",
                            fontSize = 11.sp,
                            color = AppTheme.colors.textMuted
                        )
                    }
                }
            } else {
                // Big hours stat
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "%.1f".format(stats.totalHoursListened),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppTheme.colors.textPrimary,
                        lineHeight = 48.sp
                    )
                    Text(
                        "hrs",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    "${stats.totalPlays} songs played · ${stats.streak} day streak 🔥",
                    fontSize = 12.sp,
                    color = AppTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3 stat pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top song
                    StatPill(
                        modifier = Modifier.weight(1f),
                        emoji = "🏆",
                        label = "Top Song",
                        value = stats.topSongs.firstOrNull()?.title ?: "—",
                        gradient = listOf(
                            Color(0xFF4C1D95).copy(alpha = 0.5f),
                            Color(0xFF7C3AED).copy(alpha = 0.5f)
                        )
                    )
                    // Top artist
                    StatPill(
                        modifier = Modifier.weight(1f),
                        emoji = "🎤",
                        label = "Top Artist",
                        value = stats.topArtists.firstOrNull()?.artist ?: "—",
                        gradient = listOf(
                            Color(0xFF1E3A8A).copy(alpha = 0.5f),
                            Color(0xFF0E7490).copy(alpha = 0.5f)
                        )
                    )
                    // Peak hour
                    StatPill(
                        modifier = Modifier.weight(1f),
                        emoji = "⏰",
                        label = "Peak",
                        value = if (stats.peakHour >= 0)
                            viewModel.formatHour(stats.peakHour)
                        else "—",
                        gradient = listOf(
                            Color(0xFF065F46).copy(alpha = 0.5f),
                            Color(0xFF059669).copy(alpha = 0.5f)
                        )
                    )
                }

                // Mini weekly bar chart
                if (stats.weeklyPlays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "This week",
                        fontSize = 11.sp,
                        color = AppTheme.colors.textMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    val maxPlays = stats.weeklyPlays.maxOfOrNull { it.playCount } ?: 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        stats.weeklyPlays.forEach { day ->
                            val frac = day.playCount.toFloat() / maxPlays
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .fillMaxHeight(frac.coerceAtLeast(0.05f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        AppTheme.brandBrushVertical
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    value: String,
    gradient: List<Color>
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(colors = gradient))
            .padding(10.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 9.sp,
            color = AppTheme.colors.textSecondary
        )
        Text(
            value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}