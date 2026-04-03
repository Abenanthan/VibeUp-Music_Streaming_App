package com.vibeup.android.domain.usecase

import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.SongRepository
import javax.inject.Inject

class GetSongsByLanguageUseCase @Inject constructor(
    private val repository: SongRepository
) {
    suspend operator fun invoke(language: String): List<Song> {
        return repository.getSongsByLanguage(language)
    }
}