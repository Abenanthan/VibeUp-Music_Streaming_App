package com.vibeup.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

    // Material3 needs the correct light/dark base: it drives defaults we don't set
    // explicitly (ripples, disabled states, scrim, outline, elevation tints). Using
    // darkColorScheme for a light palette leaves those defaults wrong.
    val materialColors = if (colors.isLight) {
        lightColorScheme(
            primary          = colors.primary,
            secondary        = colors.secondary,
            tertiary         = colors.accent,
            background       = colors.background,
            surface          = colors.surface,
            surfaceVariant   = colors.card,
            onPrimary        = colors.onAccent,
            onSecondary      = colors.onAccent,
            onTertiary       = colors.onAccent,
            onBackground     = colors.textPrimary,
            onSurface        = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary,
            outline          = colors.textMuted,
            error            = Color(0xFFB3261E)
        )
    } else {
        darkColorScheme(
            primary          = colors.primary,
            secondary        = colors.secondary,
            tertiary         = colors.accent,
            background       = colors.background,
            surface          = colors.surface,
            surfaceVariant   = colors.card,
            onPrimary        = colors.onAccent,
            onSecondary      = colors.onAccent,
            onTertiary       = colors.onAccent,
            onBackground     = colors.textPrimary,
            onSurface        = colors.textPrimary,
            onSurfaceVariant = colors.textSecondary,
            outline          = colors.textMuted,
            error            = Color(0xFFFF5252)
        )
    }

    // Keep the system status/navigation bars in step with the selected theme.
    // This lives here rather than in MainActivity.onCreate so it re-applies when the
    // user switches theme at runtime — a light theme needs dark bar icons, a dark
    // theme needs light ones.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colors.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colors.navBar.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = colors.isLight
                isAppearanceLightNavigationBars = colors.isLight
            }
        }
    }

    CompositionLocalProvider(LocalVibeColors provides colors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}
