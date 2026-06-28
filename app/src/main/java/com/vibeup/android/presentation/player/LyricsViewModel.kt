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
            // ✅ Step 1 — Check local Room cache (instant)
            val cached = lyricsDao.getLyrics(song.id)
            if (cached != null) {
                android.util.Log.d("Lyrics", "Cache hit: ${song.title}")
                return@withContext buildStateFromCache(cached)
            }

            android.util.Log.d("Lyrics", "Fetching: ${song.title}")

            // ✅ Step 2 — Fetch from BOTH providers in parallel
            val lrclibDeferred = async {
                withTimeoutOrNull(5000) {
                    fetchFromLrclib(song)
                }
            }
            val ovhDeferred = async {
                withTimeoutOrNull(5000) {
                    fetchFromLyricsOvh(song)
                }
            }

            // ✅ Use first synced result, fallback to plain
            val lrclibResult = lrclibDeferred.await()
            val ovhResult = ovhDeferred.await()

            val finalResult = when {
                // Prefer synced from LRCLIB
                lrclibResult is LyricsState.SyncedLoaded -> lrclibResult
                // Then plain from LRCLIB
                lrclibResult is LyricsState.PlainLoaded -> lrclibResult
                // Then lyrics.ovh
                ovhResult is LyricsState.PlainLoaded -> ovhResult
                // Instrumental
                lrclibResult is LyricsState.Instrumental -> lrclibResult
                // Not found
                else -> LyricsState.NotFound
            }

            // ✅ Cache to Room DB for future instant load
            saveToDB(song.id, finalResult)

            finalResult
        }
    }

    private suspend fun fetchFromLrclib(song: Song): LyricsState {
        return try {
            val response = lyricsApiService.getLyrics(
                artistName = cleanArtist(song.artist),
                trackName = cleanTitle(song.title),
                albumName = song.album
            )

            when {
                response.instrumental == true ->
                    LyricsState.Instrumental

                response.syncedLyrics != null -> {
                    val lines = parseSyncedLyrics(response.syncedLyrics)
                    if (lines.isNotEmpty())
                        LyricsState.SyncedLoaded(lines)
                    else if (response.plainLyrics != null)
                        LyricsState.PlainLoaded(response.plainLyrics)
                    else
                        searchLrclib(song)
                }

                response.plainLyrics != null ->
                    LyricsState.PlainLoaded(response.plainLyrics)

                else -> searchLrclib(song)
            }
        } catch (e: Exception) {
            searchLrclib(song)
        }
    }

    private suspend fun searchLrclib(song: Song): LyricsState {
        return try {
            val results = lyricsApiService.searchLyrics(
                artistName = cleanArtist(song.artist),
                trackName = cleanTitle(song.title)
            )
            val first = results.firstOrNull()
                ?: return LyricsState.NotFound

            when {
                first.instrumental == true ->
                    LyricsState.Instrumental

                first.syncedLyrics != null -> {
                    val lines = parseSyncedLyrics(first.syncedLyrics)
                    if (lines.isNotEmpty())
                        LyricsState.SyncedLoaded(lines)
                    else if (first.plainLyrics != null)
                        LyricsState.PlainLoaded(first.plainLyrics)
                    else
                        LyricsState.NotFound
                }

                first.plainLyrics != null ->
                    LyricsState.PlainLoaded(first.plainLyrics)

                else -> LyricsState.NotFound
            }
        } catch (e: Exception) {
            LyricsState.NotFound
        }
    }

    private suspend fun fetchFromLyricsOvh(song: Song): LyricsState {
        return try {
            val response = lyricsOvhApiService.getLyrics(
                artist = cleanArtist(song.artist),
                title = cleanTitle(song.title)
            )
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

    private fun cleanArtist(artist: String) =
        artist.split(",", "&", "feat.", "ft.", "Feat.", "Ft.")
            .first().trim()

    private fun cleanTitle(title: String) =
        title.replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .trim()

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