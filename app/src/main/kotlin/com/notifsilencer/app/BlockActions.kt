package com.notifsilencer.app

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

/**
 * The "tap a log entry to act on it" dialog. Shows the notification's details
 * and offers explicit one-tap actions — block its channel, block its app, or
 * ignore its app — each button labelled with the exact value it will add, so
 * the user chooses precisely what goes on the list without typing.
 *
 * Uses a custom view (not setMessage + setItems, which can't coexist).
 */
object BlockActions {

    fun show(activity: Activity, entry: LogStore.Entry, onChanged: () -> Unit) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_block_actions, null)
        // formatEntry already collapses + truncates the body, and the layout caps
        // the detail to 8 lines, so the action buttons always stay visible.
        view.findViewById<TextView>(R.id.dlgDetails).text = LogRender.formatEntry(entry)

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.action_title)
            .setView(view)
            .setNegativeButton(R.string.action_close, null)
            .create()

        val channelBtn = view.findViewById<Button>(R.id.dlgBlockChannel)
        if (entry.channel.isEmpty()) {
            channelBtn.visibility = View.GONE
        } else {
            channelBtn.text = activity.getString(R.string.action_block_channel) + "\n" + entry.channel
            channelBtn.setOnClickListener {
                val added = Prefs.addTo(
                    activity, Prefs.blockChannels(activity), entry.channel, Prefs::setBlockChannels
                )
                toast(activity, added, R.string.action_added_channel)
                onChanged(); dialog.dismiss()
            }
        }

        val appBtn = view.findViewById<Button>(R.id.dlgBlockApp)
        appBtn.text = activity.getString(R.string.action_block_app) + "\n" + entry.pkg
        appBtn.setOnClickListener {
            val added = Prefs.addTo(
                activity, Prefs.blockPackages(activity), entry.pkg, Prefs::setBlockPackages
            )
            toast(activity, added, R.string.action_added_app)
            onChanged(); dialog.dismiss()
        }

        val keywordBtn = view.findViewById<Button>(R.id.dlgBlockKeyword)
        keywordBtn.text = activity.getString(R.string.action_block_keyword)
        keywordBtn.setOnClickListener {
            dialog.dismiss()
            promptKeyword(activity, entry, onChanged)
        }

        val ignoreBtn = view.findViewById<Button>(R.id.dlgIgnoreApp)
        ignoreBtn.text = activity.getString(R.string.action_ignore_app) + "\n" + entry.pkg
        ignoreBtn.setOnClickListener {
            val added = Prefs.addTo(
                activity, Prefs.ignorePackages(activity), entry.pkg, Prefs::setIgnorePackages
            )
            toast(activity, added, R.string.action_added_ignore)
            onChanged(); dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * "Block messages containing this text" — adds a phrase to the BLOCK keyword
     * list, which matches the notification text (and channel id) of ANY app. Useful
     * for recurring spam whose wording stays the same while the package/channel change.
     * Pre-fills with the notification text so the user can trim it to a distinctive phrase.
     */
    private fun promptKeyword(activity: Activity, entry: LogStore.Entry, onChanged: () -> Unit) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_keyword, null)
        val input = view.findViewById<EditText>(R.id.kwInput)
        val override = view.findViewById<CheckBox>(R.id.kwOverride)
        input.setText(entry.text.replace(Regex("\\s+"), " ").trim().take(60))
        input.setSelection(input.text.length)
        // Pre-tick the override if this notification was kept by the ALLOW list,
        // since a plain block keyword wouldn't catch it otherwise.
        override.isChecked = entry.reason.startsWith("allow:")

        AlertDialog.Builder(activity)
            .setTitle(R.string.action_block_keyword)
            .setView(view)
            .setPositiveButton(R.string.add) { _, _ ->
                val value = input.text.toString()
                val added = if (override.isChecked) {
                    Prefs.addTo(activity, Prefs.forceBlockList(activity), value, Prefs::setForceBlockList)
                } else {
                    Prefs.addTo(activity, Prefs.blockList(activity), value, Prefs::setBlockList)
                }
                val msg = if (override.isChecked) R.string.action_added_override else R.string.action_added_keyword
                toast(activity, added, msg)
                onChanged()
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }

    private fun toast(activity: Activity, added: Boolean, addedRes: Int) {
        Toast.makeText(
            activity,
            if (added) addedRes else R.string.action_already,
            Toast.LENGTH_SHORT
        ).show()
    }
}
