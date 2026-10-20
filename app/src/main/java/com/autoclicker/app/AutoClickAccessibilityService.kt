package com.autoclicker.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class AutoClickAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var x = 0f
    private var y = 0f
    private var interval = 100L
    private var remaining = 0
    private var generation = 0

    override fun onServiceConnected() {
        instance = this
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { stopClicks() }

    override fun onDestroy() {
        stopClicks()
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun scheduleNext(token: Int, waitMs: Long) {
        handler.postDelayed({
            if (!running || token != generation) return@postDelayed
            if (remaining <= 0) {
                running = false
                return@postDelayed
            }
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                .build()
            val accepted = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (!running || token != generation) return
                    remaining--
                    if (remaining <= 0) {
                        running = false
                    } else {
                        scheduleNext(token, interval)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (!running || token != generation) return
                    remaining--
                    if (remaining <= 0) running = false else scheduleNext(token, interval)
                }
            }, null)
            if (!accepted) {
                running = false
            }
        }, waitMs.coerceAtLeast(0L))
    }

    companion object {
        @Volatile private var instance: AutoClickAccessibilityService? = null

        fun startClicks(x: Int, y: Int, intervalMs: Long, count: Int, initialDelayMs: Long) {
            instance?.begin(x, y, intervalMs, count, initialDelayMs)
        }

        fun stopClicks() {
            instance?.stopInternal()
        }
    }

    private fun begin(x: Int, y: Int, intervalMs: Long, count: Int, delayMs: Long) {
        stopInternal()
        this.x = x.toFloat()
        this.y = y.toFloat()
        interval = intervalMs.coerceAtLeast(1L)
        remaining = count
        running = true
        generation++
        scheduleNext(generation, delayMs)
    }

    private fun stopInternal() {
        running = false
        remaining = 0
        generation++
        handler.removeCallbacksAndMessages(null)
    }
}
