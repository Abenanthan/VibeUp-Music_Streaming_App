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
import com.vibeup.android.domain.repository.LibraryRepository

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val playSongUseCase: PlaySongUseCase,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val queue: StateFlow<List<Song>> = playerManager.queue
    val activeQueue: StateFlow<List<Song>> = playerManager.activeQueue
    val isShuffleEnabled: StateFlow<Boolean> = playerManager.isShuffleEnabled
    val isSmartShuffle: StateFlow<Boolean> = playerManager.isSmartShuffle
    val repeatMode: StateFlow<Int> = playerManager.repeatMode
    val isRestored = playerManager.isRestored

    // ✅ Track loading state
    private val _isResolvingUrl = MutableStateFlow(false)
    val isResolvingUrl: StateFlow<Boolean> = _isResolvingUrl.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    init {
        // Watch current song and check liked status whenever it changes
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    _isLiked.value = libraryRepository.isLiked(song.id)
                } else {
                    _isLiked.value = false
                }
            }
        }
    }

    fun toggleLike() {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            if (_isLiked.value) {
                libraryRepository.unlikeSong(song.id)
                _isLiked.value = false
            } else {
                libraryRepository.likeSong(song)
                _isLiked.value = true
            }
        }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        viewModelScope.launch {
            _isResolvingUrl.value = true
            try {
                // ✅ Resolve playable URL for clicked song
                val playableSong = withContext(Dispatchers.IO) {
                    playSongUseCase(song)
                }

                // ✅ Also resolve URLs for queue items
                val resolvedQueue = if (queue.isEmpty()) {
                    listOf(playableSong)
                } else {
                    queue.map { queueSong ->
                        if (queueSong.id == song.id) playableSong
                        else queueSong
                    }
                }

                playerManager.playSong(playableSong, resolvedQueue)
            } catch (e: Exception) {
                android.util.Log.e("PlayerVM", "PlaySong error: ${e.message}")
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
            val active = playerManager.activeQueue.value

            if (nextIndex < active.size) {
                val nextSong = active[nextIndex]
                // ✅ Resolve URL for next song before playing
                val playable = withContext(Dispatchers.IO) {
                    playSongUseCase(nextSong)
                }
                playerManager.updateQueueItem(nextIndex, playable)
                player.seekToNextMediaItem()
            }
        }
    }



    fun playPrevious() = playerManager.playPrevious()

    fun toggleShuffle() = playerManager.toggleShuffle()

    fun toggleRepeatMode() = playerManager.toggleRepeatMode()
}
