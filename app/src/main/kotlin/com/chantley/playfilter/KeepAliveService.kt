package com.chantley.playfilter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
 * to reclaim, so PlayFilterService stays connected between notifications.
 *
 * The ongoing notification is posted on a MIN-importance channel so it stays
 * silent and collapsed at the bottom of the shade.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel(this)
        startForeground(NOTIF_ID, buildNotification())
        // If the system kills us anyway, ask it to recreate the service.
        return START_STICKY
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.keepalive_title))
            .setContentText(getString(R.string.keepalive_text))
            .setOngoing(true)
            .build()

    companion object {
        const val CHANNEL_ID = "playfilter_keepalive"
        const val NOTIF_ID = 1001

        /** Safe to call repeatedly; starting an already-running service is a no-op restart. */
        fun start(ctx: Context) {
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
