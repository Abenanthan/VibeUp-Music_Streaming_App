package com.vibeup.android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SearchResponseDto(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data")    val data: SongsDataDto?
)

data class SongsDataDto(
    @SerializedName("total")   val total: Int?,
    @SerializedName("start")   val start: Int?,
    @SerializedName("results") val results: List<SongDto>?
)

data class SongDto(
    @SerializedName("id")          val id: String?,
    @SerializedName("name")        val name: String?,
    @SerializedName("artists")     val artists: ArtistsDto?,
    @SerializedName("album")       val album: AlbumDto?,
    @SerializedName("duration")    val duration: Int?,
    @SerializedName("language")    val language: String?,
    @SerializedName("image")       val image: List<ImageDto>?,
    @SerializedName("downloadUrl") val downloadUrl: List<DownloadUrlDto>?
)

data class ArtistsDto(
    @SerializedName("primary") val primary: List<ArtistDto>?
)

data class ArtistDto(
    @SerializedName("id")   val id: String?,
    @SerializedName("name") val name: String?
)

data class AlbumDto(
    @SerializedName("id")   val id: String?,
    @SerializedName("name") val name: String?
)

data class ImageDto(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url")     val url: String?
)

data class DownloadUrlDto(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url")     val url: String?
)