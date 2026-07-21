package com.vibeup.android.presentation.ringtone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.vibeup.android.Screen
import com.vibeup.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Tiny VM whose only job is to stash the chosen song into the singleton holder. */
@HiltViewModel
class RingtoneLaunchViewModel @Inject constructor(
    private val holder: RingtoneRequestHolder
) : ViewModel() {
    fun stash(song: Song) = holder.set(song)
}

/**
 * Returns a `(Song) -> Unit` that stashes the song and navigates to the ringtone
 * trimmer. Call once per screen and reuse for every menu item.
 */
@Composable
fun rememberRingtoneLauncher(navController: NavController): (Song) -> Unit {
    val vm: RingtoneLaunchViewModel = hiltViewModel()
    return remember(navController) {
        { song: Song ->
            vm.stash(song)
            navController.navigate(Screen.Ringtone.route)
        }
    }
}
