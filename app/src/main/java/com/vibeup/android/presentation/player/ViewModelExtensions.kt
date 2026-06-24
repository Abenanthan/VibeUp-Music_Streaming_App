package com.vibeup.android.presentation.player

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
inline fun <reified VM : ViewModel> activityViewModel(): VM {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
        ?: throw IllegalStateException("LocalContext is not a ComponentActivity")
    return hiltViewModel(activity)
}
