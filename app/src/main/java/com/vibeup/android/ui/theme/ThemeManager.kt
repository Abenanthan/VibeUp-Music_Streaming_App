package com.vibeup.android.ui.theme

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vibe_theme", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(loadSavedTheme())
    val currentTheme: StateFlow<VibeTheme> = _currentTheme.asStateFlow()

    private fun loadSavedTheme(): VibeTheme {
        val saved = prefs.getString("theme", VibeTheme.CLASSIC_PURPLE.name)  // 👈 CHANGED
        return try {
            VibeTheme.valueOf(saved ?: VibeTheme.CLASSIC_PURPLE.name)         // 👈 CHANGED
        } catch (e: Exception) {
            VibeTheme.CLASSIC_PURPLE                                           // 👈 CHANGED
        }
    }

    fun applyTheme(theme: VibeTheme) {
        _currentTheme.value = theme
        prefs.edit().putString("theme", theme.name).apply()
    }

    val colors: VibeColorScheme
        get() = VibeThemes.get(_currentTheme.value)
}