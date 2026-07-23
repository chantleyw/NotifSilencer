package com.notifsilencer.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.app.Activity
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    companion object {
        private const val DONATION_URL = "https://ko-fi.com/moersebene"
    }

    private lateinit var statusLine: TextView
    private lateinit var modeHint: TextView
    private lateinit var logView: TextView
    private lateinit var switchLogOnly: Switch

    private val timeFmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusLine = findViewById(R.id.statusLine)
        modeHint = findViewById(R.id.modeHint)
        logView = findViewById(R.id.logView)
        switchLogOnly = findViewById(R.id.switchLogOnly)

        findViewById<Button>(R.id.btnAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnBattery).setOnClickListener { requestBatteryExemption() }

        findViewById<Button>(R.id.btnIgnoreApps).setOnClickListener {
            startActivity(Intent(this, IgnoreAppsActivity::class.java))
        }

        findViewById<Button>(R.id.btnKeywords).setOnClickListener {
            startActivity(Intent(this, KeywordEditActivity::class.java))
        }

        findViewById<Button>(R.id.btnSupport).setOnClickListener { openDonationPage() }

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { renderLog() }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            LogStore.clear(this)
            renderLog()
        }

        switchLogOnly.isChecked = Prefs.isLogOnly(this)
        switchLogOnly.setOnCheckedChangeListener { _, checked ->
            Prefs.setLogOnly(this, checked)
            renderMode()
        }

        requestPostNotificationsIfNeeded()
        // Kick the keep-alive service so the listener process stays resident.
        KeepAliveService.start(this)
    }

    private fun openDonationPage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.support_unavailable, Toast.LENGTH_SHORT).show()
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
        renderLog()
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

    private fun renderLog() {
        val entries = LogStore.all(this)
        if (entries.isEmpty()) {
            logView.text = getString(R.string.log_empty)
            return
        }
        val green = 0xFF2E7D32.toInt()
        val red = 0xFFC62828.toInt()
        val sb = SpannableStringBuilder()
        for (e in entries) {
            val verdict = if (e.killed) "KILLED" else "KEPT"
            sb.append(timeFmt.format(Date(e.time))).append("  ")
            // Colour + bold only the verdict token.
            val start = sb.length
            sb.append(verdict)
            val colour = if (e.killed) red else green
            sb.setSpan(ForegroundColorSpan(colour), start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("  ").append(e.reason).append('\n')
                .append("  pkg=").append(e.pkg).append('\n')
                .append("  ch=").append(if (e.channel.isEmpty()) "(none)" else e.channel).append('\n')
                .append("  ").append(e.text.ifEmpty { "(no text)" }).append("\n\n")
        }
        logView.text = sb
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
