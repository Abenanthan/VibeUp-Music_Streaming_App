package com.vibeup.android.domain.usecase

import com.vibeup.android.domain.repository.SongRepository
import javax.inject.Inject

class GetTopSearchesUseCase @Inject constructor(
    private val repository: SongRepository
) {
    suspend operator fun invoke(): List<String> {
        return repository.getTopSearches()
    }
}
