package com.vibeup.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MusicPlayerService — fixed for OriginOS 6 Origin Island.
 *
 * HOW ORIGIN ISLAND WORKS FOR MUSIC:
 * OriginOS 6 monitors the system for any active Android MediaSession that has
 * a playing state. When it finds one (from ANY app — Spotify, JioSaavn, or yours),
 * it automatically renders the Origin Island pill with album art, song title,
 * artist, and playback controls. No special Vivo SDK is needed.
 *
 * WHY YOUR OLD CODE DIDN'T TRIGGER THE ISLAND:
 * 1. You ran TWO parallel sessions (Media3 MediaSession + MediaSessionCompat).
 *    They produced conflicting/incomplete MediaStyle notifications — OriginOS
 *    saw a broken session and skipped it.
 * 2. PlayerNotificationManager was fighting with MediaSessionService's own
 *    built-in notification publisher. You only need one — not both.
 * 3. Metadata was pushed only to the Compat session via setMetadata(), but
 *    OriginOS 6 reads from the Media3 MediaSession which saw no metadata.
 *
 * THE FIX (matches how Spotify / JioSaavn work):
 * - Use ONLY Media3 MediaSessionService + MediaSession. Drop MediaSessionCompat.
 * - Let MediaSessionService auto-publish the MediaStyle notification.
 * - All metadata lives on MediaItem objects (set in PlayerManager.playSong).
 * - ExoPlayer syncs MediaItem metadata → MediaSession → notification automatically.
 * - OriginOS 6 reads the active MediaSession and renders the Origin Island pill.
 */
@UnstableApi
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    @Inject
    lateinit var playerManager: PlayerManager

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var songObserveJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val player = playerManager.getExoPlayer()

        // ── Single, clean Media3 MediaSession ────────────────────────────────
        // This is the ONLY session you need. MediaSessionService automatically
        // publishes a proper MediaStyle foreground notification backed by this
        // session. OriginOS 6 detects this notification + active playback state
        // and expands the Origin Island pill with your song info & album art.
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(buildLaunchIntent())
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<androidx.media3.common.MediaItem>
                ) = com.google.common.util.concurrent.Futures.immediateFuture(mediaItems)
            })
            .build()

        observeSongChanges()
    }

    /** Required by MediaSessionService — return your active session. */
    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            playerManager.resetState()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        songObserveJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun observeSongChanges() {
        // Metadata is already embedded in MediaItems via PlayerManager.playSong().
        // ExoPlayer propagates it to the MediaSession automatically, so no manual
        // setMetadata() calls are needed here. Keep this for side effects if needed.
        songObserveJob = scope.launch {
            playerManager.currentSong.collect { /* side effects / analytics */ }
        }
    }

    private fun buildLaunchIntent(): PendingIntent {
        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
            ?: Intent()
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VibeUp Music Player"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1001
    }
}