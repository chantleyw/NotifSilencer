package com.notifsilencer.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts the keep-alive service after a reboot. Android re-binds the
 * notification listener itself on boot, but on OEMs that delay that, starting
 * the foreground service here nudges the process back to life. (Starting a
 * foreground service from BOOT_COMPLETED is permitted.)
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            KeepAliveService.start(context)
        }
    }
}
