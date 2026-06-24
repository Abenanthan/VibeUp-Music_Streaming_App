package com.vibeup.android.data.local.dao

import androidx.room.*
import com.vibeup.android.data.local.entity.DownloadedSong
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadedSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(song: DownloadedSong)

    @Query("DELETE FROM downloaded_songs WHERE id = :songId")
    suspend fun deleteDownload(songId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE id = :songId)")
    suspend fun isDownloaded(songId: String): Boolean

    @Query("SELECT * FROM downloaded_songs WHERE id = :songId")
    suspend fun getDownload(songId: String): DownloadedSong?

    @Query("SELECT SUM(fileSize) FROM downloaded_songs")
    suspend fun getTotalSize(): Long?
}