package com.vibeup.android.data.local.dao

import androidx.room.*
import com.vibeup.android.data.local.entity.SearchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getSearchHistory(): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistory)

    @Delete
    suspend fun deleteSearch(search: SearchHistory)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}