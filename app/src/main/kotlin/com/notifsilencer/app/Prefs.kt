package com.notifsilencer.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single source of truth for all persisted config: the log-only flag, the
 * editable ALLOW / BLOCK keyword lists, and the set of channel IDs to block.
 *
 * Everything is stored as newline-joined strings so the same storage backs the
 * runtime edit screen without any serialization ceremony.
 */
object Prefs {

    private const val FILE = "notifsilencer_prefs"

    private const val KEY_LOG_ONLY = "log_only"
    private const val KEY_ALLOW = "allow_list"
    private const val KEY_BLOCK = "block_list"
    private const val KEY_CHANNELS = "block_channels"
    private const val KEY_IGNORE = "ignore_packages"

    // Bias toward false negatives: if a payment-ish word appears, we never cancel.
    val DEFAULT_ALLOW = listOf(
        "payment", "purchase", "receipt", "refund", "order", "subscription",
        "renew", "billing", "card", "charged", "declined", "expire", "expired",
        "invoice", "transaction", "payment method", "past due", "failed payment"
    )

    val DEFAULT_BLOCK = listOf(
        "recommend", "for you", "deal", "deals", "offer", "sale", "discount",
        "top charts", "trending", "new game", "new games", "editors' choice",
        "editors choice", "free trial", "promo", "promotion", "check out",
        "you might like", "popular", "just launched", "back in the game",
        "based on your", "explore", "discover", "coupon", "% off"
    )

    // Empty by default — populate once you have observed real Play Store channel IDs
    // from log-only mode. Channel match is exact (case-insensitive).
    val DEFAULT_CHANNELS = emptyList<String>()

    // Packages skipped entirely: never logged, never matched, never cancelled.
    // Matched by prefix, so "com.whatsapp" also covers "com.whatsapp.w4b" (Business).
    val DEFAULT_IGNORE_PACKAGES = listOf(
        "com.whatsapp"
    )

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isLogOnly(ctx: Context): Boolean =
        sp(ctx).getBoolean(KEY_LOG_ONLY, true)

    fun setLogOnly(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean(KEY_LOG_ONLY, value).apply()
    }

    fun allowList(ctx: Context): List<String> = getList(ctx, KEY_ALLOW, DEFAULT_ALLOW)

    fun blockList(ctx: Context): List<String> = getList(ctx, KEY_BLOCK, DEFAULT_BLOCK)

    fun blockChannels(ctx: Context): List<String> = getList(ctx, KEY_CHANNELS, DEFAULT_CHANNELS)

    fun setAllowList(ctx: Context, items: List<String>) = putList(ctx, KEY_ALLOW, items)

    fun setBlockList(ctx: Context, items: List<String>) = putList(ctx, KEY_BLOCK, items)

    fun setBlockChannels(ctx: Context, items: List<String>) = putList(ctx, KEY_CHANNELS, items)

    fun ignorePackages(ctx: Context): List<String> = getList(ctx, KEY_IGNORE, DEFAULT_IGNORE_PACKAGES)

    fun setIgnorePackages(ctx: Context, items: List<String>) = putList(ctx, KEY_IGNORE, items)

    // --- Backup / restore ------------------------------------------------------

    private const val EXPORT_VERSION = 1

    /** All user settings as a pretty JSON string, for export to a file. */
    fun exportJson(ctx: Context): String {
        return JSONObject().apply {
            put("version", EXPORT_VERSION)
            put("app", "NotifSilencer")
            put(KEY_LOG_ONLY, isLogOnly(ctx))
            put(KEY_ALLOW, JSONArray(allowList(ctx)))
            put(KEY_BLOCK, JSONArray(blockList(ctx)))
            put(KEY_CHANNELS, JSONArray(blockChannels(ctx)))
            put(KEY_IGNORE, JSONArray(ignorePackages(ctx)))
        }.toString(2)
    }

    /** Applies settings from an exported JSON string. Only keys present are changed. */
    fun importJson(ctx: Context, json: String) {
        val o = JSONObject(json)
        if (o.has(KEY_LOG_ONLY)) setLogOnly(ctx, o.getBoolean(KEY_LOG_ONLY))
        if (o.has(KEY_ALLOW)) setAllowList(ctx, jsonToList(o.getJSONArray(KEY_ALLOW)))
        if (o.has(KEY_BLOCK)) setBlockList(ctx, jsonToList(o.getJSONArray(KEY_BLOCK)))
        if (o.has(KEY_CHANNELS)) setBlockChannels(ctx, jsonToList(o.getJSONArray(KEY_CHANNELS)))
        if (o.has(KEY_IGNORE)) setIgnorePackages(ctx, jsonToList(o.getJSONArray(KEY_IGNORE)))
    }

    private fun jsonToList(arr: JSONArray): List<String> {
        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) out.add(arr.getString(i))
        return out
    }

    private fun getList(ctx: Context, key: String, default: List<String>): List<String> {
        val raw = sp(ctx).getString(key, null) ?: return default
        return raw.split("\n").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    }

    private fun putList(ctx: Context, key: String, items: List<String>) {
        val cleaned = items.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        sp(ctx).edit().putString(key, cleaned.joinToString("\n")).apply()
    }
}
