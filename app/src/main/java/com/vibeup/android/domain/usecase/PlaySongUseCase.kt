package com.vibeup.android.domain.usecase

import android.util.Log
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.SongRepository
import java.io.File
import javax.inject.Inject

class PlaySongUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(song: Song): Song {

        // ── 1. Local / downloaded file ──────────────────────────────────────
        // Strip file:// prefix to get the raw absolute path
        val rawPath = when {
            song.audioUrl.startsWith("file://") ->
                song.audioUrl.removePrefix("file://")
            song.audioUrl.startsWith("/") ->
                song.audioUrl
            else -> null
        }

        if (rawPath != null) {
            // Try the path as-is first, then fallback variants
            val candidates = listOf(
                rawPath,
                rawPath.replace("/data/user/0/", "/data/data/"),
                rawPath.replace("/data/data/", "/data/user/0/")
            )
            val existing = candidates.firstOrNull { File(it).exists() && File(it).length() > 0 }

            return if (existing != null) {
                Log.d("PlaySongUseCase", "✅ Local file found: $existing")
                song.copy(audioUrl = "file://$existing")
            } else {
                // File missing — log all tried paths, return song as-is
                // ExoPlayer will emit an error, app will NOT crash
                Log.e("PlaySongUseCase", "❌ Local file missing. Tried: $candidates")
                song.copy(audioUrl = "file://$rawPath")
            }
        }

        // ── 2. Already a valid CDN stream URL ──────────────────────────────
        if (song.audioUrl.startsWith("https://aac.saavncdn.com")) {
            return song
        }

        // ── 3. Needs network resolution ────────────────────────────────────
        return try {
            val resolved = songRepository.getPlayableSong(song.id)
            resolved ?: song
        } catch (e: Exception) {
            Log.e("PlaySongUseCase", "❌ URL resolve failed: ${e.message}")
            song
        }
    }
}