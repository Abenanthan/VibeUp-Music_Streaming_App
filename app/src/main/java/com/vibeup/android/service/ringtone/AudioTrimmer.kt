package com.vibeup.android.service.ringtone

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Clips a slice out of an audio file entirely in-app using Media3 Transformer.
 * The output keeps the source's AAC audio (audio-only export, no video), which is a
 * perfectly valid ringtone format and avoids a costly re-encode.
 */
@Singleton
class AudioTrimmer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * @param sourcePath absolute path or content/file uri string of the full audio.
     * @param startMs    clip start, milliseconds.
     * @param endMs      clip end, milliseconds.
     * @return the trimmed file written into the app cache.
     */
    suspend fun trim(
        sourcePath: String,
        startMs: Long,
        endMs: Long
    ): Result<File> = withContext(Dispatchers.Main) {
        // Transformer must be built and started on a thread with a Looper (main).
        val outputDir = File(context.cacheDir, "ringtones").apply { if (!exists()) mkdirs() }
        val outputFile = File(outputDir, "ringtone_${startMs}_${endMs}_${sourcePath.hashCode()}.m4a")
        if (outputFile.exists()) outputFile.delete()

        val uri = if (sourcePath.startsWith("content://") || sourcePath.startsWith("file://")) {
            sourcePath
        } else {
            "file://$sourcePath"
        }

        val clippedItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()

        // Drop the video track (if any) — ringtones are audio-only.
        val editedItem = EditedMediaItem.Builder(clippedItem)
            .setRemoveVideo(true)
            .build()

        try {
            awaitExport(editedItem, outputFile.absolutePath)
            if (outputFile.exists() && outputFile.length() > 0) {
                Result.success(outputFile)
            } else {
                Result.failure(IllegalStateException("Trim produced an empty file"))
            }
        } catch (e: Exception) {
            outputFile.delete()
            Result.failure(e)
        }
    }

    private suspend fun awaitExport(
        editedItem: EditedMediaItem,
        outputPath: String
    ): ExportResult = suspendCancellableCoroutine { cont ->
        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    if (cont.isActive) cont.resume(result)
                }

                override fun onError(
                    composition: Composition,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            })
            .build()

        transformer.start(editedItem, outputPath)

        cont.invokeOnCancellation {
            runCatching { transformer.cancel() }
        }
    }
}
