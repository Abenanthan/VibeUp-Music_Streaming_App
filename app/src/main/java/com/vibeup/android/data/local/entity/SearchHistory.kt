package com.vibeup.android.data.local.entity

import androidx.room.Entity

@Entity(tableName = "search_history", primaryKeys = ["query", "userId"])
data class SearchHistory(
    val query: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)