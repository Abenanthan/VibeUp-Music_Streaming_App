package com.vibeup.android.data.remote.mapper

import com.vibeup.android.data.remote.dto.ArtistDetailsResponseDto
import com.vibeup.android.data.remote.dto.toDomain
import com.vibeup.android.domain.model.Artist
import com.vibeup.android.domain.model.ArtistAlbum
import com.vibeup.android.domain.model.Song

fun ArtistDetailsResponseDto.toDomain(fallbackSongs: List<Song> = emptyList()): Artist {
    val artistData = data

    // bio can be a String OR an empty array [] from the API — handle both
    val bioText = try {
        val bioElement = artistData?.bio
        when {
            bioElement == null || bioElement.isJsonNull -> "No biography available."
            bioElement.isJsonArray -> "No biography available."
            bioElement.isJsonPrimitive && bioElement.asString.isNotBlank() -> bioElement.asString
            else -> "No biography available."
        }
    } catch (e: Exception) {
        "No biography available."
    }

    val songsFromApi = artistData?.topSongs?.map { it.toDomain() } ?: emptyList()
    val finalSongs = songsFromApi.ifEmpty { fallbackSongs }

    return Artist(
        id = artistData?.id ?: "",
        name = artistData?.name ?: "Unknown Artist",
        imageUrl = artistData?.image?.lastOrNull()?.url ?: "",
        followerCount = artistData?.followerCount?.toString()
            ?: artistData?.fanCount
            ?: "0",
        bio = bioText,
        songs = finalSongs,
        albums = artistData?.topAlbums?.map {
            ArtistAlbum(
                id = it.id ?: "",
                title = it.name ?: "",
                imageUrl = it.image?.lastOrNull()?.url ?: "",
                year = it.year?.toString() ?: ""
            )
        } ?: emptyList()
    )
}