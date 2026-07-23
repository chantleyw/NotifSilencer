package com.notifsilencer.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.app.Activity
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private const val DONATION_URL = "https://ko-fi.com/moersebene"
        private const val REQ_EXPORT = 101
        private const val REQ_IMPORT = 102
        private const val EXPORT_FILENAME = "notifsilencer-settings.json"
    }

    private lateinit var statusLine: TextView
    private lateinit var modeHint: TextView
    private lateinit var switchLogOnly: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusLine = findViewById(R.id.statusLine)
        modeHint = findViewById(R.id.modeHint)
        switchLogOnly = findViewById(R.id.switchLogOnly)

        findViewById<Button>(R.id.btnAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnBattery).setOnClickListener { requestBatteryExemption() }

        findViewById<Button>(R.id.btnBlockHistory).setOnClickListener {
            startActivity(Intent(this, BlockLogActivity::class.java))
        }

        findViewById<Button>(R.id.btnManageBlock).setOnClickListener {
            startActivity(Intent(this, ManageBlockActivity::class.java))
        }

        findViewById<Button>(R.id.btnIgnoreApps).setOnClickListener {
            startActivity(Intent(this, IgnoreAppsActivity::class.java))
        }

        findViewById<Button>(R.id.btnKeywords).setOnClickListener {
            startActivity(Intent(this, KeywordEditActivity::class.java))
        }

        findViewById<Button>(R.id.btnSupport).setOnClickListener { openDonationPage() }

        switchLogOnly.isChecked = Prefs.isLogOnly(this)
        switchLogOnly.setOnCheckedChangeListener { _, checked ->
            Prefs.setLogOnly(this, checked)
            renderMode()
        }

        requestPostNotificationsIfNeeded()
        // Kick the keep-alive service so the listener process stays resident.
        KeepAliveService.start(this)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_log -> {
                startActivity(Intent(this, LogActivity::class.java)); true
            }
            R.id.menu_export -> {
                exportSettings(); true
            }
            R.id.menu_import -> {
                importSettings(); true
            }
            R.id.menu_exit -> {
                confirmKill(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmKill() {
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.menu_exit)
            .setMessage(R.string.exit_confirm)
            .setPositiveButton(R.string.exit_now) { _, _ -> killApp() }
            .setNegativeButton(R.string.action_close, null)
            .show()
    }

    private fun killApp() {
        // Drop the keep-alive foreground service (removes its notification), close
        // the UI, and kill the process. Note: Android may re-bind the notification
        // listener afterwards — to stop filtering for good, revoke notification access.
        stopService(Intent(this, KeepAliveService::class.java))
        finishAndRemoveTask()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun openDonationPage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.support_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportSettings() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, EXPORT_FILENAME)
        }
        try {
            startActivityForResult(intent, REQ_EXPORT)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.settings_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importSettings() {
        // Accept any type: some file managers don't tag .json as application/json.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            startActivityForResult(intent, REQ_IMPORT)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.settings_error, Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            REQ_EXPORT -> {
                try {
                    contentResolver.openOutputStream(uri)?.use {
                        it.write(Prefs.exportJson(this).toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(this, R.string.settings_exported, Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.settings_error, Toast.LENGTH_SHORT).show()
                }
            }
            REQ_IMPORT -> {
                try {
                    val text = contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: return
                    Prefs.importJson(this, text)
                    // Reflect any imported changes in the UI immediately.
                    switchLogOnly.isChecked = Prefs.isLogOnly(this)
                    renderMode()
                    Toast.makeText(this, R.string.settings_imported, Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.settings_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        renderStatus()
        renderMode()
    }

    private fun renderStatus() {
        val granted = isAccessGranted()
        statusLine.text = if (granted) {
            getString(R.string.status_granted)
        } else {
            getString(R.string.status_denied)
        }
    }

    private fun renderMode() {
        modeHint.text = if (switchLogOnly.isChecked) {
            getString(R.string.mode_log_only)
        } else {
            getString(R.string.mode_enforce)
        }
    }

    /** Reads enabled_notification_listeners and looks for our component. */
    private fun isAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val me = packageName
        return flat.split(":").any { it.contains(me) }
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, R.string.battery_already, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (_: Exception) {
            // Fall back to the general battery-optimisation list on OEMs that block the direct prompt.
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}
