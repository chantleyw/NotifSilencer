package com.notifsilencer.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView

/**
 * The intercepted log as a tappable list. Tapping a row opens [BlockActions]
 * so a notification can be added to the block/ignore lists without typing.
 */
class LogActivity : Activity() {

    private lateinit var list: ListView
    private lateinit var empty: TextView
    private lateinit var adapter: LogEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loglist)
        findViewById<TextView>(R.id.listHeader).setText(R.string.intercepted_log)
        list = findViewById(R.id.logList)
        empty = findViewById(R.id.emptyView)

        adapter = LogEntryAdapter(this, LogStore.all(this))
        list.adapter = adapter
        list.onItemClickListener = android.widget.AdapterView.OnItemClickListener { _, _, pos, _ ->
            BlockActions.show(this, adapter.getItem(pos)) { render() }
        }

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { render() }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            LogStore.clear(this)
            render()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val entries = LogStore.all(this)
        adapter.setEntries(entries)
        empty.visibility = if (entries.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}
