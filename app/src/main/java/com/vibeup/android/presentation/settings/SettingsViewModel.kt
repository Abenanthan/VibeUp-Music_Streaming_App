package com.vibeup.android.presentation.settings

import androidx.lifecycle.ViewModel
import com.vibeup.android.ui.theme.ThemeManager
import com.vibeup.android.ui.theme.VibeTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager
) : ViewModel() {

    val currentTheme: StateFlow<VibeTheme> = themeManager.currentTheme

    private val _pendingTheme = MutableStateFlow(themeManager.currentTheme.value)
    val pendingTheme: StateFlow<VibeTheme> = _pendingTheme.asStateFlow()

    fun selectTheme(theme: VibeTheme) {
        _pendingTheme.value = theme
    }

    fun applyTheme() {
        themeManager.applyTheme(_pendingTheme.value)
    }

    fun hasUnappliedChanges(): Boolean =
        _pendingTheme.value != currentTheme.value
}