package com.vibeup.android.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class RingtoneType {
    RINGTONE, NOTIFICATION, ALARM;

    fun toManagerType(): Int = when (this) {
        RINGTONE -> RingtoneManager.TYPE_RINGTONE
        NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
        ALARM -> RingtoneManager.TYPE_ALARM
    }
}

@Singleton
class RingtoneRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** WRITE_SETTINGS is a special permission — must be granted via system settings screen. */
    fun canWriteSettings(): Boolean =
        Settings.System.canWrite(context)

    /**
     * Copies the trimmed clip into shared storage as a ringtone/notification/alarm entry
     * and returns its MediaStore content Uri. Handles Android Q+ (RELATIVE_PATH, no raw
     * file access) and pre-Q (raw file into the public Ringtones dir) paths.
     */
    suspend fun saveToMediaStore(
        clip: File,
        displayName: String,
        type: RingtoneType
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val safeName = displayName.replace(Regex("[^a-zA-Z0-9 _-]"), "").take(60).ifBlank { "VibeUp Ringtone" }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val directory = when (type) {
                RingtoneType.RINGTONE -> Environment.DIRECTORY_RINGTONES
                RingtoneType.NOTIFICATION -> Environment.DIRECTORY_NOTIFICATIONS
                RingtoneType.ALARM -> Environment.DIRECTORY_ALARMS
            }

            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "$safeName.m4a")
                put(MediaStore.Audio.Media.TITLE, safeName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                put(MediaStore.Audio.Media.IS_RINGTONE, type == RingtoneType.RINGTONE)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, type == RingtoneType.NOTIFICATION)
                put(MediaStore.Audio.Media.IS_ALARM, type == RingtoneType.ALARM)
                put(MediaStore.Audio.Media.IS_MUSIC, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, directory)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                } else {
                    @Suppress("DEPRECATION")
                    val publicDir = Environment.getExternalStoragePublicDirectory(directory)
                        .apply { if (!exists()) mkdirs() }
                    val dest = File(publicDir, "$safeName.m4a")
                    put(MediaStore.Audio.Media.DATA, dest.absolutePath)
                }
            }

            // Remove any stale entry with the same display name to avoid duplicates.
            runCatching {
                resolver.delete(
                    collection,
                    "${MediaStore.Audio.Media.DISPLAY_NAME}=?",
                    arrayOf("$safeName.m4a")
                )
            }

            val itemUri = resolver.insert(collection, values)
                ?: return@withContext Result.failure(IllegalStateException("MediaStore insert failed"))

            resolver.openOutputStream(itemUri)?.use { output ->
                clip.inputStream().use { input -> input.copyTo(output) }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open output stream"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                resolver.update(itemUri, done, null, null)
            }

            Result.success(itemUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Sets the given MediaStore uri as the default ringtone/notification/alarm sound. */
    fun setAsDefault(type: RingtoneType, uri: Uri): Result<Unit> = try {
        RingtoneManager.setActualDefaultRingtoneUri(context, type.toManagerType(), uri)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Assigns the ringtone uri to a specific contact (from ACTION_PICK contact uri).
     * Requires WRITE_CONTACTS.
     */
    suspend fun setContactRingtone(contactUri: Uri, ringtoneUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val values = ContentValues().apply {
                    put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri.toString())
                }
                val updated = context.contentResolver.update(contactUri, values, null, null)
                if (updated > 0) Result.success(Unit)
                else Result.failure(IllegalStateException("No contact updated"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
