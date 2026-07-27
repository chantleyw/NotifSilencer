package com.notifsilencer.app

import android.app.Notification
import android.os.Handler
import android.os.Looper
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
class NotifSilencerService : NotificationListenerService() {

    companion object {
        const val TAG = "NotifSilencer"
    }

    private val handler = Handler(Looper.getMainLooper())

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
        val blockPackages = Prefs.blockPackages(this)
        val forceBlock = Prefs.forceBlockList(this)
        val logOnly = Prefs.isLogOnly(this)

        // 1. FORCE-BLOCK overrides everything, including ALLOW — for wanted-looking
        // messages that aren't yours (e.g. a payment SMS for the number's old owner).
        val forceHit = forceBlock.firstOrNull { it.isNotEmpty() && matchText.contains(it) }

        // 2. ALLOW wins next — never cancel a payment/billing notification — UNLESS a
        // force-block phrase matched above.
        if (forceHit == null) {
            val allowHit = allow.firstOrNull { it.isNotEmpty() && matchText.contains(it) }
            if (allowHit != null) {
                record(pkg, channel, haystack, killed = false, reason = "allow:$allowHit", logOnly)
                return
            }
        }

        // 3. Package-based block — cancel everything from this app.
        val pkgHit = blockPackages.firstOrNull { it.isNotEmpty() && pkg.startsWith(it) }
        // 4. Channel-based block. Substring match, because channel ids often carry
        // a variable per-notification suffix (e.g. "...RECOMMEND.03pnc").
        val channelHit = blockChannels.firstOrNull {
            it.isNotEmpty() && channel.lowercase().contains(it)
        }
        // 5. Keyword-based block (text + channel id).
        val blockHit = block.firstOrNull { it.isNotEmpty() && matchText.contains(it) }

        val reason = when {
            forceHit != null -> "force-block:$forceHit"
            pkgHit != null -> "block-pkg:$pkgHit"
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
            record(pkg, channel, haystack, killed = false, reason = "WOULD-KILL $reason", logOnly, blocked = true)
        } else {
            // Notifications flagged ongoing / no-clear / foreground-service, or that
            // report themselves not clearable, CANNOT be cancelled by any listener —
            // that's an Android restriction. Annotate so it's visible in the log.
            val flags = sbn.notification?.flags ?: 0
            val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0
            val noClear = flags and Notification.FLAG_NO_CLEAR != 0
            val fgs = flags and Notification.FLAG_FOREGROUND_SERVICE != 0
            val stuck = ongoing || noClear || fgs || !sbn.isClearable
            val note = if (stuck) {
                " [may not clear: ongoing=$ongoing noClear=$noClear fgs=$fgs clearable=${sbn.isClearable}]"
            } else ""
            record(pkg, channel, haystack, killed = true, reason = reason + note, logOnly, blocked = true)
            cancelNotification(sbn.key)
            // Some apps re-post the notification right after it's cancelled; retry a
            // couple of times to catch those.
            handler.postDelayed({ cancelNotification(sbn.key) }, 400)
            handler.postDelayed({ cancelNotification(sbn.key) }, 1200)
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
        logOnly: Boolean,
        blocked: Boolean = false
    ) {
        val mode = if (logOnly) "LOG-ONLY" else "ENFORCE"
        Log.i(
            TAG,
            "[$mode] pkg=$pkg channel=\"$channel\" killed=$killed reason=$reason text=\"$text\""
        )
        val entry = LogStore.Entry(
            time = System.currentTimeMillis(),
            pkg = pkg,
            channel = channel,
            text = text,
            killed = killed,
            reason = reason
        )
        LogStore.add(this, entry)
        // Block decisions also go to the persistent, separately-cleared history.
        if (blocked) BlockLog.add(this, entry)
    }
}
