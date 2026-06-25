package com.vibeup.android.data.remote.api

import com.vibeup.android.data.remote.dto.JioSaavnDirectSearchResponseDto
import com.vibeup.android.data.remote.dto.JioSaavnDirectSuggestionDto
import com.vibeup.android.data.remote.dto.SuggestionItemDto
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

    @GET("api.php")
    suspend fun getSuggestions(
        @Query("__call") call: String = "autocomplete.get",
        @Query("_format") format: String = "json",
        @Query("_marker") marker: String = "0",
        @Query("api_version") apiVersion: String = "4",
        @Query("ctx") ctx: String = "web6dot0",
        @Query("query") query: String
    ): JioSaavnDirectSuggestionDto

    @GET("api.php")
    suspend fun getTopSearches(
        @Query("__call") call: String = "content.getTopSearches",
        @Query("_format") format: String = "json",
        @Query("_marker") marker: String = "0",
        @Query("api_version") apiVersion: String = "4",
        @Query("ctx") ctx: String = "web6dot0"
    ): List<SuggestionItemDto>
}
