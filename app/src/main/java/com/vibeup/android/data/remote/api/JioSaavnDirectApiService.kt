package com.vibeup.android.data.remote.api

import com.vibeup.android.data.remote.dto.JioSaavnDirectSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface JioSaavnDirectApiService {

    @GET("api.php")
    suspend fun searchSongs(
        @Query("__call") call: String = "search.getResults",
        @Query("_format") format: String = "json",
        @Query("_marker") marker: String = "0",
        @Query("api_version") apiVersion: String = "4",
        @Query("ctx") ctx: String = "web6dot0",
        @Query("q") query: String,
        @Query("n") limit: Int = 20,
        @Query("p") page: Int = 0,
        @Query("languages") languages: String = "english,hindi,punjabi,tamil,telugu"
    ): JioSaavnDirectSearchResponseDto
}
