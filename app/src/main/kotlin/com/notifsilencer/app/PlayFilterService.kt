package com.notifsilencer.app

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Inspects notifications from EVERY app and suppresses promotional /
 * "recommendations" ones while always letting anything payment-related through.
 *
 * Blanket watching is deliberate: the target notifications ("Recommendations",
 * offers, etc.) can be posted by many different packages, so restricting to a
 * fixed package list silently missed them. The ALLOW list, checked first, is
 * what keeps this safe — a payment/billing notification is never cancelled.
 *
 * Decision order (biased toward false negatives — when in doubt, keep):
 *   1. ALLOW keyword matches      -> KEEP, never cancel (checked first, wins).
 *   2. BLOCK channel id matches   -> KILL.
 *   3. BLOCK keyword matches      -> KILL.
 *   4. Otherwise                  -> KEEP.
 *
 * In log-only mode (default true) nothing is ever cancelled; the would-be
 * decision is logged and recorded so real notifications can be observed first.
 */
class PlayFilterService : NotificationListenerService() {

    companion object {
        const val TAG = "PlayFilter"
    }

    override fun onListenerConnected() {
        // Keep the process resident so aggressive OEM battery managers (Huawei,
        // Xiaomi, etc.) can't kill the listener between notifications.
        KeepAliveService.start(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName

        // Ignored packages are skipped entirely — never logged, matched, or cancelled.
        // Prefix match so "com.whatsapp" also covers "com.whatsapp.w4b".
        val ignore = Prefs.ignorePackages(this)
        if (ignore.any { it.isNotEmpty() && pkg.startsWith(it) }) return

        val extras = sbn.notification?.extras
        val channel = channelId(sbn)
        val haystack = buildHaystack(extras)

        // Keyword matching runs against the visible text AND the channel id: some
        // OEM push agents (e.g. Honor's, channel "...RECOMMEND...") put the only
        // usable signal in the channel id rather than the title/body text.
        val matchText = (haystack + " " + channel).lowercase()

        val allow = Prefs.allowList(this)
        val block = Prefs.blockList(this)
        val blockChannels = Prefs.blockChannels(this)
        val logOnly = Prefs.isLogOnly(this)

        // 1. ALLOW wins outright — never cancel a payment/billing notification.
        val allowHit = allow.firstOrNull { it.isNotEmpty() && matchText.contains(it) }
        if (allowHit != null) {
            record(pkg, channel, haystack, killed = false, reason = "allow:$allowHit", logOnly)
            return
        }

        // 2. Channel-based block. Substring match, because channel ids often carry
        // a variable per-notification suffix (e.g. "...RECOMMEND.03pnc").
        val channelHit = blockChannels.firstOrNull {
            it.isNotEmpty() && channel.lowercase().contains(it)
        }
        // 3. Keyword-based block (text + channel id).
        val blockHit = block.firstOrNull { it.isNotEmpty() && matchText.contains(it) }

        val reason = when {
            channelHit != null -> "block-channel:$channelHit"
            blockHit != null -> "block:$blockHit"
            else -> null
        }

        if (reason == null) {
            // 4. No match — keep, biased toward letting it through.
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
