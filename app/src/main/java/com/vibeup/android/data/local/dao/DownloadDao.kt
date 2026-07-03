package com.vibeup.android.data.local.dao

import androidx.room.*
import com.vibeup.android.data.local.entity.DownloadedSong
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloaded_songs WHERE userId = :userId ORDER BY downloadedAt DESC")
    fun getAllDownloads(userId: String): Flow<List<DownloadedSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(song: DownloadedSong)

    @Query("DELETE FROM downloaded_songs WHERE id = :songId AND userId = :userId")
    suspend fun deleteDownload(songId: String, userId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE id = :songId AND userId = :userId)")
    suspend fun isDownloaded(songId: String, userId: String): Boolean

    @Query("SELECT * FROM downloaded_songs WHERE id = :songId AND userId = :userId")
    suspend fun getDownload(songId: String, userId: String): DownloadedSong?

    @Query("SELECT SUM(fileSize) FROM downloaded_songs WHERE userId = :userId")
    suspend fun getTotalSize(userId: String): Long?
}