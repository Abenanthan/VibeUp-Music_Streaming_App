package com.vibeup.android.data.repository

import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.dto.toDomain
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.SongRepository
import javax.inject.Inject

class SongRepositoryImpl @Inject constructor(
    private val api: SaavnApiService
) : SongRepository {

    override suspend fun searchSongs(query: String): List<Song> {
        return try {
            // 1. Increased Result Limit: Fetch 20 results instead of 10 to have a better pool
            val response = api.searchSongs(query, limit = 20)
            val results = response.data?.results?.map { it.toDomain() } ?: emptyList()
            
            // 2. Smart Filtering: Automatically hides "Tributes", "Instrumentals", "Lofi", "Karaoke", "Remix", and "Mashup"
            // This filters out "random tunes" that clutter results when looking for official versions.
            val filteredResults = results.filterNot { song ->
                val titleLower = song.title.lowercase()
                titleLower.contains("tribute") || 
                titleLower.contains("instrumental") ||
                titleLower.contains("lofi") ||
                titleLower.contains("karaoke") ||
                titleLower.contains("mashup") ||
                titleLower.contains("remix") ||
                titleLower.contains("cover")
            }
            
            // 3. Popularity Logic: Return filtered results if available. 
            // JioSaavn API naturally ranks official/popular results higher.
            if (filteredResults.isEmpty()) results else filteredResults
            
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getSongsByLanguage(language: String): List<Song> {
        return try {
            val query = when (language) {
                "tamil"  -> "tamil hits 2024"
                "telugu" -> "telugu hits 2024"
                "hindi"  -> "hindi hits 2024"
                else     -> language
            }
            val response = api.getSongsByLanguage(query)
            response.data?.results?.map { it.toDomain() } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun extractAudioUrl(url: String): String {
        return url
    }
}