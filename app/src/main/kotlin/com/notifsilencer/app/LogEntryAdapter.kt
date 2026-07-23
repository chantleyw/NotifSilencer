package com.notifsilencer.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

/** Renders log entries as tappable rows (coloured verdict via [LogRender]). */
class LogEntryAdapter(
    private val ctx: Context,
    private var entries: List<LogStore.Entry>
) : BaseAdapter() {

    fun setEntries(list: List<LogStore.Entry>) {
        entries = list
        notifyDataSetChanged()
    }

    override fun getCount(): Int = entries.size
    override fun getItem(position: Int): LogStore.Entry = entries[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(ctx).inflate(R.layout.row_log, parent, false)
        (view as TextView).text = LogRender.formatEntry(entries[position])
        return view
    }
}
