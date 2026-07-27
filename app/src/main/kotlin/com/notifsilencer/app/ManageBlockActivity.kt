package com.notifsilencer.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView

/**
 * A managed (non-typing) view of every block rule: blocked keywords, override
 * keywords, channels, and apps — each with a Remove button. New entries are
 * usually added by tapping a notification in the log; this screen shows them all
 * and allows manual add/remove. (The ALLOW list stays in the text editor.)
 */
class ManageBlockActivity : Activity() {

    private enum class Type { KEYWORD, OVERRIDE, CHANNEL, APP }

    private data class Item(val type: Type, val value: String)

    private lateinit var list: ListView
    private lateinit var empty: TextView
    private lateinit var typeFilter: Spinner
    private val items = ArrayList<Item>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manageblock)
        list = findViewById(R.id.blockList)
        empty = findViewById(R.id.emptyView)
        typeFilter = findViewById(R.id.typeFilter)
        list.adapter = Adapter()

        typeFilter.adapter = ArrayAdapter.createFromResource(
            this, R.array.blocktype_filters, android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        typeFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = reload()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.btnAddChannel).setOnClickListener {
            promptAdd(R.string.add_channel, Type.CHANNEL)
        }
        findViewById<Button>(R.id.btnAddApp).setOnClickListener {
            promptAdd(R.string.add_app, Type.APP)
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        items.clear()
        // Spinner: 0 All, 1 Keyword, 2 Override, 3 Channel, 4 App
        val sel = typeFilter.selectedItemPosition
        if (sel == 0 || sel == 1) Prefs.blockList(this).forEach { items.add(Item(Type.KEYWORD, it)) }
        if (sel == 0 || sel == 2) Prefs.forceBlockList(this).forEach { items.add(Item(Type.OVERRIDE, it)) }
        if (sel == 0 || sel == 3) Prefs.blockChannels(this).forEach { items.add(Item(Type.CHANNEL, it)) }
        if (sel == 0 || sel == 4) Prefs.blockPackages(this).forEach { items.add(Item(Type.APP, it)) }
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        (list.adapter as Adapter).notifyDataSetChanged()
    }

    private fun remove(item: Item) {
        when (item.type) {
            Type.KEYWORD -> Prefs.setBlockList(this, Prefs.blockList(this).filterNot { it == item.value })
            Type.OVERRIDE -> Prefs.setForceBlockList(this, Prefs.forceBlockList(this).filterNot { it == item.value })
            Type.CHANNEL -> Prefs.setBlockChannels(this, Prefs.blockChannels(this).filterNot { it == item.value })
            Type.APP -> Prefs.setBlockPackages(this, Prefs.blockPackages(this).filterNot { it == item.value })
        }
        reload()
    }

    private fun promptAdd(titleRes: Int, type: Type) {
        val input = EditText(this).apply {
            hint = getString(if (type == Type.CHANNEL) R.string.hint_channel else R.string.hint_app)
        }
        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val value = input.text.toString()
                if (type == Type.CHANNEL) {
                    Prefs.addTo(this, Prefs.blockChannels(this), value, Prefs::setBlockChannels)
                } else {
                    Prefs.addTo(this, Prefs.blockPackages(this), value, Prefs::setBlockPackages)
                }
                reload()
            }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }

    private fun typeLabel(type: Type): Int = when (type) {
        Type.KEYWORD -> R.string.type_keyword
        Type.OVERRIDE -> R.string.type_override
        Type.CHANNEL -> R.string.type_channel
        Type.APP -> R.string.type_app
    }

    private inner class Adapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Item = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@ManageBlockActivity)
                .inflate(R.layout.row_block, parent, false)
            val item = items[position]
            view.findViewById<TextView>(R.id.blockType).setText(typeLabel(item.type))
            view.findViewById<TextView>(R.id.blockValue).text = item.value
            view.findViewById<Button>(R.id.btnRemove).setOnClickListener { remove(item) }
            return view
        }
    }
}
