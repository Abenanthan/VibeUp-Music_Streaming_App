package com.vibeup.android.domain.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val followerCount: String,
    val bio: String,
    val songs: List<Song>,
    val albums: List<ArtistAlbum>
)

data class ArtistAlbum(
    val id: String,
    val title: String,
    val imageUrl: String,
    val year: String
)