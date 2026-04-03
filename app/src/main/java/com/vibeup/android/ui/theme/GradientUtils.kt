package com.vibeup.android.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Gradients {
    val PurpleBlue = Brush.horizontalGradient(
        colors = listOf(GradientStart, GradientEnd)
    )

    val PurplePink = Brush.horizontalGradient(
        colors = listOf(PurplePrimary, GradientPink)
    )

    val BluePurpleVertical = Brush.verticalGradient(
        colors = listOf(
            DeepDark,
            DarkSurface
        )
    )

    val CardGradient = Brush.linearGradient(
        colors = listOf(DarkCard, DarkElevated)
    )

    val PlayerBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A0A3A),
            Color(0xFF0A0A1A)
        )
    )

    val SplashBackground = Brush.radialGradient(
        colors = listOf(
            Color(0xFF1A0A3A),
            Color(0xFF0A0A1A)
        )
    )
}