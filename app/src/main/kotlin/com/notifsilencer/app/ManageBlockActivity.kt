package com.notifsilencer.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView

/**
 * A managed (non-typing) view of the block rules: blocked channels and blocked
 * apps, each with a Remove button. New entries are usually added by tapping a
 * notification in the log; this screen also allows manual add and removal.
 * (Keyword and ALLOW lists remain in the text editor.)
 */
class ManageBlockActivity : Activity() {

    private data class Item(val isChannel: Boolean, val value: String)

    private lateinit var list: ListView
    private lateinit var empty: TextView
    private val items = ArrayList<Item>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manageblock)
        list = findViewById(R.id.blockList)
        empty = findViewById(R.id.emptyView)
        list.adapter = Adapter()

        findViewById<Button>(R.id.btnAddChannel).setOnClickListener {
            promptAdd(R.string.add_channel, isChannel = true)
        }
        findViewById<Button>(R.id.btnAddApp).setOnClickListener {
            promptAdd(R.string.add_app, isChannel = false)
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        items.clear()
        Prefs.blockChannels(this).forEach { items.add(Item(true, it)) }
        Prefs.blockPackages(this).forEach { items.add(Item(false, it)) }
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        (list.adapter as Adapter).notifyDataSetChanged()
    }

    private fun remove(item: Item) {
        if (item.isChannel) {
            Prefs.setBlockChannels(this, Prefs.blockChannels(this).filterNot { it == item.value })
        } else {
            Prefs.setBlockPackages(this, Prefs.blockPackages(this).filterNot { it == item.value })
        }
        reload()
    }

    private fun promptAdd(titleRes: Int, isChannel: Boolean) {
        val input = EditText(this).apply {
            hint = getString(if (isChannel) R.string.hint_channel else R.string.hint_app)
        }
        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val value = input.text.toString()
                if (isChannel) {
                    Prefs.addTo(this, Prefs.blockChannels(this), value, Prefs::setBlockChannels)
                } else {
                    Prefs.addTo(this, Prefs.blockPackages(this), value, Prefs::setBlockPackages)
                }
                reload()
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Item = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@ManageBlockActivity)
                .inflate(R.layout.row_block, parent, false)
            val item = items[position]
            view.findViewById<TextView>(R.id.blockType).setText(
                if (item.isChannel) R.string.type_channel else R.string.type_app
            )
            view.findViewById<TextView>(R.id.blockValue).text = item.value
            view.findViewById<Button>(R.id.btnRemove).setOnClickListener { remove(item) }
            return view
        }
    }
}
