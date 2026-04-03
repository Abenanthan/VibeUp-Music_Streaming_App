package com.vibeup.android.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.vibeup.android.domain.model.LocalSong
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMusicScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scanLocalSongs(): List<LocalSong> {
        val songs = mutableListOf<LocalSong>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Filter only supported formats
        val selection = "${MediaStore.Audio.Media.MIME_TYPE} IN (?,?,?)" +
                " AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(
            "audio/mpeg",   // mp3
            "audio/flac",   // flac
            "audio/mp4",    // m4a
            "30000"         // minimum 30 seconds
        )

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media._ID
            )
            val titleColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.TITLE
            )
            val artistColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ARTIST
            )
            val albumColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ALBUM
            )
            val durationColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.DURATION
            )
            val dataColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.DATA
            )
            val albumIdColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ALBUM_ID
            )
            val sizeColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.SIZE
            )
            val mimeColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.MIME_TYPE
            )

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val albumId = cursor.getLong(albumIdColumn)

                // Get album art URI
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                // Get file URI for playback
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                val mimeType = cursor.getString(mimeColumn)
                val format = when (mimeType) {
                    "audio/mpeg" -> "MP3"
                    "audio/flac" -> "FLAC"
                    "audio/mp4"  -> "M4A"
                    else         -> "Audio"
                }

                songs.add(
                    LocalSong(
                        id = id,
                        title = cursor.getString(titleColumn)
                            ?: "Unknown",
                        artist = cursor.getString(artistColumn)
                            ?: "Unknown Artist",
                        album = cursor.getString(albumColumn)
                            ?: "Unknown Album",
                        duration = cursor.getLong(durationColumn),
                        path = contentUri,
                        albumArtUri = albumArtUri,
                        size = cursor.getLong(sizeColumn),
                        format = format
                    )
                )
            }
        }

        return songs
    }
}