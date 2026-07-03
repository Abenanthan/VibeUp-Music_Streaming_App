package com.vibeup.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun VibeUpTheme(
    themeManager: ThemeManager? = null,
    content: @Composable () -> Unit
) {
    val theme by themeManager?.currentTheme?.collectAsState()
        ?: androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(VibeTheme.OBSIDIAN)
        }

    val colors = VibeThemes.get(theme)

    val materialColors = darkColorScheme(
        primary          = colors.primary,
        secondary        = colors.secondary,
        tertiary         = colors.accent,
        background       = colors.background,
        surface          = colors.surface,
        surfaceVariant   = colors.card,
        onPrimary        = Color.White,
        onSecondary      = Color.White,
        onBackground     = colors.textPrimary,
        onSurface        = colors.textPrimary,
        onSurfaceVariant = colors.textSecondary,
        error            = Color(0xFFFF5252)
    )

    CompositionLocalProvider(LocalVibeColors provides colors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}