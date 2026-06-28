package com.vibeup.android.data.local.dao

import androidx.room.*
import com.vibeup.android.data.local.entity.CachedLyrics

@Dao
interface LyricsDao {

    @Query("SELECT * FROM cached_lyrics WHERE songId = :songId")
    suspend fun getLyrics(songId: String): CachedLyrics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: CachedLyrics)

    @Query(
        "DELETE FROM cached_lyrics WHERE cachedAt < :before"
    )
    suspend fun deleteOldLyrics(before: Long)

    @Query("SELECT COUNT(*) FROM cached_lyrics")
    suspend fun getCount(): Int
}