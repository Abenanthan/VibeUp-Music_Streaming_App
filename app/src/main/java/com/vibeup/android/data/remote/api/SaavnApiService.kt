package com.vibeup.android.data.remote.api

import com.vibeup.android.data.remote.dto.SearchResponseDto
import com.vibeup.android.data.remote.dto.SongByIdResponseDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import com.vibeup.android.data.remote.dto.ArtistDetailsResponseDto

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

    @GET("api/artists/{id}")
    suspend fun getArtistDetails(
        @Path("id") artistId: String,
        @Query("songCount") songCount: Int = 30,
        @Query("albumCount") albumCount: Int = 10
    ): ArtistDetailsResponseDto
}