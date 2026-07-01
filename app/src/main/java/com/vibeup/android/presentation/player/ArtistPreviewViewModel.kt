package com.vibeup.android.presentation.player

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

@HiltViewModel
class ArtistPreviewViewModel @Inject constructor(
    private val saavnApiService: SaavnApiService
) : ViewModel() {

    private val _artist = MutableStateFlow<Artist?>(null)
    val artist: StateFlow<Artist?> = _artist.asStateFlow()

    private var lastLoadedArtistId: String? = null

    fun loadArtistPreview(artistId: String) {
        if (artistId.isBlank() || artistId == lastLoadedArtistId) return
        lastLoadedArtistId = artistId

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    saavnApiService.getArtistDetails(artistId = artistId, songCount = 7)
                }

                val fallbackSongs = if (response.data?.topSongs.isNullOrEmpty()) {
                    val artistName = response.data?.name ?: ""
                    if (artistName.isNotBlank()) {
                        try {
                            withContext(Dispatchers.IO) {
                                saavnApiService.searchSongs(artistName, limit = 7)
                                    .data?.results?.map { it.songToDomain() } ?: emptyList()
                            }
                        } catch (e: Exception) { emptyList() }
                    } else emptyList()
                } else emptyList()

                _artist.value = response.toDomain(fallbackSongs = fallbackSongs)
            } catch (e: Exception) {
                android.util.Log.e("ArtistPreviewVM", "Failed: ${e.message}")
                _artist.value = null
            }
        }
    }
}