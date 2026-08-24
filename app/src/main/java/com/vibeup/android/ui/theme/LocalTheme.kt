package com.vibeup.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor

val LocalVibeColors = staticCompositionLocalOf { VibeThemes.Obsidian }

// Convenience extension — use AppTheme.colors.primary anywhere
object AppTheme {
    val colors: VibeColorScheme
        @Composable
        get() = LocalVibeColors.current

    /**
     * The brand fill for buttons, FABs, selected chips and gradient titles.
     *
     * Dark themes get the two-stop gradient they were designed around. Light themes
     * get a FLAT primary: YouTube Music and Apple Music both use solid colour in
     * light mode, and a saturated gradient on white reads as garish rather than
     * premium. Using this instead of building a Brush at the call site keeps
     * `accent` free to stay a distinct colour (e.g. YT Music's red now-playing
     * indicator) rather than being forced to equal `primary` just to flatten.
     */
    val brandBrush: Brush
        @Composable
        get() = with(LocalVibeColors.current) {
            if (isLight) SolidColor(primary)
            else Brush.horizontalGradient(listOf(gradientStart, gradientEnd))
        }

    /** Vertical variant, for bars and tiles that read top-to-bottom. */
    val brandBrushVertical: Brush
        @Composable
        get() = with(LocalVibeColors.current) {
            if (isLight) SolidColor(primary)
            else Brush.verticalGradient(listOf(gradientStart, gradientEnd))
        }

    /**
     * Fill for "selected" chips/rows. Dark themes tint with a translucent brand
     * wash; on white a 10-20% tint is nearly invisible, so light themes use a
     * stronger, opaque-enough tint that still reads as a selection.
     */
    val selectedFill: androidx.compose.ui.graphics.Color
        @Composable
        get() = with(LocalVibeColors.current) {
            if (isLight) primary.copy(alpha = 0.12f) else primary.copy(alpha = 0.18f)
        }

    /** A divider that is actually visible on both light and dark backgrounds. */
    val divider: androidx.compose.ui.graphics.Color
        @Composable
        get() = with(LocalVibeColors.current) {
            if (isLight) elevated else textPrimary.copy(alpha = 0.10f)
        }
}
