package com.vibeup.android.data.remote.mapper

import com.vibeup.android.data.remote.dto.JioSaavnDirectSongDto
import com.vibeup.android.domain.model.Song

fun JioSaavnDirectSongDto.toDomain(): Song {
    return Song(
        id = id,
        title = title ?: "",
        artist = moreInfo?.artistMap?.primaryArtists?.joinToString(", ") { it.name ?: "" } ?: "",
        artistId = moreInfo?.artistMap?.primaryArtists?.firstOrNull()?.id ?: "",
        album = moreInfo?.album ?: "",
        duration = moreInfo?.duration?.toIntOrNull() ?: 0,
        imageUrl = image ?: "",
        audioUrl = "", // Decryption needed for direct API, or fetch via getSongById
        language = language ?: ""
    )
}