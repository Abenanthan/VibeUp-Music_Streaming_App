package com.vibeup.android.domain.usecase

import com.vibeup.android.domain.repository.SongRepository
import javax.inject.Inject

class ExtractAudioUrlUseCase @Inject constructor(
    private val repository: SongRepository
) {
    suspend operator fun invoke(youtubeUrl: String): String {
        return repository.extractAudioUrl(youtubeUrl)
    }
}