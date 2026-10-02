package com.focusblock.app

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val maxAllowed = 3
    private val checkboxes = mutableListOf<CheckBox>()

    private lateinit var startText: TextView
    private lateinit var endText: TextView
    private lateinit var scheduleSwitch: Switch
    private lateinit var limitInput: EditText
    private lateinit var statusText: TextView

    private var startMin = 9 * 60
    private var endMin = 21 * 60

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) LocalVpnService.start(this)
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startMin = Prefs.getStartMinute(this)
        endMin = Prefs.getEndMinute(this)

        buildAppList(findViewById(R.id.appsContainer))

        scheduleSwitch = findViewById(R.id.scheduleSwitch)
        scheduleSwitch.isChecked = Prefs.isScheduleOn(this)

        startText = findViewById(R.id.startTimeText)
        endText = findViewById(R.id.endTimeText)
        updateTimeLabels()
        startText.setOnClickListener { pickTime(startMin) { m -> startMin = m; updateTimeLabels() } }
        endText.setOnClickListener { pickTime(endMin) { m -> endMin = m; updateTimeLabels() } }

        limitInput = findViewById(R.id.limitInput)
        limitInput.setText(Prefs.getDailyLimitMinutes(this).toString())

        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.accessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.batteryButton).setOnClickListener { requestBatteryExemption() }
        findViewById<Button>(R.id.vpnButton).setOnClickListener { requestVpn() }

        findViewById<Switch>(R.id.blockingSwitch).apply {
            isChecked = Prefs.isBlockingOn(this@MainActivity)
            setOnCheckedChangeListener { _, isOn ->
                Prefs.setBlockingOn(this@MainActivity, isOn)
                refreshStatus()
            }
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener { save() }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    /** Lists the device's launchable apps (except this one) as checkboxes, capped at [maxAllowed]. */
    private fun buildAppList(container: LinearLayout) {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcherIntent, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val allowed = Prefs.getAllowed(this)
        for (ri in apps) {
            val pkg = ri.activityInfo.packageName
            val box = CheckBox(this).apply {
                text = ri.loadLabel(pm)
                tag = pkg
                isChecked = pkg in allowed
                setOnCheckedChangeListener { _, checked ->
                    if (checked && checkboxes.count { it.isChecked } > maxAllowed) {
                        isChecked = false
                        Toast.makeText(this@MainActivity, "Choose at most $maxAllowed apps", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            checkboxes.add(box)
            container.addView(box)
        }
    }

    private fun pickTime(initialMinutes: Int, onPicked: (Int) -> Unit) {
        val h = initialMinutes / 60
        val m = initialMinutes % 60
        TimePickerDialog(this, { _, hh, mm -> onPicked(hh * 60 + mm) }, h, m, true).show()
    }

    private fun updateTimeLabels() {
        startText.text = "From %02d:%02d".format(startMin / 60, startMin % 60)
        endText.text = "To %02d:%02d".format(endMin / 60, endMin % 60)
    }

    private fun requestBatteryExemption() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun requestVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) vpnLauncher.launch(intent) else { LocalVpnService.start(this); refreshStatus() }
    }

    private fun save() {
        val chosen = checkboxes.filter { it.isChecked }.map { it.tag as String }.toSet()
        if (chosen.isEmpty()) {
            Toast.makeText(this, "Choose 1 to $maxAllowed apps to allow", Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.setAllowed(this, chosen)
        Prefs.setScheduleOn(this, scheduleSwitch.isChecked)
        Prefs.setWindow(this, startMin, endMin)
        val limit = limitInput.text.toString().toIntOrNull() ?: 120
        Prefs.setDailyLimitMinutes(this, limit.coerceIn(1, 1440))
        LocalVpnService.start(this)   // re-applies the updated allow-list to the VPN's exclude rules
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun isAccessibilityOn(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabled.contains("$packageName/${BlockerAccessibilityService::class.java.name}")
    }

    private fun refreshStatus() {
        val used = Prefs.getUsedSecondsToday(this) / 60
        val limit = Prefs.getDailyLimitMinutes(this)
        val acc = if (isAccessibilityOn()) "on" else "OFF \u2013 tap \u201cTurn on the blocking service\u201d above"
        val blocking = if (Prefs.isBlockingOn(this)) "ON" else "off"
        statusText.text = "Blocking service: $acc\nBlocking switch: $blocking\nUsed today: ${used} of ${limit} min"
    }
}
