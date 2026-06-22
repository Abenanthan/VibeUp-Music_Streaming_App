package com.vibeup.android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LyricsDto(
    @SerializedName("id") val id: Int?,
    @SerializedName("trackName") val trackName: String?,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("albumName") val albumName: String?,
    @SerializedName("duration") val duration: Double?,
    @SerializedName("instrumental") val instrumental: Boolean?,
    @SerializedName("plainLyrics") val plainLyrics: String?,
    @SerializedName("syncedLyrics") val syncedLyrics: String?
)