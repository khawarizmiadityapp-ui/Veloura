package com.glassroom.music.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.glassroom.music.GlassroomApplication
import com.glassroom.music.MainActivity
import com.glassroom.music.R

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var playerManagerRef: PlayerManager? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "veloura_music_playback_live"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val app = application as? GlassroomApplication
        val playerManager = app?.container?.playerManager
        playerManagerRef = playerManager

        if (playerManager != null) {
            val sessionActivityPendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val forwardingPlayer = VelouraForwardingPlayer(playerManager)
            mediaSession = MediaSession.Builder(this, forwardingPlayer)
                .setSessionActivity(sessionActivityPendingIntent)
                .build()

            addSession(mediaSession!!)

            // Connect DefaultMediaNotificationProvider for ongoing MediaStyle notification & Dynamic Island
            val provider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setChannelName(R.string.app_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
            provider.setSmallIcon(R.drawable.ic_launcher_foreground)
            setMediaNotificationProvider(provider)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        val isTransitioning = playerManagerRef?.isLoading?.value == true
        if (!isTransitioning && (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == androidx.media3.common.Player.STATE_ENDED)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            removeSession(this)
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Veloura Playback",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows currently playing song in Veloura & Dynamic Island"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private class VelouraForwardingPlayer(
        private val playerManager: PlayerManager
    ) : ForwardingPlayer(playerManager.exoPlayer) {

        override fun isCommandAvailable(command: Int): Boolean {
            if (playerManager.queue.value.isNotEmpty() && command in MEDIA_QUEUE_COMMANDS) {
                return true
            }
            return super.isCommandAvailable(command)
        }

        override fun getAvailableCommands(): Player.Commands {
            val base = super.getAvailableCommands()
            if (playerManager.queue.value.isEmpty()) return base
            return base.buildUpon()
                .addAll(*MEDIA_QUEUE_COMMANDS)
                .build()
        }

        override fun hasNextMediaItem(): Boolean = playerManager.hasNextTrack()

        override fun hasPreviousMediaItem(): Boolean = playerManager.queue.value.isNotEmpty()

        override fun seekToNext() {
            playerManager.skipNext()
        }

        override fun seekToNextMediaItem() {
            playerManager.skipNext()
        }

        override fun seekToPrevious() {
            playerManager.skipPrevious()
        }

        override fun seekToPreviousMediaItem() {
            playerManager.skipPrevious()
        }

        override fun play() {
            playerManager.resumeOrPlayCurrent()
        }

        companion object {
            private val MEDIA_QUEUE_COMMANDS = intArrayOf(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
            )
        }
    }
}
