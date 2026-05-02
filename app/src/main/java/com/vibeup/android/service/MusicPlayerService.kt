package com.vibeup.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URL
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    @Inject
    lateinit var playerManager: PlayerManager

    private var mediaSession: MediaSession? = null
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var playerNotificationManager: PlayerNotificationManager? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var metadataJob: Job? = null
    private var songObserveJob: Job? = null
    private var playingObserveJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val player = playerManager.getExoPlayer()

        // ✅ Step 1 — MediaSessionCompat for Dynamic Island
        mediaSessionCompat = MediaSessionCompat(
            this, "VibeUpSession"
        ).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playerManager.togglePlayPause()
                }
                override fun onPause() {
                    playerManager.togglePlayPause()
                }
                override fun onSkipToNext() {
                    playerManager.playNext()
                }
                override fun onSkipToPrevious() {
                    playerManager.playPrevious()
                }
                override fun onSeekTo(pos: Long) {
                    playerManager.seekTo(pos)
                }
            })
            isActive = true
        }

        // ✅ Step 2 — Media3 MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<androidx.media3.common.MediaItem>
                ) = com.google.common.util.concurrent.Futures
                    .immediateFuture(mediaItems)
            })
            .build()

        // ✅ Step 3 — Notification
        playerNotificationManager = PlayerNotificationManager
            .Builder(this, NOTIFICATION_ID, CHANNEL_ID)
            .setMediaDescriptionAdapter(
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun getCurrentContentTitle(
                        player: Player
                    ): CharSequence {
                        return playerManager.currentSong.value?.title
                            ?: "VibeUp"
                    }

                    override fun createCurrentContentIntent(
                        player: Player
                    ) = null

                    override fun getCurrentContentText(
                        player: Player
                    ): CharSequence {
                        return playerManager.currentSong.value?.artist
                            ?: ""
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                    ) = null
                }
            )
            .setNotificationListener(
                object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationPosted(
                        notificationId: Int,
                        notification: Notification,
                        ongoing: Boolean
                    ) {
                        startForeground(notificationId, notification)
                    }

                    override fun onNotificationCancelled(
                        notificationId: Int,
                        dismissedByUser: Boolean
                    ) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            )
            .build()

        playerNotificationManager?.apply {
            setPlayer(player)
            setUseFastForwardAction(false)
            setUseRewindAction(false)
            setUseNextAction(true)
            setUsePreviousAction(true)
            setUsePlayPauseActions(true)
            setUseStopAction(false)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setColor(0xFF8B5CF6.toInt())
            setColorized(true)
            // ✅ Use mediaSessionCompat token
            setMediaSessionToken(
                mediaSessionCompat!!.sessionToken
            )
        }

        // ✅ Step 4 — Observe changes for Dynamic Island
        observePlayerState()
    }

    private fun observePlayerState() {
        // Observe song changes → update metadata
        songObserveJob = scope.launch {
            playerManager.currentSong.collect { song ->
                song ?: return@collect
                updateMetadata(song)
            }
        }

        // Observe playing state → update Dynamic Island
        playingObserveJob = scope.launch {
            playerManager.isPlaying.collect { isPlaying ->
                updatePlaybackState(isPlaying)
            }
        }
    }

    private fun updateMetadata(
        song: com.vibeup.android.domain.model.Song
    ) {
        metadataJob?.cancel()
        metadataJob = scope.launch {
            try {
                // Load album art bitmap
                val bitmap = try {
                    val url = URL(song.imageUrl)
                    val connection = url.openConnection().apply {
                        connectTimeout = 3000
                        readTimeout = 3000
                    }
                    connection.connect()
                    BitmapFactory.decodeStream(
                        connection.getInputStream()
                    )
                } catch (e: Exception) {
                    null
                }

                // ✅ Set metadata — Dynamic Island reads this!
                val metadata = MediaMetadataCompat.Builder()
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        song.title
                    )
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                        song.artist
                    )
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_ALBUM,
                        song.album
                    )
                    .putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        song.duration * 1000L
                    )
                    .apply {
                        bitmap?.let {
                            putBitmap(
                                MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                                it
                            )
                            putBitmap(
                                MediaMetadataCompat.METADATA_KEY_ART,
                                it
                            )
                        }
                    }
                    .build()

                mediaSessionCompat?.setMetadata(metadata)
                android.util.Log.d(
                    "MusicService",
                    "Metadata set: ${song.title}"
                )
            } catch (e: Exception) {
                android.util.Log.e(
                    "MusicService",
                    "Metadata error: ${e.message}"
                )
            }
        }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying)
            PlaybackStateCompat.STATE_PLAYING
        else
            PlaybackStateCompat.STATE_PAUSED

        mediaSessionCompat?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    state,
                    playerManager.currentPosition.value,
                    1f
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        playerManager.resetState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        songObserveJob?.cancel()
        playingObserveJob?.cancel()
        metadataJob?.cancel()
        playerNotificationManager?.setPlayer(null)
        mediaSessionCompat?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
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
            val manager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1001
    }
}