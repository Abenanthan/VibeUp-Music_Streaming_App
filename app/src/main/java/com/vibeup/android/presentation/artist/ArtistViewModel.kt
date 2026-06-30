package com.vibeup.android.presentation.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.dto.toDomain as songToDomain
import com.vibeup.android.data.remote.mapper.toDomain
import com.vibeup.android.domain.model.Artist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class ArtistUiState {
    object Loading : ArtistUiState()
    data class Success(val artist: Artist) : ArtistUiState()
    data class Error(val message: String) : ArtistUiState()
}

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val saavnApiService: SaavnApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = savedStateHandle.get<String>("artistId") ?: ""

    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init {
        loadArtist()
    }

    fun loadArtist() {
        if (artistId.isBlank()) {
            _uiState.value = ArtistUiState.Error("Artist not found")
            return
        }
        viewModelScope.launch {
            _uiState.value = ArtistUiState.Loading
            try {
                val response = withContext(Dispatchers.IO) {
                    saavnApiService.getArtistDetails(artistId = artistId)
                }

                android.util.Log.d("ArtistViewModel", "Artist name: ${response.data?.name}")

                // If wrapper returned no songs, fetch via search as fallback
                val fallbackSongs = if (response.data?.topSongs.isNullOrEmpty()) {
                    val artistName = response.data?.name ?: ""
                    if (artistName.isNotBlank()) {
                        try {
                            withContext(Dispatchers.IO) {
                                saavnApiService.searchSongs(artistName, limit = 20)
                                    .data?.results?.map { it.songToDomain() } ?: emptyList()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ArtistViewModel", "Fallback search failed: ${e.message}")
                            emptyList()
                        }
                    } else emptyList()
                } else emptyList()

                val artist = response.toDomain(fallbackSongs = fallbackSongs)
                _uiState.value = ArtistUiState.Success(artist)
            } catch (e: Exception) {
                android.util.Log.e("ArtistViewModel", "loadArtist failed: ${e.message}", e)
                _uiState.value = ArtistUiState.Error("Couldn't load artist")
            }
        }
    }
}