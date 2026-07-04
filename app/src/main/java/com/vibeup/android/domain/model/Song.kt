package com.vibeup.android.domain.model

import java.io.Serializable

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
) : Serializable

data class ArtistCredit(
    val id: String,
    val name: String,
    val imageUrl: String = ""
) : Serializable
