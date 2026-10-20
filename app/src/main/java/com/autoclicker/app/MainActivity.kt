package com.autoclicker.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executors
import java.time.LocalTime
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {
    private lateinit var xInput: EditText
    private lateinit var yInput: EditText
    private lateinit var intervalInput: EditText
    private lateinit var countInput: EditText
    private lateinit var delayInput: EditText
    private lateinit var timeInput: EditText
    private lateinit var startMode: RadioGroup
    private lateinit var statusText: TextView
    private lateinit var timeUrlInput: EditText
    private lateinit var timeSyncStatus: TextView
    private var siteTimeOffsetMillis: Long? = null
    private val networkExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        xInput = findViewById(R.id.xInput)
        yInput = findViewById(R.id.yInput)
        intervalInput = findViewById(R.id.intervalInput)
        countInput = findViewById(R.id.countInput)
        delayInput = findViewById(R.id.startDelayInput)
        timeInput = findViewById(R.id.startTimeInput)
        startMode = findViewById(R.id.startModeGroup)
        statusText = findViewById(R.id.statusText)
        timeUrlInput = findViewById(R.id.timeUrlInput)
        timeSyncStatus = findViewById(R.id.timeSyncStatus)

        findViewById<Button>(R.id.syncTimeButton).setOnClickListener { syncTimeFromSite() }
        findViewById<Button>(R.id.accessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.overlayButton).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            startService(Intent(this, CoordinatePickerService::class.java))
            statusText.text = "نشانگر شناور را جابه‌جا کن و برای ثبت نقطه روی آن ضربه بزن."
        }
        findViewById<Button>(R.id.startButton).setOnClickListener { startClicks() }
        findViewById<Button>(R.id.stopButton).setOnClickListener {
            AutoClickAccessibilityService.stopClicks()
            statusText.text = "متوقف شد"
        }
    }

    override fun onResume() {
        super.onResume()
        if (::xInput.isInitialized && ::yInput.isInitialized) {
            val prefs = getSharedPreferences("autoclicker", MODE_PRIVATE)
            if (prefs.contains("x") && prefs.contains("y")) {
                xInput.setText(prefs.getInt("x", 0).toString())
                yInput.setText(prefs.getInt("y", 0).toString())
            }
        }
    }

    private fun startClicks() {
        val serviceEnabled = isAccessibilityServiceEnabled()
        if (!serviceEnabled) {
            Toast.makeText(this, "ابتدا سرویس Accessibility را فعال کن.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        val x = xInput.text.toString().toIntOrNull()
        val y = yInput.text.toString().toIntOrNull()
        val interval = intervalInput.text.toString().toLongOrNull()
        val count = countInput.text.toString().toIntOrNull()
        if (x == null || y == null || x < 0 || y < 0 ||
            interval == null || interval < 1 || count == null || count < 1) {
            Toast.makeText(this, "مختصات، فاصله و تعداد را درست وارد کن.", Toast.LENGTH_LONG).show()
            return
        }
        val delayMillis = when (startMode.checkedRadioButtonId) {
            R.id.startAfter -> {
                val seconds = delayInput.text.toString().toLongOrNull()
                if (seconds == null || seconds < 0) {
                    Toast.makeText(this, "تأخیر نامعتبر است.", Toast.LENGTH_SHORT).show()
                    return
                }
                seconds * 1000L
            }
            R.id.startAtTime -> {
                try {
                    val target = LocalTime.parse(timeInput.text.toString(),
                        DateTimeFormatter.ofPattern("HH:mm"))
                    val nowInstant = Instant.ofEpochMilli(System.currentTimeMillis() + (siteTimeOffsetMillis ?: 0L))
                    val now = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault())
                    var targetDateTime = now.withHour(target.hour).withMinute(target.minute)
                        .withSecond(0).withNano(0)
                    if (!targetDateTime.isAfter(now)) targetDateTime = targetDateTime.plusDays(1)
                    Duration.between(now, targetDateTime).toMillis()
                } catch (_: Exception) {
                    Toast.makeText(this, "ساعت را به شکل HH:mm وارد کن.", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            else -> 0L
        }
        AutoClickAccessibilityService.startClicks(x, y, interval, count, delayMillis)
        statusText.text = "برنامه‌ریزی شد: $count کلیک، فاصله $interval ms"
    }

    private fun syncTimeFromSite() {
        val raw = timeUrlInput.text.toString().trim()
        if (raw.isBlank()) {
            timeSyncStatus.text = "ابتدا آدرس سایت را وارد کن."
            return
        }
        val normalized = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        timeSyncStatus.text = "در حال بررسی ساعت سایت..."
        networkExecutor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(normalized).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 7000
                    readTimeout = 7000
                    instanceFollowRedirects = true
                    useCaches = false
                }
                connection.connect()
                val dateHeader = connection.getHeaderFieldDate("Date", -1L)
                if (dateHeader <= 0L) throw IllegalStateException("سایت هدر معتبر Date برنگرداند.")
                val offset = dateHeader - System.currentTimeMillis()
                runOnUiThread {
                    siteTimeOffsetMillis = offset
                    val siteNow = Instant.ofEpochMilli(System.currentTimeMillis() + offset)
                        .atZone(ZoneId.systemDefault()).toLocalTime()
                    timeSyncStatus.text = "ساعت سایت دریافت شد: ${siteNow.toString().take(8)}؛ اختلاف تقریبی با گوشی ${offset / 1000} ثانیه."
                    Toast.makeText(this, "ساعت سایت همگام شد.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    timeSyncStatus.text = "همگام‌سازی ناموفق: ${e.message ?: "سایت پاسخ مناسب نداد"}. آدرس را بررسی کن یا از سایتی استفاده کن که هدر Date دارد."
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { it.resolveInfo.serviceInfo.packageName == packageName &&
                it.resolveInfo.serviceInfo.name == AutoClickAccessibilityService::class.java.name }
    }
}
