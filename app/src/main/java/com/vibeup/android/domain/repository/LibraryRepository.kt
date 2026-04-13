package com.vibeup.android.domain.repository

import com.vibeup.android.domain.model.Playlist
import com.vibeup.android.domain.model.Song
import kotlinx.coroutines.flow.Flow


interface LibraryRepository {
    suspend fun likeSong(song: Song)
    suspend fun unlikeSong(songId: String)
    suspend fun isLiked(songId: String): Boolean
    fun getLikedSongs(): Flow<List<Song>>
    suspend fun createPlaylist(name: String, description: String): String
    suspend fun addSongToPlaylist(playlistId: String, song: Song)
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
    fun getPlaylists(): Flow<List<Playlist>>
    fun getPlaylistSongs(playlistId: String): Flow<List<Song>>
    suspend fun deletePlaylist(playlistId: String)

    suspend fun addToRecentlyPlayed(song: Song)

    suspend fun renamePlaylist(playlistId: String, newName: String)

    fun getRecentlyPlayed(): Flow<List<Song>>
}