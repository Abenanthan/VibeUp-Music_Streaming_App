package com.vibeup.android.domain.model

import androidx.compose.runtime.Immutable

// @Immutable: these are value-style data classes whose properties are all vals and
// are never mutated after construction. Without it the Compose compiler infers them
// as UNSTABLE (they hold kotlin.collections.List, an interface it can't prove is
// immutable), which means every composable taking one can never skip recomposition —
// and Song is passed to essentially every row and card in the app.
@Immutable
data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val followerCount: String,
    val bio: String,
    val songs: List<Song>,
    val albums: List<ArtistAlbum>
)

@Immutable
data class ArtistAlbum(
    val id: String,
    val title: String,
    val imageUrl: String,
    val year: String
)