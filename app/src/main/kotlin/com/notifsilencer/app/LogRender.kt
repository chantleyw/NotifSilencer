package com.notifsilencer.app

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared formatting for log entries so the main log and the blocked-history
 * screen render identically. Colours + bolds only the verdict token.
 */
object LogRender {

    private val timeFmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    // Brightened for legibility on the dark navy background.
    private const val GREEN = 0xFF34D399.toInt()   // KEPT
    private const val RED = 0xFFFF5C5C.toInt()      // KILLED
    private const val ORANGE = 0xFFFBBF24.toInt()   // WOULD-KILL (log-only)

    /** One entry, coloured verdict token, no trailing blank line. */
    fun formatEntry(e: LogStore.Entry): CharSequence {
        val (verdict, colour) = when {
            e.killed -> "KILLED" to RED
            e.reason.startsWith("WOULD-KILL") -> "WOULD-KILL" to ORANGE
            else -> "KEPT" to GREEN
        }
        val sb = SpannableStringBuilder()
        sb.append(timeFmt.format(Date(e.time))).append("  ")
        val start = sb.length
        sb.append(verdict)
        sb.setSpan(ForegroundColorSpan(colour), start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Collapse whitespace and truncate the body — a full email/message would
        // otherwise make rows huge. The channel id (the key for blocking) is shown
        // in full above; the text here is just a preview to identify the notification.
        val preview = e.text.replace(Regex("\\s+"), " ").trim().let {
            if (it.length > TEXT_PREVIEW_MAX) it.take(TEXT_PREVIEW_MAX).trimEnd() + "…" else it
        }
        sb.append("  ").append(e.reason).append('\n')
            .append("  pkg=").append(e.pkg).append('\n')
            .append("  ch=").append(if (e.channel.isEmpty()) "(none)" else e.channel).append('\n')
            .append("  ").append(preview.ifEmpty { "(no text)" })

        // Highlight what actually caused the decision — the matched channel substring,
        // keyword, or package — wherever it appears, so the cause is obvious at a glance.
        highlightCause(sb, e.reason)
        return sb
    }

    /** Underlines + colours every occurrence of the token that triggered the block/allow. */
    private fun highlightCause(sb: SpannableStringBuilder, reason: String) {
        val r = reason.removePrefix("WOULD-KILL ").trim()
        val colon = r.indexOf(':')
        if (colon < 0) return
        val type = r.substring(0, colon)
        val token = r.substring(colon + 1).trim()
        if (token.isEmpty() || token == "no-match") return
        val colour = when (type) {
            "allow" -> GREEN
            else -> RED   // block, block-channel, block-pkg, force-block
        }
        val hay = sb.toString().lowercase()
        val needle = token.lowercase()
        var i = hay.indexOf(needle)
        while (i >= 0) {
            sb.setSpan(ForegroundColorSpan(colour), i, i + needle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(UnderlineSpan(), i, i + needle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            i = hay.indexOf(needle, i + needle.length)
        }
    }

    private const val TEXT_PREVIEW_MAX = 160

    /** Case-insensitive filter over text, package, channel, and reason. Empty query = all. */
    fun filter(entries: List<LogStore.Entry>, query: String): List<LogStore.Entry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return entries
        return entries.filter {
            it.text.lowercase().contains(q) ||
                it.pkg.lowercase().contains(q) ||
                it.channel.lowercase().contains(q) ||
                it.reason.lowercase().contains(q)
        }
    }

    fun format(entries: List<LogStore.Entry>): CharSequence {
        val sb = SpannableStringBuilder()
        for (e in entries) sb.append(formatEntry(e)).append("\n\n")
        while (sb.isNotEmpty() && sb.last() == '\n') sb.delete(sb.length - 1, sb.length)
        return sb
    }
}
