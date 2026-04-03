package com.vibeup.android.data.repository

import com.vibeup.android.data.remote.api.SaavnApiService
import com.vibeup.android.data.remote.dto.toDomain
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.SongRepository
import javax.inject.Inject

class SongRepositoryImpl @Inject constructor(
    private val api: SaavnApiService  // ← SaavnApiService not YouTubeSearchService
) : SongRepository {

    override suspend fun searchSongs(query: String): List<Song> {
        return try {
            val response = api.searchSongs(query)
            response.data?.results?.map { it.toDomain() } ?: emptyList()
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
/*```

---

## Steps in Order:
```
1. Update SongRepositoryImpl.kt ✅
2. Delete NewPipeDownloader.kt ✅
3. Delete YouTubeSearchService.kt ✅
4. Remove NewPipe from build.gradle.kts ✅
5. Run: ./gradlew clean
6. Build → Rebuild Project
7. Run app ✅*/