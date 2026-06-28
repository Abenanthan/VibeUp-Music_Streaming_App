package com.vibeup.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vibeup.android.data.local.dao.DownloadDao
import com.vibeup.android.data.local.dao.SearchHistoryDao
import com.vibeup.android.data.local.entity.DownloadedSong
import com.vibeup.android.data.local.entity.SearchHistory
import com.vibeup.android.data.local.entity.CachedLyrics
import com.vibeup.android.data.local.dao.LyricsDao

@Database(
    entities = [
        SearchHistory::class,
        DownloadedSong::class,
        CachedLyrics::class
    ],
    version = 3,  // ← bump version
    exportSchema = false
)
abstract class VibeUpDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun lyricsDao(): LyricsDao// ← ADD
}