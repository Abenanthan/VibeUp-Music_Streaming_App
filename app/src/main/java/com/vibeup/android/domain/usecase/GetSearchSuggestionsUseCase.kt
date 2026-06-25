package com.vibeup.android.domain.usecase

import com.vibeup.android.domain.repository.SongRepository
import javax.inject.Inject

class GetSearchSuggestionsUseCase @Inject constructor(
    private val repository: SongRepository
) {
    suspend operator fun invoke(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return repository.getSearchSuggestions(query)
    }
}
