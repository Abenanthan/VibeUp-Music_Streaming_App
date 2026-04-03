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
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isPlaying.value = isPlaying
                            if (isPlaying) startProgressTracking()
                            else stopProgressTracking()
                        }

                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                _duration.value = duration
                            }
                        }

                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int
                        ) {
                            val currentIndex = exoPlayer?.currentMediaItemIndex ?: 0
                            if (_queue.value.isNotEmpty() &&
                                currentIndex < _queue.value.size
                            ) {
                                val newSong = _queue.value[currentIndex]
                                _currentSong.value = newSong
                                // ✅ Track recently played on auto transition too
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        libraryRepository.addToRecentlyPlayed(newSong)
                                    } catch (e: Exception) {
                                        android.util.Log.e(
                                            "PlayerManager",
                                            "Recently played: ${e.message}"
                                        )
                                    }
                                }
                            }
                        }
                    })
                }
        }
        return exoPlayer!!
    }

    private fun startProgressTracking() {
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

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        _currentSong.value = song
        if (queue.isNotEmpty()) _queue.value = queue

        // ✅ Track recently played
        CoroutineScope(Dispatchers.IO).launch {
            try {
                libraryRepository.addToRecentlyPlayed(song)
            } catch (e: Exception) {
                android.util.Log.e("PlayerManager", "Recently played: ${e.message}")
            }
        }

        // Start service for notification
        val serviceIntent = Intent(context, MusicPlayerService::class.java)
        context.startForegroundService(serviceIntent)

        val player = getExoPlayer()

        // Set entire queue in ExoPlayer
        val mediaItems = _queue.value.map { queueSong ->
            MediaItem.Builder()
                .setUri(queueSong.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(queueSong.title)
                        .setArtist(queueSong.artist)
                        .setAlbumTitle(queueSong.album)
                        .setArtworkUri(queueSong.imageUrl.toUri())
                        .build()
                )
                .build()
        }

        val currentIndex = _queue.value.indexOf(song).coerceAtLeast(0)
        player.setMediaItems(mediaItems, currentIndex, 0)
        player.prepare()
        player.play()
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

    fun release() {
        stopProgressTracking()
        exoPlayer?.release()
        exoPlayer = null
    }
}