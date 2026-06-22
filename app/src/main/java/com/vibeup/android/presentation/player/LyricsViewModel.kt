package com.vibeup.android.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.remote.api.LyricsApiService
import com.vibeup.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

    private var lastSong: Song? = null

    fun loadLyrics(song: Song) {
        if (lastSong?.id == song.id) return
        lastSong = song
        _lyricsState.value = LyricsState.Loading
        _currentLineIndex.value = 0

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try exact match first
                val response = lyricsApiService.getLyrics(
                    artistName = song.artist,
                    trackName = song.title,
                    albumName = song.album
                )

                when {
                    response.instrumental == true -> {
                        _lyricsState.value = LyricsState.Instrumental
                    }
                    response.syncedLyrics != null -> {
                        val lines = parseSyncedLyrics(response.syncedLyrics)
                        if (lines.isNotEmpty()) {
                            _lyricsState.value = LyricsState.SyncedLoaded(lines)
                        } else if (response.plainLyrics != null) {
                            _lyricsState.value = LyricsState.PlainLoaded(
                                response.plainLyrics
                            )
                        } else {
                            _lyricsState.value = LyricsState.NotFound
                        }
                    }
                    response.plainLyrics != null -> {
                        _lyricsState.value = LyricsState.PlainLoaded(
                            response.plainLyrics
                        )
                    }
                    else -> {
                        // Try search as fallback
                        searchLyrics(song)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LyricsVM", "Error: ${e.message}")
                // Try search as fallback
                searchLyrics(song)
            }
        }
    }

    private suspend fun searchLyrics(song: Song) {
        try {
            val results = lyricsApiService.searchLyrics(
                artistName = song.artist,
                trackName = song.title
            )
            val first = results.firstOrNull()
            when {
                first == null -> {
                    _lyricsState.value = LyricsState.NotFound
                }
                first.instrumental == true -> {
                    _lyricsState.value = LyricsState.Instrumental
                }
                first.syncedLyrics != null -> {
                    val lines = parseSyncedLyrics(first.syncedLyrics)
                    _lyricsState.value = if (lines.isNotEmpty())
                        LyricsState.SyncedLoaded(lines)
                    else if (first.plainLyrics != null)
                        LyricsState.PlainLoaded(first.plainLyrics)
                    else
                        LyricsState.NotFound
                }
                first.plainLyrics != null -> {
                    _lyricsState.value = LyricsState.PlainLoaded(
                        first.plainLyrics
                    )
                }
                else -> {
                    _lyricsState.value = LyricsState.NotFound
                }
            }
        } catch (e: Exception) {
            _lyricsState.value = LyricsState.NotFound
        }
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
                    (if (ms.length == 2) ms.toLong() * 10
                    else ms.toLong())
            val cleanText = text.trim()
            if (cleanText.isNotEmpty()) {
                lines.add(LyricLine(timeMs, cleanText))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    // ✅ Update current line based on playback position
    fun updateCurrentLine(positionMs: Long) {
        val state = _lyricsState.value
        if (state !is LyricsState.SyncedLoaded) return

        val lines = state.lines
        var index = 0
        for (i in lines.indices) {
            if (positionMs >= lines[i].timeMs) {
                index = i
            } else {
                break
            }
        }
        if (_currentLineIndex.value != index) {
            _currentLineIndex.value = index
        }
    }

    fun toggleDisplayMode() {
        _showSynced.value = !_showSynced.value
    }

    fun resetLyrics() {
        _lyricsState.value = LyricsState.Idle
        lastSong = null
    }
}