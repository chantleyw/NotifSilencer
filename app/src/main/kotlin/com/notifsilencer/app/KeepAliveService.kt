package com.notifsilencer.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

/**
 * A do-nothing foreground service whose only job is to keep this app's process
 * resident. Notification listeners get killed aggressively by some OEMs
 * (Huawei/Honor especially) when the process is idle in the background; a
 * foreground service with an ongoing notification makes the process much harder
 * to reclaim, so NotifSilencerService stays connected between notifications.
 *
 * The ongoing notification is posted on a MIN-importance channel so it stays
 * silent and collapsed at the bottom of the shade, and tapping it opens the app.
 * Android requires a foreground service to show a notification, so it can't be
 * removed entirely — but [start] is guarded so it isn't re-posted (and re-sorted
 * to the top of the shade) every time the app is opened.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        startForeground(NOTIF_ID, buildNotification())
        running = true
        // If the system kills us anyway, ask it to recreate the service.
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.keepalive_title))
            .setContentText(getString(R.string.keepalive_text))
            .setContentIntent(tap)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "notifsilencer_keepalive"
        const val NOTIF_ID = 1001

        // True while the foreground service is live in this process. Reset when the
        // process (and thus the service) is killed, so start() revives it then.
        @Volatile
        private var running = false

        /**
         * Starts the keep-alive if it isn't already running. Guarded so re-opening
         * the app doesn't re-post the notification and bump it to the top of the shade.
         */
        fun start(ctx: Context) {
            if (running) return
            val i = Intent(ctx, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun ensureChannel(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                ctx.getString(R.string.keepalive_channel),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = ctx.getString(R.string.keepalive_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
