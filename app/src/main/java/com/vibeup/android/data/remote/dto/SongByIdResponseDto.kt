package com.vibeup.android.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SongByIdResponseDto(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data")    val data: List<SongDto>?
)