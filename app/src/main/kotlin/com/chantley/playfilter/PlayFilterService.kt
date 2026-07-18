package com.chantley.playfilter

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Watches Play Store / Play Services notifications and suppresses the
 * promotional ones while letting anything payment-related through.
 *
 * Decision order (biased toward false negatives — when in doubt, keep):
 *   1. Not from a watched package -> ignore entirely.
 *   2. ALLOW keyword matches      -> KEEP, never cancel (checked first, wins).
 *   3. BLOCK channel id matches   -> KILL.
 *   4. BLOCK keyword matches      -> KILL.
 *   5. Otherwise                  -> KEEP.
 *
 * In log-only mode (default true) nothing is ever cancelled; the would-be
 * decision is logged and recorded so real notifications can be observed first.
 */
class PlayFilterService : NotificationListenerService() {

    companion object {
        const val TAG = "PlayFilter"

        val WATCHED_PACKAGES = setOf(
            "com.android.vending",            // Play Store
            "com.google.android.gms"          // Play Services / billing
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg !in WATCHED_PACKAGES) return

        val extras = sbn.notification?.extras
        val channel = channelId(sbn)
        val haystack = buildHaystack(extras)

        val allow = Prefs.allowList(this)
        val block = Prefs.blockList(this)
        val blockChannels = Prefs.blockChannels(this)
        val logOnly = Prefs.isLogOnly(this)

        // 2. ALLOW wins outright — never cancel a payment/billing notification.
        val allowHit = allow.firstOrNull { it.isNotEmpty() && haystack.contains(it) }
        if (allowHit != null) {
            record(pkg, channel, haystack, killed = false, reason = "allow:$allowHit", logOnly)
            return
        }

        // 3. Channel-based block (most reliable when Play uses distinct channels).
        val channelHit = blockChannels.firstOrNull {
            it.isNotEmpty() && channel.lowercase() == it
        }
        // 4. Keyword-based block.
        val blockHit = block.firstOrNull { it.isNotEmpty() && haystack.contains(it) }

        val reason = when {
            channelHit != null -> "block-channel:$channelHit"
            blockHit != null -> "block:$blockHit"
            else -> null
        }

        if (reason == null) {
            // 5. No match — keep, biased toward letting it through.
            record(pkg, channel, haystack, killed = false, reason = "keep:no-match", logOnly)
            return
        }

        if (logOnly) {
            record(pkg, channel, haystack, killed = false, reason = "WOULD-KILL $reason", logOnly)
        } else {
            record(pkg, channel, haystack, killed = true, reason = reason, logOnly)
            cancelNotification(sbn.key)
        }
    }

    private fun buildHaystack(extras: android.os.Bundle?): String {
        if (extras == null) return ""
        val parts = listOf(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        )
        return parts.filterNotNull().joinToString(" ") { it.toString() }.lowercase()
    }

    private fun channelId(sbn: StatusBarNotification): String =
        sbn.notification?.channelId ?: ""

    private fun record(
        pkg: String,
        channel: String,
        text: String,
        killed: Boolean,
        reason: String,
        logOnly: Boolean
    ) {
        val mode = if (logOnly) "LOG-ONLY" else "ENFORCE"
        Log.i(
            TAG,
            "[$mode] pkg=$pkg channel=\"$channel\" killed=$killed reason=$reason text=\"$text\""
        )
        LogStore.add(
            this,
            LogStore.Entry(
                time = System.currentTimeMillis(),
                pkg = pkg,
                channel = channel,
                text = text,
                killed = killed,
                reason = reason
            )
        )
    }
}
