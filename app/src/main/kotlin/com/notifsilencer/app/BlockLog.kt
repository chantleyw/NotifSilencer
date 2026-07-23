package com.notifsilencer.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A separate, longer-lived history of block decisions only (KILLED, and
 * WOULD-KILL in log-only mode). It is independent of [LogStore], so clearing
 * the main log does NOT wipe this — it exists so you can verify the app is
 * catching things even after clearing the noisy full log.
 *
 * Same JSON-in-SharedPreferences approach as LogStore, reusing LogStore.Entry.
 */
object BlockLog {

    private const val FILE = "notifsilencer_blocklog"
    private const val KEY = "entries"
    const val MAX = 500

    @Synchronized
    fun add(ctx: Context, entry: LogStore.Entry) {
        val arr = readArray(ctx)
        val obj = JSONObject().apply {
            put("time", entry.time)
            put("pkg", entry.pkg)
            put("channel", entry.channel)
            put("text", entry.text)
            put("killed", entry.killed)
            put("reason", entry.reason)
        }
        val trimmed = JSONArray()
        trimmed.put(obj)
        var i = 0
        while (i < arr.length() && trimmed.length() < MAX) {
            trimmed.put(arr.getJSONObject(i))
            i++
        }
        sp(ctx).edit().putString(KEY, trimmed.toString()).apply()
    }

    @Synchronized
    fun all(ctx: Context): List<LogStore.Entry> {
        val arr = readArray(ctx)
        val out = ArrayList<LogStore.Entry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                LogStore.Entry(
                    time = o.optLong("time"),
                    pkg = o.optString("pkg"),
                    channel = o.optString("channel"),
                    text = o.optString("text"),
                    killed = o.optBoolean("killed"),
                    reason = o.optString("reason")
                )
            )
        }
        return out
    }

    @Synchronized
    fun clear(ctx: Context) {
        sp(ctx).edit().remove(KEY).apply()
    }

    private fun readArray(ctx: Context): JSONArray {
        val raw = sp(ctx).getString(KEY, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
