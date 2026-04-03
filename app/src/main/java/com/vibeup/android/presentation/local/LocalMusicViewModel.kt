package com.vibeup.android.presentation.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.local.LocalMusicScanner
import com.vibeup.android.domain.model.LocalSong
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    private val localMusicScanner: LocalMusicScanner,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _localSongs = MutableStateFlow<List<LocalSong>>(emptyList())
    val localSongs: StateFlow<List<LocalSong>> = _localSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredSongs = MutableStateFlow<List<LocalSong>>(emptyList())
    val filteredSongs: StateFlow<List<LocalSong>> = _filteredSongs.asStateFlow()

    init {
        checkPermissionAndScan()
    }

    fun checkPermissionAndScan() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val granted = ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED

        android.util.Log.d("LocalMusic", "Permission granted: $granted")
        _hasPermission.value = granted

        if (granted) {
            scanSongs()
        }
    }

    fun onPermissionGranted() {
        android.util.Log.d("LocalMusic", "Permission just granted!")
        _hasPermission.value = true
        scanSongs()
    }

    fun scanSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                android.util.Log.d("LocalMusic", "Scanning songs...")
                val songs = localMusicScanner.scanLocalSongs()
                android.util.Log.d("LocalMusic", "Found ${songs.size} songs")
                _localSongs.value = songs
                _filteredSongs.value = songs
            } catch (e: Exception) {
                android.util.Log.e("LocalMusic", "Scan error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _filteredSongs.value = if (query.isEmpty()) {
            _localSongs.value
        } else {
            _localSongs.value.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
            }
        }
    }
}