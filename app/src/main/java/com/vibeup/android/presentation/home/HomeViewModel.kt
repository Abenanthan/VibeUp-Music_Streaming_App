package com.vibeup.android.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.dto.toDomain
import com.vibeup.android.domain.model.Playlist
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: SaavnApiService,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val favouriteIds = listOf( //2 4
        "GWwnRe0u", "cDHlLKvW", "m1iXOUID", "_KjTxjcC",
        "__YIeFT-", "uP7MlTHz", "eLm-JvK4", "SM-rvz75",
        "qcVqPqk5", "vRNpPA7_", "yBmo2qWU", "QWLY3Ls_",
        "QkFUdVod", "BH07HVc8", "kehuVn2F"
    )

    // Wave 1
    private val _favouriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favouriteSongs: StateFlow<List<Song>> = _favouriteSongs.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Wave 2
    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()

    // Wave 3
    private val _newReleases = MutableStateFlow<List<Song>>(emptyList())
    val newReleases: StateFlow<List<Song>> = _newReleases.asStateFlow()

    private val _romanticSongs = MutableStateFlow<List<Song>>(emptyList())
    val romanticSongs: StateFlow<List<Song>> = _romanticSongs.asStateFlow()

    private val _partySongs = MutableStateFlow<List<Song>>(emptyList())
    val partySongs: StateFlow<List<Song>> = _partySongs.asStateFlow()

    private val _chillSongs = MutableStateFlow<List<Song>>(emptyList())
    val chillSongs: StateFlow<List<Song>> = _chillSongs.asStateFlow()

    private val _sadSongs = MutableStateFlow<List<Song>>(emptyList())
    val sadSongs: StateFlow<List<Song>> = _sadSongs.asStateFlow()

    private val _arRahmanSongs = MutableStateFlow<List<Song>>(emptyList())
    val arRahmanSongs: StateFlow<List<Song>> = _arRahmanSongs.asStateFlow()

    private val _anirudhSongs = MutableStateFlow<List<Song>>(emptyList())
    val anirudhSongs: StateFlow<List<Song>> = _anirudhSongs.asStateFlow()

    private val _sidSriramSongs = MutableStateFlow<List<Song>>(emptyList())
    val sidSriramSongs: StateFlow<List<Song>> = _sidSriramSongs.asStateFlow()

    private val _arijitSongs = MutableStateFlow<List<Song>>(emptyList())
    val arijitSongs: StateFlow<List<Song>> = _arijitSongs.asStateFlow()

    private val _gvPrakashSongs = MutableStateFlow<List<Song>>(emptyList())
    val gvPrakashSongs: StateFlow<List<Song>> = _gvPrakashSongs.asStateFlow()

    private val _hipHopSongs = MutableStateFlow<List<Song>>(emptyList())
    val hipHopSongs: StateFlow<List<Song>> = _hipHopSongs.asStateFlow()

    private val _tamilSongs = MutableStateFlow<List<Song>>(emptyList())
    val tamilSongs: StateFlow<List<Song>> = _tamilSongs.asStateFlow()

    private val _teluguSongs = MutableStateFlow<List<Song>>(emptyList())
    val teluguSongs: StateFlow<List<Song>> = _teluguSongs.asStateFlow()

    private val _hindiSongs = MutableStateFlow<List<Song>>(emptyList())
    val hindiSongs: StateFlow<List<Song>> = _hindiSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        observeRecentlyPlayed()
        observePlaylists()
        loadWave1()
    }

    private fun observeRecentlyPlayed() {
        viewModelScope.launch {
            try {
                libraryRepository.getRecentlyPlayed().collect {
                    _recentlyPlayed.value = it.take(15)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "RecentlyPlayed: ${e.message}")
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            try {
                libraryRepository.getPlaylists().collect {
                    _playlists.value = it.take(4)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Playlists: ${e.message}")
            }
        }
    }

    private fun loadWave1() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                withContext(Dispatchers.IO) {
                    loadFavourites()
                }
                _isLoading.value = false
                // Load wave 2 in background
                loadWave2()
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun loadWave2() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val t = async { loadTrending() }
                val n = async { loadNewReleases() }
                t.await()
                n.await()
                // Load wave 3 after wave 2
                loadWave3()
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Wave2: ${e.message}")
            }
        }
    }

    private fun loadWave3() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val r = async { loadMoodSongs("romantic love songs", _romanticSongs) }
                val p = async { loadMoodSongs("party dance songs", _partySongs) }
                val c = async { loadMoodSongs("chill relaxing songs", _chillSongs) }
                val s = async { loadMoodSongs("sad emotional songs", _sadSongs) }
                val ar = async { loadArtistSongs("AR Rahman", _arRahmanSongs) }
                val an = async { loadArtistSongs("Anirudh Ravichander", _anirudhSongs) }
                val si = async { loadArtistSongs("Sid Sriram", _sidSriramSongs) }
                val aj = async { loadArtistSongs("Arijit Singh", _arijitSongs) }
                val gv = async { loadArtistSongs("GV Prakash", _gvPrakashSongs) }
                val hh = async { loadArtistSongs("Hiphop Tamizha", _hipHopSongs) }
                val ta = async { loadLanguageSongs("tamil") }
                val te = async { loadLanguageSongs("telugu") }
                val hi = async { loadLanguageSongs("hindi") }
                r.await(); p.await(); c.await(); s.await()
                ar.await(); an.await(); si.await(); aj.await()
                gv.await(); hh.await()
                ta.await(); te.await(); hi.await()
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Wave3: ${e.message}")
            }
        }
    }

    private suspend fun loadFavourites() {
        try {
            val allIds = favouriteIds.joinToString(",")
            val response = api.getSongById(allIds)
            _favouriteSongs.value = response.data
                ?.map { it.toDomain() } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("HomeVM", "Favourites: ${e.message}")
        }
    }

    private suspend fun loadTrending() {
        try {
            val result = api.searchSongs(
                "trending songs 2025", limit = 30
            ).data?.results?.map { it.toDomain() } ?: emptyList()
            _trendingSongs.value = deduplicate(result)
        } catch (e: Exception) {
            android.util.Log.e("HomeVM", "Trending: ${e.message}")
        }
    }

    private suspend fun loadNewReleases() {
        try {
            // ✅ Better query for new releases
            val tamil = api.searchSongs(
                "new tamil songs 2025", limit = 10
            ).data?.results?.map { it.toDomain() } ?: emptyList()

            val hindi = api.searchSongs(
                "new hindi songs 2025", limit = 10
            ).data?.results?.map { it.toDomain() } ?: emptyList()

            val telugu = api.searchSongs(
                "new telugu songs 2025", limit = 10
            ).data?.results?.map { it.toDomain() } ?: emptyList()

            _newReleases.value = deduplicate((tamil + hindi + telugu).shuffled(), limit = 15)
        } catch (e: Exception) {
            android.util.Log.e("HomeVM", "NewReleases: ${e.message}")
        }
    }

    private suspend fun loadMoodSongs(
        query: String,
        state: MutableStateFlow<List<Song>>
    ) {
        try {
            val result = api.searchSongs(query, limit = 30)
                .data?.results?.map { it.toDomain() } ?: emptyList()
            state.value = deduplicate(result)
        } catch (e: Exception) {
            android.util.Log.e("HomeVM", "Mood: ${e.message}")
        }
    }

    private suspend fun loadArtistSongs(
        artist: String,
        state: MutableStateFlow<List<Song>>
    ) {
        try {
            val result = api.searchSongs(artist, limit = 30)
                .data?.results?.map { it.toDomain() } ?: emptyList()
            state.value = deduplicate(result)
        } catch (e: Exception) {
            android.util.Log.e("HomeVM", "Artist: ${e.message}")
        }
    }

    private suspend fun loadLanguageSongs(language: String) {
        try {
            val result = api.searchSongs(
                "$language hits 2025", limit = 30
            ).data?.results?.map { it.toDomain() } ?: emptyList()

            val unique = deduplicate(result)

            when (language) {
                "tamil"  -> _tamilSongs.value = unique
                "telugu" -> _teluguSongs.value = unique
                "hindi"  -> _hindiSongs.value = unique
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeVM", "$language: ${e.message}")
        }
    }

    // ✅ Reusable dedup function
    private fun deduplicate(songs: List<Song>, limit: Int = 12): List<Song> {
        val seen = mutableSetOf<String>()
        val unique = mutableListOf<Song>()
        for (song in songs) {
            val key = buildString {
                append(
                    song.title
                        .lowercase()
                        .replace(Regex("[^a-z0-9]"), "")
                        .take(10)
                )
                append(
                    song.artist
                        .lowercase()
                        .split(",", "&", "feat", "ft")
                        .first()
                        .replace(Regex("[^a-z0-9]"), "")
                        .take(8)
                )
            }
            if (seen.add(key)) unique.add(song)
            if (unique.size >= limit) break
        }
        return unique
    }

    fun refresh() {
        _favouriteSongs.value = emptyList()
        _trendingSongs.value = emptyList()
        _newReleases.value = emptyList()
        loadWave1()
    }
}