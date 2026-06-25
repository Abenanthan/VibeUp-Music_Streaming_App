package com.vibeup.android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class JioSaavnDirectSuggestionDto(
    @SerializedName("albums") val albums: SuggestionSectionDto? = null,
    @SerializedName("songs") val songs: SuggestionSectionDto? = null,
    @SerializedName("playlists") val playlists: SuggestionSectionDto? = null,
    @SerializedName("artists") val artists: SuggestionSectionDto? = null,
    @SerializedName("topquery") val topquery: SuggestionSectionDto? = null
)

data class SuggestionSectionDto(
    @SerializedName("data") val data: List<SuggestionItemDto>? = null
)

data class SuggestionItemDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("description") val description: String?
)
