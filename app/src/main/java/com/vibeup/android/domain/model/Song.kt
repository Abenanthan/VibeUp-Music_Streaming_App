package com.vibeup.android.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String,
    val duration: Int,
    val imageUrl: String,
    val audioUrl: String,
    val language: String,
    val allArtists: List<ArtistCredit> = emptyList()
)

data class ArtistCredit(
    val id: String,
    val name: String,
    val imageUrl: String = ""
)