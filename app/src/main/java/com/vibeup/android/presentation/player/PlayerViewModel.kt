package com.vibeup.android.presentation.player

import androidx.lifecycle.ViewModel
import com.vibeup.android.domain.model.Song
import com.vibeup.android.service.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val queue: StateFlow<List<Song>> = playerManager.queue

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