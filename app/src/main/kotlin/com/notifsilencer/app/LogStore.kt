package com.notifsilencer.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * A tiny ring buffer of the last [MAX] intercepted notifications, persisted to
 * SharedPreferences as a JSON array (org.json ships with the platform, so no
 * third-party dependency). Newest entries are at the front of the returned list.
 */
object LogStore {

    private const val FILE = "playfilter_log"
    private const val KEY = "entries"
    const val MAX = 100

    data class Entry(
        val time: Long,
        val pkg: String,
        val channel: String,
        val text: String,
        val killed: Boolean,
        val reason: String
    )

    @Synchronized
    fun add(ctx: Context, entry: Entry) {
        val arr = readArray(ctx)
        val obj = JSONObject().apply {
            put("time", entry.time)
            put("pkg", entry.pkg)
            put("channel", entry.channel)
            put("text", entry.text)
            put("killed", entry.killed)
            put("reason", entry.reason)
        }
        // Prepend newest, then trim the tail.
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
    fun all(ctx: Context): List<Entry> {
        val arr = readArray(ctx)
        val out = ArrayList<Entry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Entry(
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
