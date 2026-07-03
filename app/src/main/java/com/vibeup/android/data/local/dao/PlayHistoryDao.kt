package com.vibeup.android.data.local.dao

import androidx.room.*
import com.vibeup.android.data.local.entity.PlayHistory
import kotlinx.coroutines.flow.Flow

data class SongPlayCount(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val imageUrl: String,
    val playCount: Int
)

data class ArtistPlayCount(
    val artist: String,
    val playCount: Int
)

data class HourPlayCount(
    val hour: Int,
    val playCount: Int
)

data class DayPlayCount(
    val dayOfYear: Int,
    val year: Int,
    val playCount: Int
)

@Dao
interface PlayHistoryDao {

    @Insert
    suspend fun insertPlay(play: PlayHistory)

    // ✅ Total plays
    @Query("SELECT COUNT(*) FROM play_history WHERE userId = :userId")
    suspend fun getTotalPlays(userId: String): Int

    // ✅ Total listen time in seconds
    @Query("SELECT SUM(duration) FROM play_history WHERE userId = :userId")
    suspend fun getTotalListenSeconds(userId: String): Long?

    // ✅ Top songs by play count
    @Query("""
        SELECT songId, title, artist, album, imageUrl,
        COUNT(*) as playCount
        FROM play_history
        WHERE userId = :userId
        GROUP BY songId
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getTopSongs(userId: String, limit: Int = 5): List<SongPlayCount>

    // ✅ Top artists
    @Query("""
        SELECT artist, COUNT(*) as playCount
        FROM play_history
        WHERE userId = :userId
        GROUP BY artist
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getTopArtists(userId: String, limit: Int = 5): List<ArtistPlayCount>

    // ✅ Peak listening hour
    @Query("""
        SELECT hourOfDay as hour, COUNT(*) as playCount
        FROM play_history
        WHERE userId = :userId
        GROUP BY hourOfDay
        ORDER BY playCount DESC
        LIMIT 1
    """)
    suspend fun getPeakHour(userId: String): HourPlayCount?

    // ✅ Last 7 days play count
    @Query("""
        SELECT dayOfYear, year, COUNT(*) as playCount
        FROM play_history
        WHERE userId = :userId AND listenedAt > :since
        GROUP BY dayOfYear, year
        ORDER BY year ASC, dayOfYear ASC
    """)
    suspend fun getWeeklyPlays(
        userId: String,
        since: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
    ): List<DayPlayCount>

    // ✅ Listening streak
    @Query("SELECT dayOfYear, year, COUNT(*) as playCount FROM play_history WHERE userId = :userId GROUP BY dayOfYear, year ORDER BY year DESC, dayOfYear DESC")
    suspend fun getDistinctPlayDays(userId: String): List<DayPlayCount>

    // ✅ Most played album
    @Query("""
        SELECT album as artist, COUNT(*) as playCount
        FROM play_history
        WHERE userId = :userId
        GROUP BY album
        ORDER BY playCount DESC
        LIMIT 1
    """)
    suspend fun getTopAlbum(userId: String): ArtistPlayCount?

    // ✅ Recent plays for home screen
    @Query("""
        SELECT * FROM play_history
        WHERE userId = :userId
        ORDER BY listenedAt DESC
        LIMIT 20
    """)
    fun getRecentPlays(userId: String): Flow<List<PlayHistory>>

    @Query("DELETE FROM play_history WHERE userId = :userId")
    suspend fun clearAll(userId: String)
}