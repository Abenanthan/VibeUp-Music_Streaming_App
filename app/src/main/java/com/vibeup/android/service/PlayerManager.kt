package com.vibeup.android.service

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.LibraryRepository
import com.vibeup.android.service.audio.SoftwareEqualizer
import com.vibeup.android.util.NetworkMonitor
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
    private val libraryRepository: LibraryRepository,
    private val audioEffectsManager: AudioEffectsManager,
    private val networkMonitor: NetworkMonitor,
    private val softwareEqualizer: SoftwareEqualizer,
    private val crossfadeManager: CrossfadeManager
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

    // ✅ Track the ID of the current queue (playlist ID, home section ID, etc)
    private val _currentQueueId = MutableStateFlow<String?>(null)
    val currentQueueId: StateFlow<String?> = _currentQueueId.asStateFlow()

    // ✅ Original queue (never modified)
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    // ✅ Active playback queue (may be shuffled/smart shuffled)
    private val _activeQueue = MutableStateFlow<List<Song>>(emptyList())
    val activeQueue: StateFlow<List<Song>> = _activeQueue.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isSmartShuffle = MutableStateFlow(false)
    val isSmartShuffle: StateFlow<Boolean> = _isSmartShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    val isRestored = MutableStateFlow(false)

    fun getExoPlayer(): ExoPlayer {
        if (exoPlayer == null) {
            val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): androidx.media3.exoplayer.audio.AudioSink {
                    return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                        .setAudioProcessors(arrayOf(softwareEqualizer))
                        .build()
                }
            }
            renderersFactory.setEnableDecoderFallback(true)

            exoPlayer = ExoPlayer.Builder(context, renderersFactory)
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

                        override fun onShuffleModeEnabledChanged(
                            shuffleModeEnabled: Boolean
                        ) {
                            _isShuffleEnabled.value = shuffleModeEnabled
                        }

                        override fun onRepeatModeChanged(repeatMode: Int) {
                            _repeatMode.value = repeatMode
                        }

                        override fun onAudioSessionIdChanged(audioSessionId: Int) {
                            audioEffectsManager.initialize(audioSessionId)
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            android.util.Log.e("PlayerManager", "Player error: ${error.message}")
                            _playbackError.value = when (error.errorCode) {
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Network connection lost"
                                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "Failed to load song"
                                else -> "Playback error: ${error.errorCodeName}"
                            }
                            _isPlaying.value = false
                            stopTracking()

                            // ✅ Auto-skip if possible
                            if (hasNextMediaItem()) {
                                android.util.Log.d("PlayerManager", "Attempting auto-skip after error...")
                                seekToNextMediaItem()
                                prepare()
                                play()
                            }
                        }

                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int
                        ) {
                            val mediaId = mediaItem?.mediaId
                            val active = _activeQueue.value
                            if (mediaId != null) {
                                val song = active.find { it.id == mediaId }
                                if (song != null) {
                                    _currentSong.value = song
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            libraryRepository
                                                .addToRecentlyPlayed(song)
                                        } catch (e: Exception) { }
                                    }
                                }
                            } else {
                                // Fallback to index if mediaId is missing for some reason
                                val index = exoPlayer?.currentMediaItemIndex ?: 0
                                if (active.isNotEmpty() && index < active.size) {
                                    _currentSong.value = active[index]
                                }
                            }

                            // ✅ Reset volume and restart monitoring on every transition
                            exoPlayer?.let {
                                if (crossfadeManager.isEnabled.value) {
                                    crossfadeManager.fadeIn(it) { }
                                    startCrossfadeMonitoring(it)
                                } else {
                                    it.volume = 1f
                                }
                            }
                        }
                    })
                }
        }
        return exoPlayer!!
    }

    // ✅ Smart shuffle — Spotify-style similarity scoring
    private fun smartShuffle(songs: List<Song>, currentSong: Song): List<Song> {
        if (songs.size <= 1) return songs

        // 1. Calculate similarity scores for all songs relative to current
        val scoredSongs = songs
            .filter { it.id != currentSong.id }
            .map { song ->
                var score = 0f
                
                // Language is the strongest signal for native music
                if (song.language.lowercase() == currentSong.language.lowercase()) {
                    score += 50f
                }
                
                // Artist match (includes collaboration checks)
                val currentArtists = currentSong.artist.lowercase()
                    .split(",", "&", "feat", "ft").map { it.trim() }
                val songArtists = song.artist.lowercase()
                    .split(",", "&", "feat", "ft").map { it.trim() }
                
                if (currentArtists.any { songArtists.contains(it) }) {
                    score += 30f
                }
                
                // Album match
                if (song.album.isNotEmpty() && song.album == currentSong.album) {
                    score += 15f
                }

                // Keyword/Mood matching in titles
                val currentTitleWords = currentSong.title.lowercase()
                    .split(" ", "-", "(", ")").filter { it.length > 3 }
                val songTitleWords = song.title.lowercase()
                    .split(" ", "-", "(", ")").filter { it.length > 3 }
                
                val commonWords = currentTitleWords.intersect(songTitleWords.toSet())
                score += commonWords.size * 5f

                // 2. Add "Entropy" (Randomness)
                val randomness = (0..30).random().toFloat()
                
                song to (score + randomness)
            }

        // 3. Sort by total score descending
        return scoredSongs.sortedByDescending { it.second }.map { it.first }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList(), queueId: String? = null) {
        _playbackError.value = null
        
        // ✅ 1. Network check for remote songs
        val isLocal = song.audioUrl.startsWith("file://") || 
                      song.audioUrl.startsWith("content://") ||
                      song.language == "local"
        
        if (!isLocal && !networkMonitor.isOnline.value) {
            _playbackError.value = "No network connection"
            return
        }

        // ✅ 2. Basic URL validation
        if (song.audioUrl.isBlank()) {
            _playbackError.value = "Unable to play: No audio URL"
            return
        }

        if (queue.isNotEmpty()) _queue.value = queue
        _currentSong.value = song
        _currentQueueId.value = queueId

        val startIndex: Int
        val finalQueue: List<Song>

        when {
            _isSmartShuffle.value -> {
                finalQueue = listOf(song) + smartShuffle(_queue.value, song)
                startIndex = 0
            }
            _isShuffleEnabled.value -> {
                finalQueue = listOf(song) + _queue.value
                    .filter { it.id != song.id }
                    .shuffled()
                startIndex = 0
            }
            else -> {
                val index = _queue.value.indexOfFirst { it.id == song.id }
                if (index >= 0) {
                    finalQueue = _queue.value
                    startIndex = index
                } else {
                    finalQueue = listOf(song) + _queue.value
                    startIndex = 0
                }
            }
        }
        _activeQueue.value = finalQueue

        try {
            context.startForegroundService(
                Intent(context, MusicPlayerService::class.java)
            )
        } catch (e: Exception) { 
            android.util.Log.e("PlayerManager", "Failed to start service: ${e.message}")
        }

        val player = getExoPlayer()

        val items = finalQueue.mapNotNull { s ->
            try {
                if (s.audioUrl.isBlank()) return@mapNotNull null
                
                MediaItem.Builder()
                    .setUri(s.audioUrl.toUri())
                    .setMediaId(s.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setAlbumTitle(s.album)
                            .setArtworkUri(if (s.imageUrl.isNotBlank()) s.imageUrl.toUri() else null)
                            .build()
                    )
                    .build()
            }

            catch (e: Exception) {
                android.util.Log.e("PlayerManager", "Error building MediaItem for ${s.title}: ${e.message}")
                null
            }
        }

        if (items.isEmpty()) {
            _playbackError.value = "Failed to load playlist"
            return
        }

        // Adjust startIndex if we filtered out the original song
        val adjustedIndex = if (startIndex < finalQueue.size) {
            val targetId = finalQueue[startIndex].id
            items.indexOfFirst { it.mediaId == targetId }.coerceAtLeast(0)
        } else 0

        player.setMediaItems(items, adjustedIndex, 0L)
        player.repeatMode = Player.REPEAT_MODE_ALL
        _repeatMode.value = Player.REPEAT_MODE_ALL
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
        val wasSmartShuffle = _isSmartShuffle.value
        val wasShuffle = _isShuffleEnabled.value

        when {
            !wasShuffle && !wasSmartShuffle -> {
                _isSmartShuffle.value = true
                _isShuffleEnabled.value = false
            }
            wasSmartShuffle -> {
                _isSmartShuffle.value = false
                _isShuffleEnabled.value = true
            }
            else -> {
                _isShuffleEnabled.value = false
                _isSmartShuffle.value = false
            }
        }

        val currentSong = _currentSong.value
        if (currentSong != null) {
            when {
                _isSmartShuffle.value -> applySmartShuffle(currentSong)
                _isShuffleEnabled.value -> applyNormalShuffle(currentSong)
                else -> restoreLinearQueue(currentSong)
            }
        }
    }

    private fun applySmartShuffle(currentSong: Song) {
        val newQueue = listOf(currentSong) + smartShuffle(_queue.value, currentSong)
        _activeQueue.value = newQueue
        reloadPlayerQueue(newQueue, 0)
    }

    private fun applyNormalShuffle(currentSong: Song) {
        val newQueue = listOf(currentSong) + _queue.value.filter { it.id != currentSong.id }.shuffled()
        _activeQueue.value = newQueue
        reloadPlayerQueue(newQueue, 0)
    }

    private fun restoreLinearQueue(currentSong: Song) {
        val index = _queue.value.indexOfFirst { it.id == currentSong.id }
        val newQueue = if (index >= 0) _queue.value else listOf(currentSong) + _queue.value
        _activeQueue.value = newQueue
        val startIndex = newQueue.indexOfFirst { it.id == currentSong.id }.coerceAtLeast(0)
        reloadPlayerQueue(newQueue, startIndex)
    }

    private fun reloadPlayerQueue(queue: List<Song>, startIndex: Int) {
        val player = getExoPlayer()
        val wasPlaying = player.isPlaying
        val items = queue.mapNotNull { s ->
            try {
                if (s.audioUrl.isBlank()) return@mapNotNull null
                MediaItem.Builder()
                    .setUri(s.audioUrl.toUri())
                    .setMediaId(s.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setAlbumTitle(s.album)
                            .setArtworkUri(if (s.imageUrl.isNotBlank()) s.imageUrl.toUri() else null)
                            .build()
                    )
                    .build()
            } catch (e: Exception) { null }
        }
        
        if (items.isNotEmpty()) {
            val adjustedIndex = if (startIndex < queue.size) {
                val targetId = queue[startIndex].id
                items.indexOfFirst { it.mediaId == targetId }.coerceAtLeast(0)
            } else 0
            
            player.setMediaItems(items, adjustedIndex, 0L)
            player.prepare()
            if (wasPlaying) player.play()
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
        _repeatMode.value = nextMode
    }

    fun updateQueueItem(index: Int, song: Song) {
        val currentQueue = _activeQueue.value.toMutableList()
        if (index < currentQueue.size) {
            currentQueue[index] = song
            _activeQueue.value = currentQueue

            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(song.audioUrl.toUri())
                    .setMediaId(song.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(if (song.imageUrl.isNotBlank()) song.imageUrl.toUri() else null)
                            .build()
                    )
                    .build()

                exoPlayer?.replaceMediaItem(index, mediaItem)
            } catch (e: Exception) { }
        }
    }

    private fun startTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                exoPlayer?.let {
                    if (it.playbackState != Player.STATE_IDLE) {
                        _currentPosition.value = it.currentPosition
                        _duration.value = it.duration.coerceAtLeast(0L)
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopTracking() {
        progressJob?.cancel()
    }

    // Moves a song from [fromIndex] to [toIndex] within the active queue and
    // Keeps ExoPlayer's internal media item order in sync via moveMediaItem,
    // Which is far cheaper than a full reloadPlayerQueue (no re-buffering).

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val current = _activeQueue.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val song = current.removeAt(fromIndex)
        current.add(toIndex, song)
        _activeQueue.value = current

        try {
            getExoPlayer().moveMediaItem(fromIndex, toIndex)
        } catch (e: Exception) {
            android.util.Log.e("PlayerManager", "moveQueueItem failed: ${e.message}")
        }
    }

    // * Removes a song from the queue by index. Refuses to remove the currently
    // * playing item — UI should disable the swipe affordance on that row instead
    // * of relying on this guard, but it's here as a safety net.

    fun removeFromQueue(index: Int) {
        val player = getExoPlayer()
        if (index == player.currentMediaItemIndex) return

        val current = _activeQueue.value.toMutableList()
        if (index !in current.indices) return

        current.removeAt(index)
        _activeQueue.value = current

        try {
            player.removeMediaItem(index)
        } catch (e: Exception) {
            android.util.Log.e("PlayerManager", "removeFromQueue failed: ${e.message}")
        }
    }

    // * Jumps playback directly to a song already in the active queue, by index.
    // * Used when the user taps a row in the queue screen.

    fun jumpToQueueIndex(index: Int) {
        val player = getExoPlayer()
        if (index !in _activeQueue.value.indices) return
        try {
            player.seekToDefaultPosition(index)
            player.play()
        } catch (e: Exception) {
            android.util.Log.e("PlayerManager", "jumpToQueueIndex failed: ${e.message}")
        }
    }

    private fun startCrossfadeMonitoring(player: ExoPlayer) {
        crossfadeManager.stopMonitoring()
        if (!crossfadeManager.isEnabled.value) {
            player.volume = 1f
            return
        }

        val startIndex = player.currentMediaItemIndex

        crossfadeManager.startMonitoring(player) {
            android.util.Log.d("PlayerManager", "Crossfade triggered!")
            crossfadeManager.fadeOut(player) {
                // ✅ Only skip if ExoPlayer hasn't transitioned naturally yet
                if (player.currentMediaItemIndex == startIndex && player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                }
            }
        }
    }

    fun resetState() {
        crossfadeManager.stopMonitoring()  // ← ADD
        exoPlayer?.let {
            crossfadeManager.cancelFade(it)  // ← ADD
        }
        progressJob?.cancel()
        audioEffectsManager.releaseEffects()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        exoPlayer?.release()
        exoPlayer = null
        _currentSong.value = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        _queue.value = emptyList()
        _activeQueue.value = emptyList()
        _isSmartShuffle.value = false
        _isShuffleEnabled.value = false
    }

    fun clearError() {
        _playbackError.value = null
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer?.let {
            it.stop()
            it.release()
        }
        exoPlayer = null
    }
}
