package com.vibeup.android.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.dto.toDomain
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

    private val favouriteIds = listOf(
        "GWwnRe0u", "rjkrTnma", "m1iXOUID", "mPTrDSun",
        "__YIeFT-", "uP7MlTHz", "eLm-JvK4", "SM-rvz75",
        "qcVqPqk5", "vRNpPA7_", "yBmo2qWU", "QWLY3Ls_",
        "QkFUdVod", "BH07HVc8", "kehuVn2F"
    )

    private val _favouriteSongs = MutableStateFlow<List<Song>>(emptyList())
    val favouriteSongs: StateFlow<List<Song>> = _favouriteSongs.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<Song>>(emptyList())
    val trendingSongs: StateFlow<List<Song>> = _trendingSongs.asStateFlow()

    private val _tamilSongs = MutableStateFlow<List<Song>>(emptyList())
    val tamilSongs: StateFlow<List<Song>> = _tamilSongs.asStateFlow()

    private val _teluguSongs = MutableStateFlow<List<Song>>(emptyList())
    val teluguSongs: StateFlow<List<Song>> = _teluguSongs.asStateFlow()

    private val _hindiSongs = MutableStateFlow<List<Song>>(emptyList())
    val hindiSongs: StateFlow<List<Song>> = _hindiSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAll()
        observeRecentlyPlayed()
    }

    private fun observeRecentlyPlayed() {
        viewModelScope.launch {
            try {
                libraryRepository.getRecentlyPlayed().collect { songs ->
                    _recentlyPlayed.value = songs.take(15)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Recently played: ${e.message}")
            }
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {

                withContext(Dispatchers.IO) {
                    loadFavourites()
                }
                _isLoading.value = false


                withContext(Dispatchers.IO) {
                    val t = async { loadTrending() }
                    val ta = async { loadLanguageSongs("tamil") }
                    val te = async { loadLanguageSongs("telugu") }
                    val hi = async { loadLanguageSongs("hindi") }
                    t.await()
                    ta.await()
                    te.await()
                    hi.await()
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
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
            val tamil = api.searchSongs(
                "trending tamil songs 2024", limit = 5
            ).data?.results?.map { it.toDomain() } ?: emptyList()

            val telugu = api.searchSongs(
                "trending telugu songs 2024", limit = 5
            ).data?.results?.map { it.toDomain() } ?: emptyList()

            val hindi = api.searchSongs(
                "trending hindi songs 2024", limit = 5
            ).data?.results?.map { it.toDomain() } ?: emptyList()

            _trendingSongs.value = (tamil + telugu + hindi).shuffled()
        } catch (e: Exception) {
            android.util.Log.e("HomeVM", "Trending: ${e.message}")
        }
    }

    private suspend fun loadLanguageSongs(language: String) {
        try {
            val response = api.searchSongs(
                "$language hits 2024",
                limit = 15
            )
            val songs = response.data?.results
                ?.map { it.toDomain() } ?: emptyList()
            when (language) {
                "tamil"  -> _tamilSongs.value = songs
                "telugu" -> _teluguSongs.value = songs
                "hindi"  -> _hindiSongs.value = songs
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeVM", "$language: ${e.message}")
        }
    }

    fun refresh() = loadAll()
}