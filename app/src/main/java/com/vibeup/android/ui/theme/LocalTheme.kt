package com.vibeup.android.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalVibeColors = staticCompositionLocalOf { VibeThemes.Obsidian }

// Convenience extension — use AppTheme.colors.primary anywhere
object AppTheme {
    val colors: VibeColorScheme
        @androidx.compose.runtime.Composable
        get() = LocalVibeColors.current
}