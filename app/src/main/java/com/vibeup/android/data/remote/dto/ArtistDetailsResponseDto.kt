package com.vibeup.android.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonElement

data class ArtistDetailsResponseDto(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: ArtistDataDto?
)

data class ArtistDataDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("image") val image: List<ImageDto>?,
    @SerializedName("followerCount") val followerCount: Int?,
    @SerializedName("fanCount") val fanCount: String?,
    @SerializedName("isVerified") val isVerified: Boolean?,
    @SerializedName("dominantLanguage") val dominantLanguage: String?,
    @SerializedName("dominantType") val dominantType: String?,
    @SerializedName("bio") val bio: JsonElement?,           // 👈 CHANGED — was String?
    @SerializedName("topSongs") val topSongs: List<SongDto>?,
    @SerializedName("topAlbums") val topAlbums: List<ArtistAlbumDto>?
)

data class ArtistAlbumDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("image") val image: List<ImageDto>?
)