package com.vibeup.android.domain.model

data class LocalSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumArtUri: String,
    val size: Long,
    val format: String
) {
    // Convert to Song for unified playback
    fun toSong() = Song(
        id = id.toString(),
        title = title,
        artist = artist,
        album = album,
        duration = (duration / 1000).toInt(),
        imageUrl = albumArtUri,
        audioUrl = path,
        language = "local"
    )
}

