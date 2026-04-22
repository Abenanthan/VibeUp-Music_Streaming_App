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
            // First search for the most relevant results
            val response = api.searchSongs(query, limit = 20)
            val results = response.data?.results?.map { it.toDomain() } ?: emptyList()
            
            // Refine results:
            // We want to prioritize results where the title exactly matches or contains 
            // the query and sort by relevance or popularity (handled by API mostly,
            // but we can filter out "covers" or "tributes" if they aren't what we want)
            
            val filteredResults = results.filterNot { song ->
                val titleLower = song.title.lowercase()
                titleLower.contains("tribute") || 
                titleLower.contains("instrumental") ||
                titleLower.contains("lofi") ||
                titleLower.contains("karaoke")
            }
            
            // If the filtered list is empty, return the original results
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