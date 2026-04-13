package com.vibeup.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vibeup.android.domain.model.Playlist
import com.vibeup.android.domain.model.Song
import com.vibeup.android.domain.repository.LibraryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : LibraryRepository {

    private val uid get() = auth.currentUser?.uid ?: ""

    private fun userDoc() = firestore.collection("users").document(uid)

    override suspend fun likeSong(song: Song) {
        try {
            val currentUid = auth.currentUser?.uid
            android.util.Log.d("LibraryRepo", "Liking song: ${song.title}, uid: $currentUid")
            if (currentUid == null) {
                android.util.Log.e("LibraryRepo", "User not logged in!")
                return
            }
            userDoc().collection("likedSongs")
                .document(song.id)
                .set(song.toMap())
                .await()
            android.util.Log.d("LibraryRepo", "Song liked successfully!")
        } catch (e: Exception) {
            android.util.Log.e("LibraryRepo", "Failed to like: ${e.message}")
        }
    }

    override suspend fun unlikeSong(songId: String) {
        userDoc().collection("likedSongs")
            .document(songId)
            .delete()
            .await()
    }

    override suspend fun isLiked(songId: String): Boolean {
        return userDoc().collection("likedSongs")
            .document(songId)
            .get()
            .await()
            .exists()
    }

    override fun getLikedSongs(): Flow<List<Song>> = callbackFlow {
        val listener = userDoc().collection("likedSongs")
            .addSnapshotListener { snapshot, _ ->
                val songs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toSong()
                } ?: emptyList()
                trySend(songs)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createPlaylist(
        name: String,
        description: String
    ): String {
        val id = UUID.randomUUID().toString()
        userDoc().collection("playlists")
            .document(id)
            .set(mapOf(
                "id" to id,
                "name" to name,
                "description" to description,
                "createdAt" to System.currentTimeMillis(),
                "createdBy" to uid
            )).await()
        return id
    }

    override suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        userDoc().collection("playlists")
            .document(playlistId)
            .collection("songs")
            .document(song.id)
            .set(song.toMap())
            .await()
    }

    override suspend fun removeSongFromPlaylist(
        playlistId: String,
        songId: String
    ) {
        userDoc().collection("playlists")
            .document(playlistId)
            .collection("songs")
            .document(songId)
            .delete()
            .await()
    }

    override fun getPlaylists(): Flow<List<Playlist>> = callbackFlow {
        val listener = userDoc().collection("playlists")
            .addSnapshotListener { snapshot, _ ->
                val playlists = snapshot?.documents?.mapNotNull { doc ->
                    Playlist(
                        id = doc.getString("id") ?: "",
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                } ?: emptyList()
                trySend(playlists)
            }
        awaitClose { listener.remove() }
    }

    override fun getPlaylistSongs(playlistId: String): Flow<List<Song>> =
        callbackFlow {
            val listener = userDoc().collection("playlists")
                .document(playlistId)
                .collection("songs")
                .addSnapshotListener { snapshot, _ ->
                    val songs = snapshot?.documents?.mapNotNull { doc ->
                        doc.toSong()
                    } ?: emptyList()
                    trySend(songs)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun deletePlaylist(playlistId: String) {
        userDoc().collection("playlists")
            .document(playlistId)
            .delete()
            .await()
    }

    override suspend fun addToRecentlyPlayed(song: Song) {
        if (uid.isEmpty()) return
        try {
            userDoc().collection("recentlyPlayed")
                .document(song.id)
                .set(song.toMap() + mapOf(
                    "playedAt" to System.currentTimeMillis()
                ))
                .await()
        } catch (e: Exception) {
            android.util.Log.e("LibraryRepo", "Recently played error: ${e.message}")
        }
    }

    override fun getRecentlyPlayed(): Flow<List<Song>> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val listener = userDoc().collection("recentlyPlayed")
            .orderBy("playedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(15)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val songs = snapshot?.documents?.mapNotNull { it.toSong() } ?: emptyList()
                trySend(songs)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String) {
        userDoc().collection("playlists")
            .document(playlistId)
            .update("name", newName)
            .await()
    }

    // Extension functions
    private fun Song.toMap() = mapOf(
        "id" to id,
        "title" to title,
        "artist" to artist,
        "album" to album,
        "duration" to duration,
        "imageUrl" to imageUrl,
        "audioUrl" to audioUrl,
        "language" to language
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toSong(): Song? {
        return try {
            Song(
                id = getString("id") ?: return null,
                title = getString("title") ?: "",
                artist = getString("artist") ?: "",
                album = getString("album") ?: "",
                duration = getLong("duration")?.toInt() ?: 0,
                imageUrl = getString("imageUrl") ?: "",
                audioUrl = getString("audioUrl") ?: "",
                language = getString("language") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}