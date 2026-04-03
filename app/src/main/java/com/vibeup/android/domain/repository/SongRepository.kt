package com.vibeup.android.domain.repository

import com.vibeup.android.domain.model.Song

interface SongRepository {
    suspend fun searchSongs(query: String): List<Song>
    suspend fun getSongsByLanguage(language: String): List<Song>
    suspend fun extractAudioUrl(youtubeUrl: String): String
}