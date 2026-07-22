package com.vibeup.android.presentation.ringtone

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.repository.DownloadRepository
import com.vibeup.android.data.repository.RingtoneRepository
import com.vibeup.android.data.repository.RingtoneType
import com.vibeup.android.domain.model.Song
import com.vibeup.android.service.ringtone.AudioTrimmer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

/** What stage the ringtone flow is in — drives the trimmer UI. */
sealed interface RingtoneStage {
    data object Loading : RingtoneStage                       // resolving / downloading source
    data class Ready(val sourcePath: String) : RingtoneStage  // clip can be picked & previewed
    data class Working(val label: String) : RingtoneStage     // trimming / saving / assigning
    data class Done(val ringtoneUri: Uri) : RingtoneStage     // finished, may still assign contact
    data class Failed(val message: String) : RingtoneStage
}

data class RingtoneUiState(
    val song: Song? = null,
    val durationMs: Long = 0L,
    val stage: RingtoneStage = RingtoneStage.Loading,
    val toast: String? = null
)

@HiltViewModel
class RingtoneViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val holder: RingtoneRequestHolder,
    private val downloadRepository: DownloadRepository,
    private val ringtoneRepository: RingtoneRepository,
    private val audioTrimmer: AudioTrimmer,
    private val api: SaavnApiService,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _state = MutableStateFlow(RingtoneUiState())
    val state: StateFlow<RingtoneUiState> = _state.asStateFlow()

    fun start() {
        val song = holder.consume() ?: run {
            _state.value = _state.value.copy(stage = RingtoneStage.Failed("No song selected"))
            return
        }
        _state.value = RingtoneUiState(
            song = song,
            durationMs = (song.duration.coerceAtLeast(0)).toLong() * 1000L,
            stage = RingtoneStage.Loading
        )
        prepareSource(song)
    }

    private fun prepareSource(song: Song) {
        viewModelScope.launch {
            // Already downloaded? Use the local file directly.
            val existing = downloadRepository.getDownload(song.id)
            if (existing != null && File(existing.localPath).exists()) {
                _state.value = _state.value.copy(stage = RingtoneStage.Ready(existing.localPath))
                return@launch
            }

            // Otherwise fetch a fresh stream URL and cache it.
            val url = resolveAudioUrl(song)
            if (url == null) {
                _state.value = _state.value.copy(stage = RingtoneStage.Failed("Couldn't get the audio for this song"))
                return@launch
            }
            val cached = downloadToCache(song.id, url)
            cached.onSuccess { file ->
                _state.value = _state.value.copy(stage = RingtoneStage.Ready(file.absolutePath))
            }.onFailure {
                _state.value = _state.value.copy(stage = RingtoneStage.Failed("Download failed: ${it.message}"))
            }
        }
    }

    private suspend fun resolveAudioUrl(song: Song): String? = withContext(Dispatchers.IO) {
        try {
            val response = api.getSongById(song.id)
            val data = response.data?.firstOrNull()
            // Prefer a mid quality for a snappy ringtone download.
            data?.downloadUrl?.find { it.quality == "160kbps" }?.url
                ?: data?.downloadUrl?.lastOrNull()?.url
                ?: song.audioUrl.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            song.audioUrl.takeIf { it.isNotBlank() }
        }
    }

    private suspend fun downloadToCache(songId: String, url: String): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "ringtone_src").apply { if (!exists()) mkdirs() }
                val file = File(dir, "src_$songId.mp4")
                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("HTTP ${response.code}"))
                    }
                    val body = response.body ?: return@withContext Result.failure(IllegalStateException("Empty body"))
                    body.byteStream().use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun canWriteSettings(): Boolean = ringtoneRepository.canWriteSettings()

    /**
     * Trim the selected slice, publish it to MediaStore, set it as the chosen default and
     * — if a contact was picked — assign it to that contact too. All in one flow.
     * Caller must ensure WRITE_SETTINGS is granted first.
     */
    fun applyRingtone(
        sourcePath: String,
        startMs: Long,
        endMs: Long,
        type: RingtoneType,
        contactUri: Uri? = null
    ) {
        val song = _state.value.song ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(stage = RingtoneStage.Working("Trimming clip…"))

            val trimmed = audioTrimmer.trim(sourcePath, startMs, endMs)
            val clip = trimmed.getOrElse {
                _state.value = _state.value.copy(stage = RingtoneStage.Failed("Couldn't trim clip: ${it.message}"))
                return@launch
            }

            _state.value = _state.value.copy(stage = RingtoneStage.Working("Saving ringtone…"))
            val title = "${song.title} - ${song.artist}"
            val saved = ringtoneRepository.saveToMediaStore(clip, title, type)
            val uri = saved.getOrElse {
                _state.value = _state.value.copy(stage = RingtoneStage.Failed("Couldn't save ringtone: ${it.message}"))
                return@launch
            }

            _state.value = _state.value.copy(stage = RingtoneStage.Working("Applying…"))
            val setResult = ringtoneRepository.setAsDefault(type, uri)
            if (setResult.isFailure) {
                _state.value = _state.value.copy(
                    stage = RingtoneStage.Failed("Couldn't apply: ${setResult.exceptionOrNull()?.message}")
                )
                return@launch
            }

            val label = when (type) {
                RingtoneType.RINGTONE -> "ringtone"
                RingtoneType.NOTIFICATION -> "notification sound"
                RingtoneType.ALARM -> "alarm sound"
            }

            // Optional contact assignment as part of the same apply.
            var toastMsg = "Set as $label ✅"
            if (contactUri != null) {
                _state.value = _state.value.copy(stage = RingtoneStage.Working("Assigning to contact…"))
                val c = ringtoneRepository.setContactRingtone(contactUri, uri)
                toastMsg = if (c.isSuccess) "$label + contact set ✅"
                           else "Ringtone set, but contact assignment failed"
            }

            _state.value = _state.value.copy(stage = RingtoneStage.Done(uri), toast = toastMsg)
        }
    }

    fun clearToast() {
        _state.value = _state.value.copy(toast = null)
    }
}
