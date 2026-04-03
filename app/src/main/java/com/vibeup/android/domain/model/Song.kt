package com.vibeup.android.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Int,
    val imageUrl: String,
    val audioUrl: String,
    val language: String
)