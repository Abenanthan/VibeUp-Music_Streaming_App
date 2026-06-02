package com.vibeup.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.vibeup.android.MainActivity
import com.vibeup.android.R
import com.google.common.collect.ImmutableList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.net.URL
import javax.inject.Inject

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicPlayerService : MediaLibraryService() {

    @Inject
    lateinit var playerManager: PlayerManager

    private var mediaLibrarySession: MediaLibrarySession? = null
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var playerNotificationManager: PlayerNotificationManager? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var metadataJob: Job? = null

    // ── Continuously ticks while playing to keep notification seek bar in sync ──
    private var progressTickJob: Job? = null

    private val playerListener = object : Player.Listener {

        // Called when the user seeks or a track transition happens.
        // Immediately push the new position so the notification doesn't lag.
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updatePlaybackState(playerManager.getExoPlayer().isPlaying)
        }

        // Called when play/pause state changes.
        // Start or stop the progress ticker accordingly.
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState(isPlaying)
            if (isPlaying) startProgressTick() else stopProgressTick()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState(playerManager.getExoPlayer().isPlaying)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val player = playerManager.getExoPlayer()
        player.addListener(playerListener)

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSessionCompat = MediaSessionCompat(this, "VibeUpSession").apply {
            @Suppress("DEPRECATION")
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setSessionActivity(pendingIntent)
            // Wire transport controls back to PlayerManager
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay()               { playerManager.togglePlayPause() }
                override fun onPause()              { playerManager.togglePlayPause() }
                override fun onSkipToNext()         { playerManager.playNext() }
                override fun onSkipToPrevious()     { playerManager.playPrevious() }
                override fun onSeekTo(pos: Long)    { playerManager.seekTo(pos) }
            })
            isActive = true
        }

        mediaLibrarySession = MediaLibrarySession.Builder(
            this, player,
            object : MediaLibrarySession.Callback {

                override fun onGetLibraryRoot(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    params: LibraryParams?
                ): com.google.common.util.concurrent.ListenableFuture<LibraryResult<androidx.media3.common.MediaItem>> {
                    val rootItem = androidx.media3.common.MediaItem.Builder()
                        .setMediaId("vibeup_root")
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle("VibeUp")
                                .build()
                        )
                        .build()
                    return com.google.common.util.concurrent.Futures
                        .immediateFuture(LibraryResult.ofItem(rootItem, params))
                }

                override fun onGetChildren(
                    session: MediaLibrarySession,
                    browser: MediaSession.ControllerInfo,
                    parentId: String,
                    page: Int,
                    pageSize: Int,
                    params: LibraryParams?
                ): com.google.common.util.concurrent.ListenableFuture<LibraryResult<ImmutableList<androidx.media3.common.MediaItem>>> {
                    return com.google.common.util.concurrent.Futures.immediateFuture(
                        LibraryResult.ofItemList(ImmutableList.of(), params)
                    )
                }
            }
        )
            .setSessionActivity(pendingIntent)
            .build()

        playerNotificationManager = PlayerNotificationManager
            .Builder(this, NOTIFICATION_ID, CHANNEL_ID)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player) =
                    playerManager.currentSong.value?.title ?: "VibeUp"
                override fun createCurrentContentIntent(player: Player) = pendingIntent
                override fun getCurrentContentText(player: Player) =
                    playerManager.currentSong.value?.artist ?: ""
                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ) = null
            })
            .setNotificationListener(object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(id: Int, notification: Notification, ongoing: Boolean) {
                    notification.category   = Notification.CATEGORY_TRANSPORT
                    notification.visibility = Notification.VISIBILITY_PUBLIC
                    notification.priority   = Notification.PRIORITY_MAX
                    startForeground(id, notification)
                }
                override fun onNotificationCancelled(id: Int, dismissed: Boolean) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            .setSmallIconResourceId(R.mipmap.ic_launcher)
            .build()
            .apply {
                setPlayer(player)
                setUseNextAction(true)
                setUsePreviousAction(true)
                setUsePlayPauseActions(true)
                setUseChronometer(true)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                setColor(0xFF8B5CF6.toInt())
                setColorized(true)
                setMediaSessionToken(mediaSessionCompat!!.sessionToken)
            }

        observeMetadataAndState()
    }

    // ── Progress tick ──────────────────────────────────────────────────────────

    /**
     * Fires every 500 ms while the player is playing and calls
     * updatePlaybackState() so the notification seek bar stays in sync with
     * the actual playback position. Without this the bar only moves on
     * play/pause/seek events — i.e. it visually freezes.
     */
    private fun startProgressTick() {
        stopProgressTick()
        progressTickJob = serviceScope.launch {
            while (isActive) {
                updatePlaybackState(isPlaying = true)
                delay(500)
            }
        }
    }

    private fun stopProgressTick() {
        progressTickJob?.cancel()
        progressTickJob = null
    }

    // ── Metadata & state observers ────────────────────────────────────────────

    private fun observeMetadataAndState() {
        serviceScope.launch {
            playerManager.currentSong.collectLatest { song ->
                song?.let {
                    updateModernMetadata(it)
                    updateLegacyMetadata(it)
                }
            }
        }
        // isPlaying observer only handles play/pause; seek bar continuity is
        // handled by progressTickJob above.
        serviceScope.launch {
            playerManager.isPlaying.collectLatest { isPlaying ->
                updatePlaybackState(isPlaying)
                if (isPlaying) startProgressTick() else stopProgressTick()
            }
        }
    }

    private fun updateModernMetadata(song: com.vibeup.android.domain.model.Song) {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setDisplayTitle(song.title)
            .setSubtitle(song.artist)
            .setArtworkUri(android.net.Uri.parse(song.imageUrl))
            .build()
        playerManager.getExoPlayer().playlistMetadata = metadata
    }

    private fun updateLegacyMetadata(song: com.vibeup.android.domain.model.Song) {
        metadataJob?.cancel()
        metadataJob = serviceScope.launch(Dispatchers.IO) {
            val bitmap = try {
                BitmapFactory.decodeStream(URL(song.imageUrl).openStream())
            } catch (e: Exception) { null }

            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, song.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration * 1000L)
                .apply {
                    bitmap?.let {
                        putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
                        putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it)
                    }
                }
                .build()

            withContext(Dispatchers.Main) {
                mediaSessionCompat?.setMetadata(metadata)
                // Push a fresh playback state right after metadata so the
                // duration reported in the notification is correct immediately.
                updatePlaybackState(playerManager.getExoPlayer().isPlaying)
            }
        }
    }

    /**
     * Pushes the current playback position + state to MediaSessionCompat.
     *
     * The critical fields are:
     *   - position  → where the seek bar thumb sits
     *   - elapsedRealtimeMs → the clock reference Android uses to extrapolate
     *                         position forward between updates. Without this
     *                         the notification shows a frozen bar.
     *   - playbackSpeed → must be 1f when playing so Android advances the bar
     *                     at the correct rate between our 500 ms ticks.
     */
    private fun updatePlaybackState(isPlaying: Boolean) {
        val player = playerManager.getExoPlayer()
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING
        else            PlaybackStateCompat.STATE_PAUSED
        mediaSessionCompat?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    state,
                    player.currentPosition,       // current position in ms
                    if (isPlaying) 1f else 0f,    // speed: 1 = advance bar, 0 = freeze
                    SystemClock.elapsedRealtime()  // reference clock for interpolation
                )
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY             or
                            PlaybackStateCompat.ACTION_PAUSE            or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT     or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .build()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaLibrarySession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            playerManager.resetState()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        stopProgressTick()
        serviceScope.cancel()
        playerManager.getExoPlayer().removeListener(playerListener)
        playerNotificationManager?.setPlayer(null)
        mediaSessionCompat?.release()
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VibeUp Atomic Playback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live music controls for Atomic Island"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID    = "vibeup_origin_island_vfinal"
        const val NOTIFICATION_ID = 1001
    }
}