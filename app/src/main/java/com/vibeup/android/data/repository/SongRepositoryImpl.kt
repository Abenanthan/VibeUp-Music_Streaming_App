package com.vibeup.android.data.repository

import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.api.JioSaavnDirectApiService
import com.vibeup.android.data.remote.dto.toDomain
import com.vibeup.android.data.remote.mapper.toDomain as toDomainDirect
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.SongRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
    private val api: SaavnApiService,
    private val directApi: JioSaavnDirectApiService
) : SongRepository {

    override suspend fun searchSongs(query: String): List<Song> {
        return try {
            val response = directApi.searchSongs(query = query)
            val results = response.results?.map { it.toDomainDirect() } ?: emptyList()
            
            val queryLower = query.lowercase().trim()
            
            val filteredResults = results.filterNot { song ->
                val titleLower = song.title.lowercase()
                titleLower.contains("tribute") || 
                titleLower.contains("instrumental") ||
                titleLower.contains("lofi") ||
                titleLower.contains("karaoke") ||
                titleLower.contains("mashup") ||
                titleLower.contains("remix") ||
                titleLower.contains("cover")
            }.sortedWith(compareByDescending<Song> { song ->
                val titleLower = song.title.lowercase()
                when {
                    titleLower == queryLower -> 2
                    titleLower.startsWith(queryLower) -> 1
                    else -> 0
                }
            })
            
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

    override suspend fun getSongById(id: String): Song? {
        return try {
            val response = api.getSongById(ids = id)
            response.data?.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getPlayableSong(songId: String): Song? {
        return getSongById(songId)
    }
}
