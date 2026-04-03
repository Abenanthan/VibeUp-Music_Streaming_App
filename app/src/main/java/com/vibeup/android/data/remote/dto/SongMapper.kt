package com.vibeup.android.data.remote.dto

import com.vibeup.android.domain.model.Song

fun SongDto.toDomain(): Song {
    return Song(
        id = id ?: "",
        title = name ?: "Unknown",
        artist = artists?.primary?.firstOrNull()?.name ?: "Unknown Artist",
        album = album?.name ?: "Unknown Album",
        duration = duration ?: 0,
        imageUrl = image?.lastOrNull()?.url ?: "",
        audioUrl = downloadUrl?.lastOrNull()?.url ?: "",
        language = language ?: ""
    )
}