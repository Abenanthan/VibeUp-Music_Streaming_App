package com.vibeup.android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class JioSaavnDirectSearchResponseDto(
    @SerializedName("results") val results: List<JioSaavnDirectSongDto>? = null,
    @SerializedName("total") val total: Int? = null
)

data class JioSaavnDirectSongDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String?,
    @SerializedName("subtitle") val subtitle: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("play_count") val playCount: String?,
    @SerializedName("explicit_content") val explicitContent: String?,
    @SerializedName("more_info") val moreInfo: JioSaavnDirectMoreInfoDto?
)

data class JioSaavnDirectMoreInfoDto(
    @SerializedName("album") val album: String?,
    @SerializedName("duration") val duration: String?,
    @SerializedName("encrypted_media_url") val encryptedMediaUrl: String?,
    @SerializedName("320kbps") val is320kbps: String?,
    @SerializedName("artistMap") val artistMap: JioSaavnDirectArtistMapDto?
)

data class JioSaavnDirectArtistMapDto(
    @SerializedName("primary_artists") val primaryArtists: List<JioSaavnDirectArtistDto>?
)

data class JioSaavnDirectArtistDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("image") val image: String?
)
