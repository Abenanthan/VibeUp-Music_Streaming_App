package com.vibeup.android.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.domain.model.Song
import com.vibeup.android.service.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playerManager.currentSong
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentPosition: StateFlow<Long> = playerManager.currentPosition
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val duration: StateFlow<Long> = playerManager.duration
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val queue: StateFlow<List<Song>> = playerManager.queue
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        playerManager.playSong(song, queue)
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(position: Long) {
        playerManager.seekTo(position)
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }
}