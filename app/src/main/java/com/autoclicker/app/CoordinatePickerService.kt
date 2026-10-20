package com.autoclicker.app

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class CoordinatePickerService : Service() {
    private lateinit var windowManager: WindowManager
    private var marker: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var initialX = 0
    private var initialY = 0
    private var touchX = 0f
    private var touchY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val label = TextView(this).apply {
            text = "✥"
            textSize = 30f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xCC246BFD.toInt())
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            contentDescription = "Auto Clicker coordinate marker. Drag to move; tap to save."
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 120; y = 300 }
        params = lp
        marker = label
        label.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x; initialY = lp.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = initialX + (event.rawX - touchX).toInt()
                    lp.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(label, lp)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val moved = kotlin.math.abs(event.rawX - touchX) > 8 ||
                        kotlin.math.abs(event.rawY - touchY) > 8
                    if (!moved) {
                        val prefs = getSharedPreferences("autoclicker", MODE_PRIVATE)
                        // Window position is approximate; account for marker center.
                        prefs.edit().putInt("x", lp.x + label.width / 2)
                            .putInt("y", lp.y + label.height / 2).apply()
                        stopSelf()
                    }
                    true
                }
                else -> false
            }
        }
        windowManager.addView(label, lp)
    }

    override fun onDestroy() {
        marker?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        marker = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
