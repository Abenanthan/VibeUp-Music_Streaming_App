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
import com.vibeup.android.domain.repository.SongRepository
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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val songRepository: SongRepository,
    private val audioEffectsManager: AudioEffectsManager,
    private val networkMonitor: NetworkMonitor,
    private val softwareEqualizer: SoftwareEqualizer,
    private val crossfadeManager: CrossfadeManager
) {
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var autoSaveJob: Job? = null

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

    // Counts consecutive playback errors so a queue full of bad/expired URLs can't
    // send ExoPlayer into an endless skip loop. Reset to 0 whenever a song loads OK.
    private var errorSkipCount = 0

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
                            if (playing) {
                                startTracking()
                                startAutoSave()
                            } else {
                                stopTracking()
                                stopAutoSave()
                                saveState()
                            }
                        }

                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                _duration.value = duration
                                // A song loaded fine → we're not in an error loop.
                                errorSkipCount = 0
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
                            android.util.Log.e("PlayerManager", "Player error: ${error.errorCodeName} ${error.message}")
                            val player = exoPlayer ?: return
                            _isPlaying.value = false
                            stopTracking()

                            val active = _activeQueue.value
                            errorSkipCount++

                            // Circuit breaker: if we've failed more times in a row than
                            // there are songs, stop — never loop the whole queue forever.
                            if (errorSkipCount > active.size.coerceAtLeast(1)) {
                                errorSkipCount = 0
                                _playbackError.value = "Couldn't play this song"
                                return
                            }

                            val idx = player.currentMediaItemIndex
                            val current = active.getOrNull(idx)
                            val isStreaming = current != null &&
                                current.language != "local" && current.id.isNotBlank()

                            // For a streaming song, the URL is likely a restore placeholder
                            // or an expired CDN link. Fetch a FRESH url and retry the SAME
                            // song in place instead of skipping — this is what breaks the
                            // infinite skip loop after session restore.
                            if (isStreaming) {
                                scope.launch(Dispatchers.IO) {
                                    val resolved = try {
                                        songRepository.getPlayableSong(current!!.id)
                                    } catch (e: Exception) { null }
                                    withContext(Dispatchers.Main) {
                                        val p = exoPlayer ?: return@withContext
                                        if (resolved != null &&
                                            resolved.audioUrl.isNotBlank() &&
                                            resolved.audioUrl != current!!.audioUrl
                                        ) {
                                            updateQueueItem(idx, resolved)
                                            p.prepare()
                                            p.play()
                                        } else if (p.hasNextMediaItem()) {
                                            p.seekToNextMediaItem(); p.prepare(); p.play()
                                        } else {
                                            _playbackError.value = "Couldn't play this song"
                                        }
                                    }
                                }
                                return
                            }

                            // Local / unknown item — bounded skip.
                            if (player.hasNextMediaItem()) {
                                player.seekToNextMediaItem(); player.prepare(); player.play()
                            } else {
                                _playbackError.value = "Couldn't play this song"
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
                                            libraryRepository.addToRecentlyPlayed(song)
                                        } catch (e: Exception) { }
                                    }
                                }
                            } else {
                                val index = exoPlayer?.currentMediaItemIndex ?: 0
                                if (active.isNotEmpty() && index < active.size) {
                                    _currentSong.value = active[index]
                                }
                            }

                            // Pre-fetch the NEXT song's URL while the current song
                            // plays, so ExoPlayer has a valid URI ready before it
                            // tries to buffer it. This is the fix for auto-advance
                            // failing after session restore.
                            val player = exoPlayer ?: return
                            val nextIndex = player.currentMediaItemIndex + 1
                            val active2 = _activeQueue.value
                            if (nextIndex < active2.size) {
                                val nextSong = active2[nextIndex]
                                if (nextSong.audioUrl.isBlank() &&
                                    nextSong.language != "local" &&
                                    nextSong.id.isNotBlank()
                                ) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val resolved = songRepository.getPlayableSong(nextSong.id)
                                            if (resolved != null && resolved.audioUrl.isNotBlank()) {
                                                withContext(Dispatchers.Main) {
                                                    updateQueueItem(nextIndex, resolved)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("PlayerManager", "Pre-fetch next song failed: ${e.message}")
                                        }
                                    }
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
                            
                            saveState()
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
        isRestored.value = true
        errorSkipCount = 0
        
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
        if (index !in _activeQueue.value.indices) return
        val song = _activeQueue.value[index]

        scope.launch {
            // If this song has no URL (empty after session restore), fetch it
            // before seeking. Without this, ExoPlayer errors and plays nothing.
            if (song.audioUrl.isBlank() &&
                song.language != "local" &&
                song.id.isNotBlank()
            ) {
                try {
                    val resolved = withContext(Dispatchers.IO) {
                        songRepository.getPlayableSong(song.id)
                    }
                    if (resolved != null && resolved.audioUrl.isNotBlank()) {
                        updateQueueItem(index, resolved)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PlayerManager", "jumpToQueueIndex URL resolve failed: ${e.message}")
                    return@launch  // Don't try to play if we can't get a URL
                }
            }

            try {
                val player = getExoPlayer()
                player.seekToDefaultPosition(index)
                player.play()
            } catch (e: Exception) {
                android.util.Log.e("PlayerManager", "jumpToQueueIndex seek failed: ${e.message}")
            }
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

    fun setPlaybackSpeed(speed: Float) {
        try {
            val params = androidx.media3.common.PlaybackParameters(speed)
            getExoPlayer().playbackParameters = params
        } catch (e: Exception) {
            android.util.Log.e("PlayerManager", "Speed change failed: ${e.message}")
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

    // ── Persistence ──────────────────────────────────────────────────────────

    private data class PlaybackState(
        val queue: List<Song>,
        val activeQueue: List<Song>,
        val currentIndex: Int,
        val position: Long,
        val queueId: String?,
        val isShuffleEnabled: Boolean,
        val isSmartShuffle: Boolean,
        val repeatMode: Int
    ) : Serializable

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            while (true) {
                delay(30000) // 30 seconds
                saveState()
            }
        }
    }

    private fun stopAutoSave() {
        autoSaveJob?.cancel()
    }

    fun saveState() {
        val player = exoPlayer ?: return
        val currentQueue = _queue.value
        val currentActive = _activeQueue.value
        if (currentActive.isEmpty()) return

        // Strip URLs to force fresh ones on restore (prevent CDN expiry issues)
        val strippedQueue = currentQueue.map { it.copy(audioUrl = "") }
        val strippedActive = currentActive.map { it.copy(audioUrl = "") }

        val state = PlaybackState(
            queue = strippedQueue,
            activeQueue = strippedActive,
            currentIndex = player.currentMediaItemIndex,
            position = player.currentPosition,
            queueId = _currentQueueId.value,
            isShuffleEnabled = _isShuffleEnabled.value,
            isSmartShuffle = _isSmartShuffle.value,
            repeatMode = player.repeatMode
        )

        scope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "playback_state.bin")
                ObjectOutputStream(FileOutputStream(file)).use { it.writeObject(state) }
            } catch (e: Exception) {
                android.util.Log.e("PlayerManager", "Save state failed: ${e.message}")
            }
        }
    }

    fun restoreState() {
        if (isRestored.value) return
        // Claim immediately so the two cold-start callers (MainActivity + the
        // MusicPlayerService) can't both run restore and stack duplicate queues.
        isRestored.value = true

        scope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "playback_state.bin")
                if (!file.exists()) return@launch

                val state = ObjectInputStream(FileInputStream(file)).use { 
                    it.readObject() as PlaybackState 
                }

                withContext(Dispatchers.Main) {
                    val player = getExoPlayer()
                    
                    // Initial metadata restore
                    _queue.value = state.queue
                    _activeQueue.value = state.activeQueue
                    _currentQueueId.value = state.queueId
                    _isShuffleEnabled.value = state.isShuffleEnabled
                    _isSmartShuffle.value = state.isSmartShuffle
                    player.repeatMode = state.repeatMode
                    _repeatMode.value = state.repeatMode
                    
                    val index = state.currentIndex.coerceIn(state.activeQueue.indices)
                    if (index != -1) {
                        val restoredSong = state.activeQueue[index]
                        _currentSong.value = restoredSong
                        _duration.value = restoredSong.duration.toLong() * 1000L
                        _currentPosition.value = state.position
                    }

                    // Fetch fresh URLs for the entire queue in one go
                    // Fetch fresh URL for ONLY the current + next song.
                    // Previous approach fetched ALL songs via a single batch call
                    // (getSongsByIds) which fails intermittently on large queues,
                    // leaving all other songs with "https://vibeup.invalid" URLs.
                    // Those invalid URLs caused ExoPlayer to error and loop on the
                    // same song. Now we fetch one or two songs synchronously and
                    // resolve the rest lazily via onMediaItemTransition pre-fetch.
                    scope.launch(Dispatchers.IO) {
                        val currentSong = state.activeQueue[index]
                        val nextIndex = if (index + 1 < state.activeQueue.size) index + 1 else -1
                        val nextSong = if (nextIndex != -1) state.activeQueue[nextIndex] else null

                        // Resolve current song — one API call, almost never fails
                        val resolvedCurrent = if (
                            currentSong.language != "local" && currentSong.id.isNotBlank()
                        ) {
                            try { songRepository.getPlayableSong(currentSong.id) ?: currentSong }
                            catch (e: Exception) { currentSong }
                        } else currentSong

                        // Resolve next song — pre-fetched so auto-advance works immediately
                        val resolvedNext = if (
                            nextSong != null &&
                            nextSong.language != "local" &&
                            nextSong.id.isNotBlank()
                        ) {
                            try { songRepository.getPlayableSong(nextSong.id) ?: nextSong }
                            catch (e: Exception) { nextSong }
                        } else nextSong

                        // All remaining songs keep audioUrl = "" for now.
                        // onMediaItemTransition will resolve each one lazily just
                        // before ExoPlayer needs it.
                        val restoredActive = state.activeQueue.mapIndexed { i, song ->
                            when (i) {
                                index     -> resolvedCurrent
                                nextIndex -> resolvedNext ?: song
                                else      -> song
                            }
                        }

                        // If even the current song couldn't be resolved, abort restore.
                        // Better to show an empty state than play an invalid URL.
                        if (resolvedCurrent.audioUrl.isBlank() && currentSong.language != "local") {
                            android.util.Log.e("PlayerManager", "Restore aborted: current song URL unresolvable")
                            return@launch
                        }

                        withContext(Dispatchers.Main) {
                            _activeQueue.value = restoredActive
                            _currentSong.value = resolvedCurrent
                            _duration.value = resolvedCurrent.duration.toLong() * 1000L
                            _currentPosition.value = state.position

                            val items = restoredActive.map { s ->
                                MediaItem.Builder()
                                    // Songs without a URL get an empty-path URI.
                                    // ExoPlayer won't try to load them until auto-advance
                                    // reaches them, at which point onMediaItemTransition
                                    // pre-fetch will have already replaced the item with
                                    // a valid URL via updateQueueItem().
                                    .setUri(
                                        if (s.audioUrl.isNotBlank()) s.audioUrl.toUri()
                                        else "vibeup://pending/${s.id}".toUri()
                                    )
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

                            // Pass the resume position straight to setMediaItems as the
                            // start position — ExoPlayer honours it once the item is
                            // ready. This replaces the old delayed seek-listener, which
                            // could fire on a DIFFERENT song the user started meanwhile
                            // and yank it back to the stored offset ("stops in middle").
                            player.setMediaItems(items, index, state.position)
                            player.playWhenReady = false
                            player.prepare()
                            _currentPosition.value = state.position
                            android.util.Log.d("PlayerManager",
                                "Restored: ${resolvedCurrent.title} at ${state.position}ms")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerManager", "Restore state failed: ${e.message}")
            }
        }
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
