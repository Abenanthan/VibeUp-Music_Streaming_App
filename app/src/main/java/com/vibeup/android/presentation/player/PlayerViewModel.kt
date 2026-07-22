package com.vibeup.android.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.LibraryRepository
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
    val currentQueueId: StateFlow<String?> = playerManager.currentQueueId
    val isRestored = playerManager.isRestored

    private val _isResolvingUrl = MutableStateFlow(false)
    val isResolvingUrl: StateFlow<Boolean> = _isResolvingUrl.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()


    init {
        viewModelScope.launch {
            currentSong.collect { song ->
                _isLiked.value = if (song != null) {
                    try { libraryRepository.isLiked(song.id) } catch (e: Exception) { false }
                } else false
            }
        }
    }

    fun toggleLike() {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            try {
                if (_isLiked.value) {
                    libraryRepository.unlikeSong(song.id)
                    _isLiked.value = false
                } else {
                    libraryRepository.likeSong(song)
                    _isLiked.value = true
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerVM", "toggleLike failed: ${e.message}")
            }
        }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList(), queueId: String? = null) {
        viewModelScope.launch {
            _isResolvingUrl.value = true
            try {
                val playableSong = withContext(Dispatchers.IO) {
                    playSongUseCase(song)
                }
                val resolvedQueue = if (queue.isEmpty()) {
                    listOf(playableSong)
                } else {
                    queue.map { if (it.id == song.id) playableSong else it }
                }
                playerManager.playSong(playableSong, resolvedQueue, queueId)
            } catch (e: Exception) {
                android.util.Log.e("PlayerVM", "playSong failed: ${e.message}")
                // Last resort — play with original song, never crash
                try { playerManager.playSong(song, queue, queueId) } catch (_: Exception) {}
            } finally {
                _isResolvingUrl.value = false
            }
        }
    }

    fun togglePlayPause() {
        try { playerManager.togglePlayPause() } catch (e: Exception) {
            android.util.Log.e("PlayerVM", "togglePlayPause: ${e.message}")
        }
    }

    fun seekTo(position: Long) {
        try { playerManager.seekTo(position) } catch (e: Exception) {
            android.util.Log.e("PlayerVM", "seekTo: ${e.message}")
        }
    }

    fun playNext() {
        viewModelScope.launch {
            try {
                val player = playerManager.getExoPlayer()
                val active = playerManager.activeQueue.value
                val nextIndex = player.currentMediaItemIndex + 1

                if (nextIndex < active.size) {
                    val nextSong = active[nextIndex]

                    // Only hit the network when the next song has no URL yet (e.g. an
                    // unresolved item after session restore). If it's already playable
                    // — the common case, thanks to the next-song pre-fetch — skip
                    // instantly instead of blocking on a resolve every single time.
                    val needsResolve = nextSong.audioUrl.isBlank() &&
                        nextSong.language != "local" && nextSong.id.isNotBlank()

                    if (needsResolve) {
                        _isResolvingUrl.value = true
                        val playable = try {
                            withContext(Dispatchers.IO) { playSongUseCase(nextSong) }
                        } catch (e: Exception) {
                            android.util.Log.e("PlayerVM", "playNext resolve failed: ${e.message}")
                            nextSong
                        } finally {
                            _isResolvingUrl.value = false
                        }
                        if (playable.audioUrl != nextSong.audioUrl) {
                            try { playerManager.updateQueueItem(nextIndex, playable) }
                            catch (e: Exception) {
                                android.util.Log.e("PlayerVM", "updateQueueItem failed: ${e.message}")
                            }
                        }
                    }

                    try { player.seekToNextMediaItem() }
                    catch (e: Exception) {
                        android.util.Log.e("PlayerVM", "seekToNext failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerVM", "playNext crashed: ${e.message}")
            }
        }
    }

    fun playPrevious() {
        try { playerManager.playPrevious() } catch (e: Exception) {
            android.util.Log.e("PlayerVM", "playPrevious: ${e.message}")
        }
    }

    fun toggleShuffle() {
        try { playerManager.toggleShuffle() } catch (e: Exception) {
            android.util.Log.e("PlayerVM", "toggleShuffle: ${e.message}")
        }
    }

    fun toggleRepeatMode() {
        try { playerManager.toggleRepeatMode() } catch (e: Exception) {
            android.util.Log.e("PlayerVM", "toggleRepeatMode: ${e.message}")
        }
    }

    fun clearError() { _errorMessage.value = null }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = playerManager.moveQueueItem(fromIndex, toIndex)
    fun removeFromQueue(index: Int) = playerManager.removeFromQueue(index)
    fun jumpToQueueIndex(index: Int) = playerManager.jumpToQueueIndex(index)

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        playerManager.setPlaybackSpeed(speed)
    }

}