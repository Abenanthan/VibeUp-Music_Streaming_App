package com.vibeup.android.data.remote.api

import com.vibeup.android.data.remote.dto.LyricsDto
import retrofit2.http.GET
import retrofit2.http.Query

interface LyricsApiService {

    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artistName: String,
        @Query("track_name") trackName: String,
        @Query("album_name") albumName: String = ""
    ): LyricsDto

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("artist_name") artistName: String,
        @Query("track_name") trackName: String
    ): List<LyricsDto>
}