package com.notifsilencer.app

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared formatting for log entries so the main log and the blocked-history
 * screen render identically. Colours + bolds only the verdict token.
 */
object LogRender {

    private val timeFmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    private const val GREEN = 0xFF2E7D32.toInt()   // KEPT
    private const val RED = 0xFFC62828.toInt()      // KILLED
    private const val ORANGE = 0xFFEF6C00.toInt()   // WOULD-KILL (log-only)

    fun format(entries: List<LogStore.Entry>): CharSequence {
        val sb = SpannableStringBuilder()
        for (e in entries) {
            val (verdict, colour) = when {
                e.killed -> "KILLED" to RED
                e.reason.startsWith("WOULD-KILL") -> "WOULD-KILL" to ORANGE
                else -> "KEPT" to GREEN
            }
            sb.append(timeFmt.format(Date(e.time))).append("  ")
            val start = sb.length
            sb.append(verdict)
            sb.setSpan(ForegroundColorSpan(colour), start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("  ").append(e.reason).append('\n')
                .append("  pkg=").append(e.pkg).append('\n')
                .append("  ch=").append(if (e.channel.isEmpty()) "(none)" else e.channel).append('\n')
                .append("  ").append(e.text.ifEmpty { "(no text)" }).append("\n\n")
        }
        while (sb.isNotEmpty() && sb.last() == '\n') sb.delete(sb.length - 1, sb.length)
        return sb
    }
}
