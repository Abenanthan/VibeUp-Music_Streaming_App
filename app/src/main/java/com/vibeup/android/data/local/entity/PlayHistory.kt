package com.vibeup.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_history")
data class PlayHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String,
    val imageUrl: String,
    val duration: Int,        // song duration in seconds
    val listenedAt: Long = System.currentTimeMillis(),
    val hourOfDay: Int = java.util.Calendar.getInstance()
        .get(java.util.Calendar.HOUR_OF_DAY),
    val dayOfYear: Int = java.util.Calendar.getInstance()
        .get(java.util.Calendar.DAY_OF_YEAR),
    val year: Int = java.util.Calendar.getInstance()
        .get(java.util.Calendar.YEAR)
)