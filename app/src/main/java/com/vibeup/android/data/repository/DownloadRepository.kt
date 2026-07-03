package com.vibeup.android.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.vibeup.android.data.local.dao.DownloadDao
import com.vibeup.android.data.local.entity.DownloadedSong
import com.vibeup.android.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgress(
    val songId: String,
    val progress: Int, // 0-100
    val status: DownloadStatus
)

enum class DownloadStatus {
    DOWNLOADING, COMPLETED, FAILED, CANCELLED
}

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient,
    private val auth: FirebaseAuth
) {
    private val uid get() = auth.currentUser?.uid ?: "guest_user"

    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> =
        _downloadProgress.asStateFlow()

    private val activeDownloads = mutableMapOf<String, Boolean>()

    fun getAllDownloads() = downloadDao.getAllDownloads(uid)

    suspend fun isDownloaded(songId: String) = downloadDao.isDownloaded(songId, uid)

    suspend fun getDownload(songId: String) = downloadDao.getDownload(songId, uid)

    suspend fun getTotalSize() = downloadDao.getTotalSize(uid) ?: 0L

    suspend fun downloadSong(
        song: Song,
        quality: String,
        audioUrl: String,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val songId = song.id
        activeDownloads[songId] = true

        updateProgress(songId, 0, DownloadStatus.DOWNLOADING)

        try {
            // Create downloads directory
            val downloadsDir = File(context.filesDir, "downloads")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val fileName = "${songId}_${quality}.mp4"
            val file = File(downloadsDir, fileName)

            val request = Request.Builder()
                .url(audioUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                updateProgress(songId, 0, DownloadStatus.FAILED)
                return@withContext Result.failure(
                    Exception("Download failed: ${response.code}")
                )
            }

            val body = response.body
                ?: return@withContext Result.failure(
                    Exception("Empty response")
                )

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // Check if cancelled
                        if (activeDownloads[songId] == false) {
                            file.delete()
                            updateProgress(songId, 0, DownloadStatus.CANCELLED)
                            return@withContext Result.failure(
                                Exception("Cancelled")
                            )
                        }

                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        if (totalBytes > 0) {
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                            updateProgress(songId, progress, DownloadStatus.DOWNLOADING)
                            onProgress(progress)
                        }
                    }
                }
            }

            // Save to DB
            val downloadedSong = DownloadedSong(
                id = songId,
                userId = uid,
                title = song.title,
                artist = song.artist,
                album = song.album,
                imageUrl = song.imageUrl,
                localPath = file.absolutePath,
                duration = song.duration,
                fileSize = file.length(),
                quality = quality
            )
            downloadDao.insertDownload(downloadedSong)
            updateProgress(songId, 100, DownloadStatus.COMPLETED)
            activeDownloads.remove(songId)

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("Download", "Error: ${e.message}")
            updateProgress(songId, 0, DownloadStatus.FAILED)
            activeDownloads.remove(songId)
            Result.failure(e)
        }
    }

    suspend fun deleteDownload(songId: String) {
        val download = downloadDao.getDownload(songId, uid)
        download?.let {
            File(it.localPath).delete()
        }
        downloadDao.deleteDownload(songId, uid)
        val current = _downloadProgress.value.toMutableMap()
        current.remove(songId)
        _downloadProgress.value = current
    }

    fun cancelDownload(songId: String) {
        activeDownloads[songId] = false
    }

    private fun updateProgress(
        songId: String,
        progress: Int,
        status: DownloadStatus
    ) {
        val current = _downloadProgress.value.toMutableMap()
        current[songId] = DownloadProgress(songId, progress, status)
        _downloadProgress.value = current
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
            else -> "$bytes B"
        }
    }

    companion object {
        fun formatSizeStatic(bytes: Long): String {
            return when {
                bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
                bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
                else -> "$bytes B"
            }
        }
    }
}