package com.notifsilencer.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

/**
 * Views the persistent [BlockLog] — every block decision, kept independently of
 * the main log so it survives clearing that. Has its own Clear so you only wipe
 * this history deliberately.
 */
class BlockLogActivity : Activity() {

    private lateinit var view: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocklog)
        view = findViewById(R.id.blockLogView)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { render() }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            BlockLog.clear(this)
            render()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val entries = BlockLog.all(this)
        view.text = if (entries.isEmpty()) getString(R.string.log_empty) else LogRender.format(entries)
    }
}
