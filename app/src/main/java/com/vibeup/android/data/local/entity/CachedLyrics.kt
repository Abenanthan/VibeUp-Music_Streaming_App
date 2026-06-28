package com.vibeup.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_lyrics")
data class CachedLyrics(
    @PrimaryKey
    val songId: String,
    val syncedLyrics: String?,
    val plainLyrics: String?,
    val isInstrumental: Boolean = false,
    val provider: String = "lrclib",
    val cachedAt: Long = System.currentTimeMillis()
)