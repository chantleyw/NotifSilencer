package com.chantley.playfilter

import android.content.Context
import android.content.SharedPreferences

/**
 * Single source of truth for all persisted config: the log-only flag, the
 * editable ALLOW / BLOCK keyword lists, and the set of channel IDs to block.
 *
 * Everything is stored as newline-joined strings so the same storage backs the
 * runtime edit screen without any serialization ceremony.
 */
object Prefs {

    private const val FILE = "playfilter_prefs"

    private const val KEY_LOG_ONLY = "log_only"
    private const val KEY_ALLOW = "allow_list"
    private const val KEY_BLOCK = "block_list"
    private const val KEY_CHANNELS = "block_channels"

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

    private fun getList(ctx: Context, key: String, default: List<String>): List<String> {
        val raw = sp(ctx).getString(key, null) ?: return default
        return raw.split("\n").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    }

    private fun putList(ctx: Context, key: String, items: List<String>) {
        val cleaned = items.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        sp(ctx).edit().putString(key, cleaned.joinToString("\n")).apply()
    }
}
