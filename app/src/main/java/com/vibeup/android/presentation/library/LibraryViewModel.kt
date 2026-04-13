package com.vibeup.android.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.domain.model.Playlist
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder {
    DATE_ADDED, ALPHABETICAL, ARTIST, DURATION
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    init {
        loadLikedSongs()
        loadPlaylists()
    }

    private fun loadLikedSongs() {
        viewModelScope.launch {
            libraryRepository.getLikedSongs().collect {
                _likedSongs.value = it
            }
        }
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            libraryRepository.getPlaylists().collect {
                _playlists.value = it
            }
        }
    }

    fun createPlaylist(name: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                libraryRepository.createPlaylist(name, description)
                _message.value = "Playlist '$name' created! ✅"
            } catch (e: Exception) {
                _message.value = "Failed to create playlist!"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            try {
                libraryRepository.renamePlaylist(playlistId, newName)
                _message.value = "Playlist renamed! ✅"
            } catch (e: Exception) {
                _message.value = "Failed to rename playlist!"
            }
        }
    }

    fun likeSong(song: Song) {
        viewModelScope.launch {
            try {
                libraryRepository.likeSong(song)
                _message.value = "Added to Liked Songs! 💚"
            } catch (e: Exception) {
                _message.value = "Failed to like song!"
            }
        }
    }

    fun unlikeSong(songId: String) {
        viewModelScope.launch {
            try {
                libraryRepository.unlikeSong(songId)
                _message.value = "Removed from Liked Songs!"
            } catch (e: Exception) {
                _message.value = "Failed to unlike song!"
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            try {
                libraryRepository.addSongToPlaylist(playlistId, song)
                _message.value = "Song added to playlist! ✅"
            } catch (e: Exception) {
                _message.value = "Failed to add song!"
            }
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            try {
                libraryRepository.removeSongFromPlaylist(playlistId, songId)
                _message.value = "Song removed!"
            } catch (e: Exception) {
                _message.value = "Failed to remove song!"
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                libraryRepository.deletePlaylist(playlistId)
                _message.value = "Playlist deleted!"
            } catch (e: Exception) {
                _message.value = "Failed to delete playlist!"
            }
        }
    }

    fun getPlaylistSongs(playlistId: String) =
        libraryRepository.getPlaylistSongs(playlistId)

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun clearMessage() {
        _message.value = null
    }
}