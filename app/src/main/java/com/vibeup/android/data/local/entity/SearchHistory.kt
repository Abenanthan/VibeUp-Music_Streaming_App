package com.vibeup.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)