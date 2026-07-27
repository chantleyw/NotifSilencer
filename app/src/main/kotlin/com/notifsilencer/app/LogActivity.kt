package com.notifsilencer.app

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView

/**
 * The intercepted log as a tappable, searchable list. Tapping a row opens
 * [BlockActions]. The search box filters by message text, package, or channel —
 * useful for finding a specific notification to block by content.
 */
class LogActivity : Activity() {

    private lateinit var list: ListView
    private lateinit var empty: TextView
    private lateinit var search: EditText
    private lateinit var verdictFilter: Spinner
    private lateinit var adapter: LogEntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loglist)
        findViewById<TextView>(R.id.listHeader).setText(R.string.intercepted_log)
        list = findViewById(R.id.logList)
        empty = findViewById(R.id.emptyView)
        search = findViewById(R.id.searchBox)
        verdictFilter = findViewById(R.id.verdictFilter)

        verdictFilter.adapter = ArrayAdapter.createFromResource(
            this, R.array.verdict_filters, android.R.layout.simple_spinner_item
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        verdictFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = render()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        adapter = LogEntryAdapter(this, emptyList())
        list.adapter = adapter
        list.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            BlockActions.show(this, adapter.getItem(pos)) { render() }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = render()
        })

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
        var entries = LogRender.filter(LogStore.all(this), search.text.toString())
        entries = when (verdictFilter.selectedItemPosition) {
            1 -> entries.filter { !it.killed && !it.reason.startsWith("WOULD-KILL") } // Kept
            2 -> entries.filter { it.killed }                                          // Killed
            3 -> entries.filter { it.reason.startsWith("WOULD-KILL") }                 // Would-kill
            else -> entries                                                            // All
        }
        adapter.setEntries(entries)
        empty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }
}
