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
    private val libraryRepository: LibraryRepository,
    private val audioEffectsManager: AudioEffectsManager
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
                                audioEffectsManager.initialize(audioSessionId)
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

                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int
                        ) {
                            val index =
                                exoPlayer?.currentMediaItemIndex ?: 0
                            val active = _activeQueue.value
                            if (active.isNotEmpty() && index < active.size) {
                                val newSong = active[index]
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
            audioEffectsManager.initialize(exoPlayer!!.audioSessionId)
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
                // Spotify's shuffle isn't pure logic; it injects weighted randomness
                // to keep it feeling like a shuffle and not a sorted list.
                val randomness = (0..30).random().toFloat()
                
                song to (score + randomness)
            }

        // 3. Sort by total score descending
        return scoredSongs.sortedByDescending { it.second }.map { it.first }
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        // ✅ Store original queue
        if (queue.isNotEmpty()) _queue.value = queue

        _currentSong.value = song

        // ✅ Build active queue based on shuffle state
        val activeQueue = when {
            _isSmartShuffle.value -> {
                listOf(song) + smartShuffle(_queue.value, song)
            }
            _isShuffleEnabled.value -> {
                listOf(song) + _queue.value
                    .filter { it.id != song.id }
                    .shuffled()
            }
            else -> {
                // ✅ Play from current song position in original queue
                val index = _queue.value.indexOfFirst { it.id == song.id }
                if (index >= 0) _queue.value
                else listOf(song) + _queue.value
            }
        }
        _activeQueue.value = activeQueue

        try {
            context.startForegroundService(
                Intent(context, MusicPlayerService::class.java)
            )
        } catch (e: Exception) { }

        val player = getExoPlayer()
        audioEffectsManager.initialize(player.audioSessionId)

        val items = activeQueue.map { s ->
            MediaItem.Builder()
                .setUri(s.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setArtworkUri(s.imageUrl.toUri())
                        .setDisplayTitle(s.title)
                        .setSubtitle(s.artist)
                        .build()
                )
                .build()
        }

        // ✅ Always start from index 0 (current song is first)
        player.setMediaItems(items, 0, 0L)

        // ✅ Set repeat ALL so it loops
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
            // OFF → Smart Shuffle
            !wasShuffle && !wasSmartShuffle -> {
                _isSmartShuffle.value = true
                _isShuffleEnabled.value = false
            }
            // Smart Shuffle → Normal Shuffle
            wasSmartShuffle -> {
                _isSmartShuffle.value = false
                _isShuffleEnabled.value = true
            }
            // Normal Shuffle → OFF
            wasShuffle -> {
                _isShuffleEnabled.value = false
                _isSmartShuffle.value = false
            }
        }

        // Apply to current playback if active
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
        val newQueue = listOf(currentSong) +
                smartShuffle(_queue.value, currentSong)
        _activeQueue.value = newQueue
        reloadPlayerQueue(newQueue, 0)
    }

    private fun applyNormalShuffle(currentSong: Song) {
        val newQueue = listOf(currentSong) +
                _queue.value.filter { it.id != currentSong.id }.shuffled()
        _activeQueue.value = newQueue
        reloadPlayerQueue(newQueue, 0)
    }

    private fun restoreLinearQueue(currentSong: Song) {
        val index = _queue.value.indexOfFirst { it.id == currentSong.id }
        val newQueue = if (index >= 0) _queue.value
        else listOf(currentSong) + _queue.value
        _activeQueue.value = newQueue
        val startIndex = newQueue.indexOfFirst { it.id == currentSong.id }
            .coerceAtLeast(0)
        reloadPlayerQueue(newQueue, startIndex)
    }

    private fun reloadPlayerQueue(queue: List<Song>, startIndex: Int) {
        val player = getExoPlayer()
        val wasPlaying = player.isPlaying
        val items = queue.map { s ->
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
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        if (wasPlaying) player.play()
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

            // Update ExoPlayer media item
            val mediaItem = MediaItem.Builder()
                .setUri(song.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.imageUrl.toUri())
                        .build()
                )
                .build()

            exoPlayer?.replaceMediaItem(index, mediaItem)
        }
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

    fun saveCurrentState() {
        android.util.Log.d("PlayerManager", "State save called")
    }

    fun resetState() {
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

    fun release() {
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}