package com.vibeup.android.domain.model

import androidx.compose.runtime.Immutable

import java.io.Serializable

// @Immutable: these are value-style data classes whose properties are all vals and
// are never mutated after construction. Without it the Compose compiler infers them
// as UNSTABLE (they hold kotlin.collections.List, an interface it can't prove is
// immutable), which means every composable taking one can never skip recomposition —
// and Song is passed to essentially every row and card in the app.
@Immutable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String,
    val duration: Int,
    val imageUrl: String,
    val audioUrl: String,
    val language: String,
    val allArtists: List<ArtistCredit> = emptyList()
) : Serializable

@Immutable
data class ArtistCredit(
    val id: String,
    val name: String,
    val imageUrl: String = ""
) : Serializable
