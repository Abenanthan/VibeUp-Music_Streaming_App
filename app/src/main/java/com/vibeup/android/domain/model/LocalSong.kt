package com.vibeup.android.domain.model

import androidx.compose.runtime.Immutable

// @Immutable: these are value-style data classes whose properties are all vals and
// are never mutated after construction. Without it the Compose compiler infers them
// as UNSTABLE (they hold kotlin.collections.List, an interface it can't prove is
// immutable), which means every composable taking one can never skip recomposition —
// and Song is passed to essentially every row and card in the app.
@Immutable
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

