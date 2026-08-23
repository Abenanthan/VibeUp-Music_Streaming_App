package com.vibeup.android.domain.model

import androidx.compose.runtime.Immutable

// @Immutable: these are value-style data classes whose properties are all vals and
// are never mutated after construction. Without it the Compose compiler infers them
// as UNSTABLE (they hold kotlin.collections.List, an interface it can't prove is
// immutable), which means every composable taking one can never skip recomposition —
// and Song is passed to essentially every row and card in the app.
@Immutable
data class Playlist(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val coverImageUrl: String = "",
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)