package com.vibeup.android.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibeup.android.data.local.dao.SearchHistoryDao
import com.vibeup.android.data.local.entity.SearchHistory
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.usecase.SearchSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val searchHistory = searchHistoryDao.getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(500)
            _isLoading.value = true
            try {
                _searchResults.value = searchSongsUseCase(newQuery)
                // Save to history after successful search
                if (_searchResults.value.isNotEmpty()) {
                    searchHistoryDao.insertSearch(
                        SearchHistory(query = newQuery.trim())
                    )
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchFromHistory(query: String) {
        onQueryChange(query)
    }

    fun deleteHistoryItem(search: SearchHistory) {
        viewModelScope.launch {
            searchHistoryDao.deleteSearch(search)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            searchHistoryDao.clearHistory()
        }
    }

    fun clearSearch() {
        _query.value = ""
        _searchResults.value = emptyList()
    }
}