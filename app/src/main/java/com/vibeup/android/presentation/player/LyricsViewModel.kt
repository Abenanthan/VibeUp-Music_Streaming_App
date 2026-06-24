package com.vibeup.android.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.remote.api.LyricsApiService
import com.vibeup.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val lyricsApiService: LyricsApiService
) : ViewModel() {

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private val _currentLineIndex = MutableStateFlow(0)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()

    private val _showSynced = MutableStateFlow(true)
    val showSynced: StateFlow<Boolean> = _showSynced.asStateFlow()

    var lastLoadedSongId: String? = null
        private set
    private var fetchJob: Job? = null

    fun loadLyrics(song: Song) {
        // ✅ Skip if same song already loaded/loading
        if (lastLoadedSongId == song.id &&
            _lyricsState.value !is LyricsState.Idle &&
            _lyricsState.value !is LyricsState.Error
        ) return

        lastLoadedSongId = song.id
        fetchJob?.cancel()
        _lyricsState.value = LyricsState.Loading
        _currentLineIndex.value = 0

        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // ✅ Try direct fetch first (faster)
                val response = lyricsApiService.getLyrics(
                    artistName = cleanArtistName(song.artist),
                    trackName = cleanTrackName(song.title),
                    albumName = song.album
                )

                when {
                    response.instrumental == true ->
                        _lyricsState.value = LyricsState.Instrumental

                    response.syncedLyrics != null -> {
                        val lines = parseSyncedLyrics(response.syncedLyrics)
                        _lyricsState.value = if (lines.isNotEmpty())
                            LyricsState.SyncedLoaded(lines)
                        else if (response.plainLyrics != null)
                            LyricsState.PlainLoaded(response.plainLyrics)
                        else
                            LyricsState.NotFound
                    }

                    response.plainLyrics != null ->
                        _lyricsState.value = LyricsState.PlainLoaded(
                            response.plainLyrics
                        )

                    else -> searchFallback(song)
                }
            } catch (e: Exception) {
                searchFallback(song)
            }
        }
    }

    private suspend fun searchFallback(song: Song) {
        try {
            val results = lyricsApiService.searchLyrics(
                artistName = cleanArtistName(song.artist),
                trackName = cleanTrackName(song.title)
            )
            val first = results.firstOrNull()
            when {
                first == null ->
                    _lyricsState.value = LyricsState.NotFound

                first.instrumental == true ->
                    _lyricsState.value = LyricsState.Instrumental

                first.syncedLyrics != null -> {
                    val lines = parseSyncedLyrics(first.syncedLyrics)
                    _lyricsState.value = if (lines.isNotEmpty())
                        LyricsState.SyncedLoaded(lines)
                    else if (first.plainLyrics != null)
                        LyricsState.PlainLoaded(first.plainLyrics)
                    else
                        LyricsState.NotFound
                }

                first.plainLyrics != null ->
                    _lyricsState.value = LyricsState.PlainLoaded(
                        first.plainLyrics
                    )

                else -> _lyricsState.value = LyricsState.NotFound
            }
        } catch (e: Exception) {
            _lyricsState.value = LyricsState.NotFound
        }
    }

    // ✅ Clean artist name — remove featuring artists
    private fun cleanArtistName(artist: String): String {
        return artist
            .split(",", "&", "feat.", "ft.", "Feat.", "Ft.")
            .first()
            .trim()
    }

    // ✅ Clean track name — remove extra info
    private fun cleanTrackName(title: String): String {
        return title
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .trim()
    }

    // ✅ Parse [mm:ss.xx] format
    private fun parseSyncedLyrics(synced: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        synced.lines().forEach { line ->
            val match = regex.find(line.trim()) ?: return@forEach
            val (min, sec, ms, text) = match.destructured
            val timeMs = (min.toLong() * 60 * 1000) +
                    (sec.toLong() * 1000) +
                    if (ms.length == 2) ms.toLong() * 10
                    else ms.toLong()
            val cleanText = text.trim()
            if (cleanText.isNotEmpty()) {
                lines.add(LyricLine(timeMs, cleanText))
            }
        }
        return lines.sortedBy { it.timeMs }
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