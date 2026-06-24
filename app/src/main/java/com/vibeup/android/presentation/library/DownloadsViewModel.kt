package com.vibeup.android.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.local.entity.DownloadedSong
import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.dto.toDomain
import com.vibeup.android.data.repository.DownloadRepository
import com.vibeup.android.data.repository.DownloadStatus
import com.vibeup.android.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val api: SaavnApiService
) : ViewModel() {

    val downloads = downloadRepository.getAllDownloads()
    val downloadProgress = downloadRepository.downloadProgress

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _totalSize = MutableStateFlow("0 MB")
    val totalSize: StateFlow<String> = _totalSize.asStateFlow()

    init {
        loadTotalSize()
    }

    private fun loadTotalSize() {
        viewModelScope.launch {
            val size = downloadRepository.getTotalSize()
            _totalSize.value = downloadRepository.formatFileSize(size)
        }
    }

    fun downloadSong(song: Song, quality: String) {
        viewModelScope.launch {
            // Check if already downloaded
            if (downloadRepository.isDownloaded(song.id)) {
                _message.value = "Already downloaded! ✅"
                return@launch
            }

            // Get the correct audio URL based on quality
            val audioUrl = getAudioUrlForQuality(song, quality)
            if (audioUrl == null) {
                _message.value = "Could not get download URL!"
                return@launch
            }

            _message.value = "Downloading ${song.title}..."

            downloadRepository.downloadSong(
                song = song,
                quality = quality,
                audioUrl = audioUrl,
                onProgress = {}
            ).onSuccess {
                _message.value = "${song.title} downloaded! ✅"
                loadTotalSize()
            }.onFailure {
                if (it.message != "Cancelled") {
                    _message.value = "Download failed!"
                }
            }
        }
    }

    private suspend fun getAudioUrlForQuality(
        song: Song,
        quality: String
    ): String? {
        return try {
            // Re-fetch song to get fresh download URLs
            val response = api.getSongById(song.id)
            val songData = response.data?.firstOrNull() ?: return null

            when (quality) {
                "320kbps" -> songData.downloadUrl
                    ?.find { it.quality == "320kbps" }?.url
                "160kbps" -> songData.downloadUrl
                    ?.find { it.quality == "160kbps" }?.url
                "96kbps" -> songData.downloadUrl
                    ?.find { it.quality == "96kbps" }?.url
                else -> songData.downloadUrl?.lastOrNull()?.url
            }
        } catch (e: Exception) {
            // Fallback to existing audioUrl
            song.audioUrl
        }
    }

    fun deleteDownload(songId: String) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(songId)
            _message.value = "Deleted!"
            loadTotalSize()
        }
    }

    fun cancelDownload(songId: String) {
        downloadRepository.cancelDownload(songId)
        _message.value = "Download cancelled"
    }

    fun clearMessage() {
        _message.value = null
    }

    fun isDownloading(songId: String): Boolean {
        val progress = downloadProgress.value[songId]
        return progress?.status == DownloadStatus.DOWNLOADING
    }

    fun toSong(downloaded: DownloadedSong) = Song(
        id = downloaded.id,
        title = downloaded.title,
        artist = downloaded.artist,
        album = downloaded.album,
        imageUrl = downloaded.imageUrl,
        audioUrl = "file://${downloaded.localPath}",
        duration = downloaded.duration,
        language = "local"
    )
}