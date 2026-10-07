package com.glassroom.music.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
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

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "glassroom_music_playback"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val app = application as? GlassroomApplication
        val playerManager = app?.container?.playerManager

        if (playerManager != null) {
            val sessionActivityPendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            mediaSession = MediaSession.Builder(this, playerManager.exoPlayer)
                .setSessionActivity(sessionActivityPendingIntent)
                .build()

            // Connect DefaultMediaNotificationProvider for ongoing MediaStyle notification & Dynamic Island
            setMediaNotificationProvider(
                DefaultMediaNotificationProvider.Builder(this)
                    .setChannelId(NOTIFICATION_CHANNEL_ID)
                    .setChannelName(R.string.app_name)
                    .setNotificationId(NOTIFICATION_ID)
                    .build()
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
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
                NOTIFICATION_CHANNEL_ID,
                "Glassroom Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing song in Glassroom"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
