package com.notifsilencer.app

import android.app.Activity
import android.app.AlertDialog
import android.widget.Toast

/**
 * The "tap a log entry to act on it" dialog. Shows the notification's details
 * and offers one-tap actions to add it to the block or ignore lists, so the
 * user never has to type a package name or channel id by hand.
 */
object BlockActions {

    fun show(activity: Activity, entry: LogStore.Entry, onChanged: () -> Unit) {
        val labels = ArrayList<String>()
        val actions = ArrayList<() -> Unit>()

        if (entry.channel.isNotEmpty()) {
            labels.add(activity.getString(R.string.action_block_channel))
            actions.add {
                val added = Prefs.addTo(
                    activity, Prefs.blockChannels(activity), entry.channel, Prefs::setBlockChannels
                )
                toast(activity, added, R.string.action_added_channel, R.string.action_already)
                onChanged()
            }
        }

        labels.add(activity.getString(R.string.action_block_app))
        actions.add {
            val added = Prefs.addTo(
                activity, Prefs.blockPackages(activity), entry.pkg, Prefs::setBlockPackages
            )
            toast(activity, added, R.string.action_added_app, R.string.action_already)
            onChanged()
        }

        labels.add(activity.getString(R.string.action_ignore_app))
        actions.add {
            val added = Prefs.addTo(
                activity, Prefs.ignorePackages(activity), entry.pkg, Prefs::setIgnorePackages
            )
            toast(activity, added, R.string.action_added_ignore, R.string.action_already)
            onChanged()
        }

        val details = LogRender.formatEntry(entry)
        AlertDialog.Builder(activity)
            .setTitle(R.string.action_title)
            .setMessage(details)
            .setItems(labels.toTypedArray()) { _, which -> actions[which]() }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }

    private fun toast(activity: Activity, added: Boolean, addedRes: Int, alreadyRes: Int) {
        Toast.makeText(activity, if (added) addedRes else alreadyRes, Toast.LENGTH_SHORT).show()
    }
}
