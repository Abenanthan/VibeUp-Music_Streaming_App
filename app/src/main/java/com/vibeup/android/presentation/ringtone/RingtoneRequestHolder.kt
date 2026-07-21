package com.vibeup.android.presentation.ringtone

import com.vibeup.android.domain.model.Song
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight in-memory handoff for the song whose "Set as Ringtone" menu was tapped.
 * Avoids serializing the full Song (with nested artist credits) through nav arguments.
 */
@Singleton
class RingtoneRequestHolder @Inject constructor() {
    var pendingSong: Song? = null
        private set

    fun set(song: Song) { pendingSong = song }
    fun consume(): Song? = pendingSong?.also { pendingSong = null }
}
