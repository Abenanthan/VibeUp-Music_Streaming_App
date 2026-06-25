package com.vibeup.android.domain.usecase

import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.SongRepository
import javax.inject.Inject

class PlaySongUseCase @Inject constructor(
    private val songRepository: SongRepository
) {
    // ✅ Resolves playable URL then returns song
    suspend operator fun invoke(song: Song): Song {
        // If song already has a valid direct URL skip re-fetch
        if (song.audioUrl.startsWith("https://aac.saavncdn.com") ||
            song.audioUrl.startsWith("file://") ||
            song.audioUrl.startsWith("content://")
        ) {
            return song
        }

        // Fetch fresh clean URL from wrapper
        val playableSong = songRepository.getPlayableSong(song.id)
        return playableSong ?: song
    }
}
