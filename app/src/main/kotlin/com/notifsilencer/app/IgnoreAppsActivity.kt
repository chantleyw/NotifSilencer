package com.notifsilencer.app

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView

/**
 * "Tap the apps you want ignored" screen. Lists the user's launchable apps
 * (plus anything already ignored) with a toggle each. Toggling writes to the
 * same ignore-package list the service reads, so it stays in sync with the
 * text-based editor in [KeywordEditActivity].
 *
 * App names and icons are loaded off the main thread to avoid jank on devices
 * with many apps installed.
 */
class IgnoreAppsActivity : Activity() {

    private data class AppRow(val pkg: String, val label: String, val icon: Drawable)

    private lateinit var listView: ListView
    private lateinit var loading: TextView
    private val rows = ArrayList<AppRow>()
    private val ignore = HashSet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ignore_apps)
        listView = findViewById(R.id.appList)
        loading = findViewById(R.id.loading)

        ignore.addAll(Prefs.ignorePackages(this))

        Thread {
            val pm = packageManager
            @Suppress("DEPRECATION")
            val apps = pm.getInstalledApplications(0)
            val loaded = apps.mapNotNull { ai ->
                val launchable = pm.getLaunchIntentForPackage(ai.packageName) != null
                // Show user-launchable apps, plus anything already on the ignore list.
                if (!launchable && ignore.none { ai.packageName.startsWith(it) }) return@mapNotNull null
                AppRow(
                    ai.packageName,
                    pm.getApplicationLabel(ai).toString(),
                    pm.getApplicationIcon(ai)
                )
            }.sortedBy { it.label.lowercase() }

            runOnUiThread {
                rows.clear()
                rows.addAll(loaded)
                loading.visibility = View.GONE
                listView.adapter = AppAdapter()
            }
        }.start()
    }

    private fun isIgnored(pkg: String): Boolean = ignore.any { pkg.startsWith(it) }

    private fun setIgnored(pkg: String, on: Boolean) {
        if (on) {
            if (!isIgnored(pkg)) ignore.add(pkg)
        } else {
            // Remove every entry that causes this package to be ignored.
            ignore.removeAll { pkg.startsWith(it) }
        }
        Prefs.setIgnorePackages(this, ignore.toList())
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount(): Int = rows.size
        override fun getItem(position: Int): Any = rows[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@IgnoreAppsActivity)
                .inflate(R.layout.row_app, parent, false)
            val row = rows[position]
            view.findViewById<ImageView>(R.id.appIcon).setImageDrawable(row.icon)
            view.findViewById<TextView>(R.id.appLabel).text = row.label
            view.findViewById<TextView>(R.id.appPkg).text = row.pkg

            val sw = view.findViewById<Switch>(R.id.appSwitch)
            // Clear the listener before setting state so recycled rows don't fire it.
            sw.setOnCheckedChangeListener(null)
            sw.isChecked = isIgnored(row.pkg)
            sw.setOnCheckedChangeListener { _, checked -> setIgnored(row.pkg, checked) }

            // Make the whole row toggle the switch.
            view.setOnClickListener { sw.toggle() }
            return view
        }
    }
}
