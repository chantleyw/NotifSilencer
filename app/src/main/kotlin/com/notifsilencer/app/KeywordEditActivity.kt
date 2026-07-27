package com.notifsilencer.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

/**
 * One entry per line. ALLOW is checked before BLOCK, so anything you put in
 * ALLOW is guaranteed to pass through untouched.
 */
class KeywordEditActivity : Activity() {

    private lateinit var allowEdit: EditText
    private lateinit var blockEdit: EditText
    private lateinit var forceEdit: EditText
    private lateinit var channelsEdit: EditText
    private lateinit var ignoreEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keywords)

        allowEdit = findViewById(R.id.allowEdit)
        blockEdit = findViewById(R.id.blockEdit)
        forceEdit = findViewById(R.id.forceEdit)
        channelsEdit = findViewById(R.id.channelsEdit)
        ignoreEdit = findViewById(R.id.ignoreEdit)

        load(
            Prefs.allowList(this),
            Prefs.blockList(this),
            Prefs.forceBlockList(this),
            Prefs.blockChannels(this),
            Prefs.ignorePackages(this)
        )

        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }

        findViewById<Button>(R.id.btnReset).setOnClickListener {
            load(
                Prefs.DEFAULT_ALLOW,
                Prefs.DEFAULT_BLOCK,
                Prefs.DEFAULT_FORCE_BLOCK,
                Prefs.DEFAULT_CHANNELS,
                Prefs.DEFAULT_IGNORE_PACKAGES
            )
            Toast.makeText(this, R.string.reset_loaded, Toast.LENGTH_SHORT).show()
        }
    }

    private fun load(
        allow: List<String>,
        block: List<String>,
        force: List<String>,
        channels: List<String>,
        ignore: List<String>
    ) {
        allowEdit.setText(allow.joinToString("\n"))
        blockEdit.setText(block.joinToString("\n"))
        forceEdit.setText(force.joinToString("\n"))
        channelsEdit.setText(channels.joinToString("\n"))
        ignoreEdit.setText(ignore.joinToString("\n"))
    }

    private fun save() {
        Prefs.setAllowList(this, toLines(allowEdit))
        Prefs.setBlockList(this, toLines(blockEdit))
        Prefs.setForceBlockList(this, toLines(forceEdit))
        Prefs.setBlockChannels(this, toLines(channelsEdit))
        Prefs.setIgnorePackages(this, toLines(ignoreEdit))
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun toLines(edit: EditText): List<String> =
        edit.text.toString().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
}
