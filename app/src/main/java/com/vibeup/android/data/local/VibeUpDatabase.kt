package com.vibeup.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vibeup.android.data.local.dao.DownloadDao
import com.vibeup.android.data.local.dao.SearchHistoryDao
import com.vibeup.android.data.local.entity.DownloadedSong
import com.vibeup.android.data.local.entity.SearchHistory
import com.vibeup.android.data.local.entity.CachedLyrics
import com.vibeup.android.data.local.dao.LyricsDao
import com.vibeup.android.data.local.entity.PlayHistory
import com.vibeup.android.data.local.dao.PlayHistoryDao
@Database(
    entities = [
        SearchHistory::class,
        DownloadedSong::class,
        CachedLyrics::class,
        PlayHistory::class    // ← ADD
    ],
    version = 4,              // ← bump
    exportSchema = false
)
abstract class VibeUpDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun playHistoryDao(): PlayHistoryDao  // ← ADD
}