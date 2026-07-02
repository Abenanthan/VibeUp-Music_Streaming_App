package com.vibeup.android.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vibeup.android.ui.theme.*

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val pendingTheme by viewModel.pendingTheme.collectAsState()
    var applied by remember { mutableStateOf(false) }

    val activeColors = LocalVibeColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(activeColors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(activeColors.card, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = activeColors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            "Appearance",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeColors.textPrimary
                        )
                        Text(
                            "Choose your visual style",
                            fontSize = 12.sp,
                            color = activeColors.textMuted
                        )
                    }
                }
            }

            // Section label
            item {
                Text(
                    "THEMES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeColors.textMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(
                        start = 20.dp, bottom = 12.dp, top = 8.dp
                    )
                )
            }

            // Theme cards
            items(VibeTheme.values().toList()) { theme ->
                val colors = VibeThemes.get(theme)
                val isSelected = pendingTheme == theme
                val isActive = currentTheme == theme

                ThemeCard(
                    theme = theme,
                    colors = colors,
                    isSelected = isSelected,
                    isActive = isActive,
                    onClick = {
                        viewModel.selectTheme(theme)
                        applied = false
                    }
                )
            }

            // Apply button
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Button(
                        onClick = {
                            viewModel.applyTheme()
                            applied = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp),
                        enabled = viewModel.hasUnappliedChanges() && !applied
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (viewModel.hasUnappliedChanges() && !applied)
                                        Brush.linearGradient(
                                            colors = listOf(
                                                activeColors.gradientStart,
                                                activeColors.gradientEnd
                                            )
                                        )
                                    else
                                        Brush.linearGradient(
                                            colors = listOf(
                                                activeColors.card,
                                                activeColors.card
                                            )
                                        ),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (applied) "✓ Theme Applied"
                                else "Apply Theme",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.hasUnappliedChanges() && !applied)
                                    Color.White
                                else
                                    activeColors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: VibeTheme,
    colors: VibeColorScheme,
    isSelected: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val activeAppColors = LocalVibeColors.current

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                brush = Brush.linearGradient(
                    colors = listOf(colors.gradientStart, colors.gradientEnd)
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .background(activeAppColors.card)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme preview swatch
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.background)
            ) {
                // Simulated mini UI preview
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Fake top bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.surface)
                    )
                    // Fake card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.card)
                    )
                    // Gradient accent bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        colors.gradientStart,
                                        colors.gradientEnd
                                    )
                                )
                            )
                    )
                    // Color dots row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        listOf(
                            colors.primary,
                            colors.secondary,
                            colors.accent
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, CircleShape)
                            )
                        }
                    }
                }
            }

            // Theme info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        theme.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeAppColors.textPrimary
                    )
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Active",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                    }
                }
                Text(
                    theme.description,
                    fontSize = 11.sp,
                    color = activeAppColors.textMuted,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            // Selection indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isSelected)
                            Brush.linearGradient(
                                colors = listOf(
                                    colors.gradientStart,
                                    colors.gradientEnd
                                )
                            )
                        else
                            Brush.linearGradient(
                                colors = listOf(
                                    activeAppColors.elevated,
                                    activeAppColors.elevated
                                )
                            ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}