package com.vibeup.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vibeup.android.data.local.dao.SearchHistoryDao
import com.vibeup.android.data.local.entity.SearchHistory

@Database(
    entities = [SearchHistory::class],
    version = 1,
    exportSchema = false
)
abstract class VibeUpDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
}