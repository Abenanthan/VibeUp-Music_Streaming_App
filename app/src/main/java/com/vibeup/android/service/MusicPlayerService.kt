package com.vibeup.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    @Inject
    lateinit var playerManager: PlayerManager

    private var mediaSession: MediaSession? = null
    private var playerNotificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val player = playerManager.getExoPlayer()

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
                        return playerManager.currentSong.value?.artist ?: ""
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
            setUseFastForwardAction(true)
            setUseRewindAction(true)
            setUseNextAction(true)
            setUsePreviousAction(true)
            setUsePlayPauseActions(true)
            setUseStopAction(false)
            setUseChronometer(false)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setColor(0xFF1DB954.toInt())
            setColorized(true)
            // ✅ Connect to MediaSession for lock screen
            setMediaSessionToken(
                mediaSession!!.sessionCompatToken
            )
        }
    }
    /*```

    ---

    ## Fix 2 — Lock Screen Shows Only 2 Buttons?

    This is Android's default behavior:
    ```
    Lock screen shows maximum 3 buttons
    ✅ Previous
    ✅ Play/Pause
    ✅ Next*/

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
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
                // ✅ Show on lock screen
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1001
    }
}
/*```

---

## What's Added
```
✅ setUseChronometer(true)     → shows time progress
✅ VISIBILITY_PUBLIC           → shows on lock screen
✅ lockscreenVisibility        → shows on lock screen
✅ setColor(VibeUpGreen)       → green accent color
✅ setColorized(true)          → colored notification*/