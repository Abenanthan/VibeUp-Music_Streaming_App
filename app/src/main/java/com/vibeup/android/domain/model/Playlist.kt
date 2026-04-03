package com.vibeup.android.domain.model

data class Playlist(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)