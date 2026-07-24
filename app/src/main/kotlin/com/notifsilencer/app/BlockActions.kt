package com.notifsilencer.app

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.widget.Button
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
        // Show a compact preview: collapse newlines and truncate the body so a long
        // notification (e.g. a full Gmail message) can't push the buttons off-screen.
        val preview = entry.text.replace(Regex("\\s+"), " ").trim().let {
            if (it.length > 140) it.take(140).trimEnd() + "…" else it
        }
        view.findViewById<TextView>(R.id.dlgDetails).text = LogRender.formatEntry(entry.copy(text = preview))

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

    private fun toast(activity: Activity, added: Boolean, addedRes: Int) {
        Toast.makeText(
            activity,
            if (added) addedRes else R.string.action_already,
            Toast.LENGTH_SHORT
        ).show()
    }
}
