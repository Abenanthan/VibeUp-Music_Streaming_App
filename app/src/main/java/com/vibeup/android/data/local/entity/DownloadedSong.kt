package com.vibeup.android.data.local.entity

import androidx.room.Entity

@Entity(tableName = "downloaded_songs", primaryKeys = ["id", "userId"])
data class DownloadedSong(
    val id: String,
    val userId: String,
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