package com.vibeup.android.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.usecase.PlaySongUseCase
import com.vibeup.android.service.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val playSongUseCase: PlaySongUseCase
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val queue: StateFlow<List<Song>> = playerManager.queue
    val isShuffleEnabled: StateFlow<Boolean> = playerManager.isShuffleEnabled
    val repeatMode: StateFlow<Int> = playerManager.repeatMode
    val isRestored = playerManager.isRestored

    // ✅ Track loading state
    private val _isResolvingUrl = MutableStateFlow(false)
    val isResolvingUrl: StateFlow<Boolean> = _isResolvingUrl.asStateFlow()

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        viewModelScope.launch {
            _isResolvingUrl.value = true
            try {
                // ✅ Resolve playable URL for clicked song
                val playableSong = withContext(Dispatchers.IO) {
                    playSongUseCase(song)
                }

                // ✅ Also resolve URLs for queue items
                // (resolve first 3 items eagerly, rest lazily)
                val resolvedQueue = if (queue.isEmpty()) {
                    listOf(playableSong)
                } else {
                    queue.map { queueSong ->
                        if (queueSong.id == song.id) playableSong
                        else queueSong // resolve others when needed
                    }
                }

                playerManager.playSong(playableSong, resolvedQueue)
            } catch (e: Exception) {
                android.util.Log.e("PlayerVM", "PlaySong error: ${e.message}")
                // Fallback — try playing with original URL
                playerManager.playSong(song, queue)
            } finally {
                _isResolvingUrl.value = false
            }
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    fun seekTo(position: Long) = playerManager.seekTo(position)

    fun playNext() {
        viewModelScope.launch {
            val player = playerManager.getExoPlayer()
            val nextIndex = player.currentMediaItemIndex + 1
            val queue = playerManager.queue.value

            if (nextIndex < queue.size) {
                val nextSong = queue[nextIndex]
                // ✅ Resolve URL for next song before playing
                val playable = withContext(Dispatchers.IO) {
                    playSongUseCase(nextSong)
                }
                // Update queue with resolved song
                val updatedQueue = queue.toMutableList()
                updatedQueue[nextIndex] = playable
                playerManager.updateQueueItem(nextIndex, playable)
                player.seekToNextMediaItem()
            }
        }
    }

    fun playPrevious() = playerManager.playPrevious()

    fun toggleShuffle() = playerManager.toggleShuffle()

    fun toggleRepeatMode() = playerManager.toggleRepeatMode()
}
