package com.vibeup.android.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.local.dao.LyricsDao
import com.vibeup.android.data.local.entity.CachedLyrics
import com.vibeup.android.data.remote.api.LyricsApiService
import com.vibeup.android.data.remote.api.LyricsOvhApiService
import com.vibeup.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class LyricLine(
    val timeMs: Long,
    val text: String
)

sealed class LyricsState {
    object Idle : LyricsState()
    object Loading : LyricsState()
    data class SyncedLoaded(val lines: List<LyricLine>) : LyricsState()
    data class PlainLoaded(val lyrics: String) : LyricsState()
    object NotFound : LyricsState()
    object Instrumental : LyricsState()
    data class Error(val message: String) : LyricsState()
}

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsApiService: LyricsApiService,
    private val lyricsOvhApiService: LyricsOvhApiService,
    private val lyricsDao: LyricsDao
) : ViewModel() {

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private val _currentLineIndex = MutableStateFlow(0)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()

    private val _showSynced = MutableStateFlow(true)
    val showSynced: StateFlow<Boolean> = _showSynced.asStateFlow()

    // ✅ Track loaded song publicly
    var lastLoadedSongId: String? = null
        private set

    private var fetchJob: Job? = null
    private var prefetchJob: Job? = null

    // ✅ Prefetch cache for next song
    private val prefetchCache = mutableMapOf<String, LyricsState>()

    fun loadLyrics(song: Song, force: Boolean = false) {
        if (!force &&
            lastLoadedSongId == song.id &&
            _lyricsState.value !is LyricsState.Idle &&
            _lyricsState.value !is LyricsState.Error
        ) return

        lastLoadedSongId = song.id
        fetchJob?.cancel()
        _currentLineIndex.value = 0

        // ✅ Check prefetch cache first (instant!)
        prefetchCache[song.id]?.let { cached ->
            _lyricsState.value = cached
            android.util.Log.d("Lyrics", "Loaded from prefetch cache!")
            return
        }

        _lyricsState.value = LyricsState.Loading

        fetchJob = viewModelScope.launch {
            val result = fetchLyrics(song)
            _lyricsState.value = result
            // Cache it
            prefetchCache[song.id] = result
        }
    }

    // ✅ Prefetch lyrics for upcoming songs (call this proactively)
    fun prefetchLyrics(songs: List<Song>) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            songs.take(3).forEach { song ->
                if (prefetchCache.containsKey(song.id)) return@forEach
                try {
                    val result = fetchLyrics(song)
                    prefetchCache[song.id] = result
                    android.util.Log.d(
                        "Lyrics",
                        "Prefetched: ${song.title}"
                    )
                } catch (e: Exception) { }
            }
        }
    }

    private suspend fun fetchLyrics(song: Song): LyricsState {
        return withContext(Dispatchers.IO) {
            // ✅ Step 1 — Check local Room cache
            val cached = lyricsDao.getLyrics(song.id)
            if (cached != null) {
                return@withContext buildStateFromCache(cached)
            }

            // ✅ Step 2 — Try with original details first
            var result = tryFetchAll(song.artist, song.title, song.album)
            
            // ✅ Step 3 — Fallback: Try with cleaned details if not found
            if (result is LyricsState.NotFound) {
                val cleanedArtist = cleanArtist(song.artist)
                val cleanedTitle = cleanTitle(song.title)
                
                if (cleanedArtist != song.artist || cleanedTitle != song.title) {
                    android.util.Log.d("Lyrics", "Retrying with cleaned: $cleanedArtist - $cleanedTitle")
                    result = tryFetchAll(cleanedArtist, cleanedTitle, null)
                }
            }

            // ✅ Cache to Room DB
            if (result !is LyricsState.Loading && result !is LyricsState.Idle) {
                saveToDB(song.id, result)
            }

            result
        }
    }

    private suspend fun tryFetchAll(artist: String, title: String, album: String?): LyricsState {
        // Fetch from BOTH providers in parallel
        val lrclibDeferred = viewModelScope.async(Dispatchers.IO) {
            withTimeoutOrNull(5000) { fetchFromLrclib(artist, title, album) }
        }
        val ovhDeferred = viewModelScope.async(Dispatchers.IO) {
            withTimeoutOrNull(5000) { fetchFromLyricsOvh(artist, title) }
        }

        val lrclibResult = lrclibDeferred.await()
        val ovhResult = ovhDeferred.await()

        return when {
            lrclibResult is LyricsState.SyncedLoaded -> lrclibResult
            lrclibResult is LyricsState.PlainLoaded -> lrclibResult
            ovhResult is LyricsState.PlainLoaded -> ovhResult
            lrclibResult is LyricsState.Instrumental -> lrclibResult
            else -> LyricsState.NotFound
        }
    }

    private suspend fun fetchFromLrclib(artist: String, title: String, album: String?): LyricsState {
        return try {
            val response = lyricsApiService.getLyrics(
                artistName = artist,
                trackName = title,
                albumName = album ?: ""
            )

            when {
                response.instrumental == true -> LyricsState.Instrumental
                response.syncedLyrics != null -> {
                    val lines = parseSyncedLyrics(response.syncedLyrics)
                    if (lines.isNotEmpty()) LyricsState.SyncedLoaded(lines)
                    else if (response.plainLyrics != null) LyricsState.PlainLoaded(response.plainLyrics)
                    else searchLrclib(artist, title)
                }
                response.plainLyrics != null -> LyricsState.PlainLoaded(response.plainLyrics)
                else -> searchLrclib(artist, title)
            }
        } catch (e: Exception) {
            searchLrclib(artist, title)
        }
    }

    private suspend fun searchLrclib(artist: String, title: String): LyricsState {
        return try {
            val results = lyricsApiService.searchLyrics(artistName = artist, trackName = title)
            val first = results.firstOrNull() ?: return LyricsState.NotFound

            when {
                first.instrumental == true -> LyricsState.Instrumental
                first.syncedLyrics != null -> {
                    val lines = parseSyncedLyrics(first.syncedLyrics)
                    if (lines.isNotEmpty()) LyricsState.SyncedLoaded(lines)
                    else if (first.plainLyrics != null) LyricsState.PlainLoaded(first.plainLyrics)
                    else LyricsState.NotFound
                }
                first.plainLyrics != null -> LyricsState.PlainLoaded(first.plainLyrics)
                else -> LyricsState.NotFound
            }
        } catch (e: Exception) {
            LyricsState.NotFound
        }
    }

    private suspend fun fetchFromLyricsOvh(artist: String, title: String): LyricsState {
        return try {
            val response = lyricsOvhApiService.getLyrics(artist = artist, title = title)
            if (!response.lyrics.isNullOrBlank()) {
                LyricsState.PlainLoaded(response.lyrics.trim())
            } else {
                LyricsState.NotFound
            }
        } catch (e: Exception) {
            LyricsState.NotFound
        }
    }

    private fun buildStateFromCache(cached: CachedLyrics): LyricsState {
        return when {
            cached.isInstrumental -> LyricsState.Instrumental
            cached.syncedLyrics != null -> {
                val lines = parseSyncedLyrics(cached.syncedLyrics)
                if (lines.isNotEmpty())
                    LyricsState.SyncedLoaded(lines)
                else if (cached.plainLyrics != null)
                    LyricsState.PlainLoaded(cached.plainLyrics)
                else
                    LyricsState.NotFound
            }
            cached.plainLyrics != null ->
                LyricsState.PlainLoaded(cached.plainLyrics)
            else -> LyricsState.NotFound
        }
    }

    private suspend fun saveToDB(songId: String, state: LyricsState) {
        try {
            val entity = when (state) {
                is LyricsState.SyncedLoaded -> CachedLyrics(
                    songId = songId,
                    syncedLyrics = state.lines.joinToString("\n") {
                        "[${formatTime(it.timeMs)}] ${it.text}"
                    },
                    plainLyrics = state.lines.joinToString("\n") { it.text }
                )
                is LyricsState.PlainLoaded -> CachedLyrics(
                    songId = songId,
                    syncedLyrics = null,
                    plainLyrics = state.lyrics
                )
                is LyricsState.Instrumental -> CachedLyrics(
                    songId = songId,
                    syncedLyrics = null,
                    plainLyrics = null,
                    isInstrumental = true
                )
                else -> return
            }
            lyricsDao.insertLyrics(entity)
        } catch (e: Exception) {
            android.util.Log.e("Lyrics", "DB save error: ${e.message}")
        }
    }

    private fun formatTime(ms: Long): String {
        val min = ms / 60000
        val sec = (ms % 60000) / 1000
        val centis = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(min, sec, centis)
    }

    fun updateCurrentLine(positionMs: Long) {
        val state = _lyricsState.value
        if (state !is LyricsState.SyncedLoaded) return
        val lines = state.lines
        var index = 0
        for (i in lines.indices) {
            if (positionMs >= lines[i].timeMs) index = i
            else break
        }
        if (_currentLineIndex.value != index) {
            _currentLineIndex.value = index
        }
    }

    private fun parseSyncedLyrics(synced: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        synced.lines().forEach { line ->
            val match = regex.find(line.trim()) ?: return@forEach
            val (min, sec, ms, text) = match.destructured
            val timeMs = (min.toLong() * 60000) +
                    (sec.toLong() * 1000) +
                    if (ms.length == 2) ms.toLong() * 10 else ms.toLong()
            val clean = text.trim()
            if (clean.isNotEmpty()) lines.add(LyricLine(timeMs, clean))
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun cleanArtist(artist: String): String {
        return artist.split(",", "&", "feat.", "ft.", "Feat.", "Ft.", "By", "by", "|")
            .first()
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .trim()
    }

    private fun cleanTitle(title: String): String {
        return title.replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("- From \".*?\""), "")
            .replace(Regex("- .*?$"), "")
            .replace(Regex("[^a-zA-Z0-9 ]"), "")
            .trim()
    }

    fun toggleDisplayMode() {
        _showSynced.value = !_showSynced.value
    }

    fun resetLyrics() {
        fetchJob?.cancel()
        _lyricsState.value = LyricsState.Idle
        lastLoadedSongId = null
        _currentLineIndex.value = 0
    }
}