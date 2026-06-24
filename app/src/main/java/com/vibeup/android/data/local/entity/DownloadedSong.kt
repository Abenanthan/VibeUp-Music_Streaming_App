package com.vibeup.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_songs")
data class DownloadedSong(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val imageUrl: String,
    val localPath: String,
    val duration: Int,
    val fileSize: Long,
    val quality: String,
    val downloadedAt: Long = System.currentTimeMillis()
)