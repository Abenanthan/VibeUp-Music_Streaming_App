package com.vibeup.android.service

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.LibraryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Random

@OptIn(UnstableApi::class)
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository
) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    // Dummy isRestored — always false now
    val isRestored = MutableStateFlow(false)

    fun getExoPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            _isPlaying.value = playing
                            if (playing) startTracking()
                            else stopTracking()
                        }

                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                _duration.value = duration
                            }
                        }

                        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                            _isShuffleEnabled.value = shuffleModeEnabled
                        }

                        override fun onRepeatModeChanged(repeatMode: Int) {
                            _repeatMode.value = repeatMode
                        }//cmt

                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int
                        ) {
                            val index =
                                exoPlayer?.currentMediaItemIndex ?: 0
                            if (_queue.value.isNotEmpty() &&
                                index < _queue.value.size
                            ) {
                                val newSong = _queue.value[index]
                                _currentSong.value = newSong
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        libraryRepository
                                            .addToRecentlyPlayed(newSong)
                                    } catch (e: Exception) { }
                                }
                            }
                        }
                    })
                }
        }
        return exoPlayer!!
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        if (queue.isNotEmpty()) _queue.value = queue
        _currentSong.value = song

        // Start service
        try {
            context.startForegroundService(
                Intent(context, MusicPlayerService::class.java)
            )
        } catch (e: Exception) { }

        val player = getExoPlayer()
        val items = _queue.value.map { s ->
            MediaItem.Builder()
                .setUri(s.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setArtworkUri(s.imageUrl.toUri())
                        .build()
                )
                .build()
        }
        val index = _queue.value.indexOf(song).coerceAtLeast(0)
        
        player.setMediaItems(items, index, 0L)
        
        // Reset shuffle order with a new random seed whenever a new queue is set
        if (player.shuffleModeEnabled) {
            player.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(items.size, Random().nextLong()))
        }
        
        player.prepare()
        player.play()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                libraryRepository.addToRecentlyPlayed(song)
            } catch (e: Exception) { }
        }
    }

    fun togglePlayPause() {
        val player = getExoPlayer()
        if (player.isPlaying) player.pause()
        else player.play()
    }

    fun seekTo(position: Long) {
        getExoPlayer().seekTo(position)
    }

    fun playNext() {
        val player = getExoPlayer()
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun playPrevious() {
        val player = getExoPlayer()
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val player = getExoPlayer()
        val enabled = !player.shuffleModeEnabled
        player.shuffleModeEnabled = enabled
        
        // Force a new random shuffle order when enabled
        if (enabled) {
            player.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(player.mediaItemCount, Random().nextLong()))
        }
    }
    
    fun setShuffleEnabled(enabled: Boolean) {
        val player = getExoPlayer()
        player.shuffleModeEnabled = enabled
        if (enabled) {
            player.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(player.mediaItemCount, Random().nextLong()))
        }
    }

    fun toggleRepeatMode() {
        val player = getExoPlayer()
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
    }

    private fun startTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                exoPlayer?.let {
                    _currentPosition.value = it.currentPosition
                    _duration.value = it.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    private fun stopTracking() {
        progressJob?.cancel()
    }

    fun resetState() {
        android.util.Log.d("PlayerManager", "Resetting all state!")
        progressJob?.cancel()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        exoPlayer?.release()
        exoPlayer = null  // ← force new instance next time
        _currentSong.value = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        _queue.value = emptyList()
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}