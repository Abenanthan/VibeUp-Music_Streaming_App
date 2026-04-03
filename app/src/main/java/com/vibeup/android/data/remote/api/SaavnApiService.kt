package com.vibeup.android.data.remote.api

import com.vibeup.android.data.remote.dto.SearchResponseDto
import com.vibeup.android.data.remote.dto.SongByIdResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SaavnApiService {

    @GET("api/search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page")  page: Int = 1,
        @Query("limit") limit: Int = 20
    ): SearchResponseDto

    @GET("api/search/songs")
    suspend fun getSongsByLanguage(
        @Query("query")    query: String,
        @Query("limit")    limit: Int = 20
    ): SearchResponseDto

    @GET("api/songs")
    suspend fun getSongById(
        @Query("ids") ids: String
    ): SongByIdResponseDto
}