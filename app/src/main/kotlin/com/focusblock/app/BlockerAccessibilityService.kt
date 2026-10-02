package com.focusblock.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the front. If it is not on the allowed list (or an allowed app's
 * time window or daily limit has run out), the service sends the user home and shows the
 * block screen. This is the piece that must stay "on" in Android's Accessibility settings.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var currentPackage: String? = null
    private var tracking = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!tracking) return
            Prefs.addUsedSeconds(applicationContext, TICK_SECONDS)
            if (Prefs.isOverDailyLimit(applicationContext)) {
                block("Today's time limit is used up.")
            } else {
                handler.postDelayed(this, TICK_SECONDS * 1000L)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == currentPackage) return
        currentPackage = pkg

        if (!Prefs.isBlockingOn(applicationContext) || isExempt(pkg)) {
            stopTracking()
            return
        }

        val allowed = Prefs.getAllowed(applicationContext)
        if (pkg !in allowed) {
            stopTracking()
            block("This app is not on your allowed list.")
            return
        }
        if (Prefs.isOverDailyLimit(applicationContext)) {
            stopTracking()
            block("Today's time limit is used up.")
            return
        }
        if (!Prefs.isWithinWindow(applicationContext)) {
            stopTracking()
            block("This app is only allowed during your set hours.")
            return
        }
        startTracking()
    }

    /** Our own app and the device's home screen always need to stay reachable. */
    private fun isExempt(pkg: String): Boolean {
        if (pkg == applicationContext.packageName) return true
        if (pkg == "com.android.systemui") return true
        val home = applicationContext.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
        )?.activityInfo?.packageName
        return pkg == home
    }

    private fun startTracking() {
        if (tracking) return
        tracking = true
        handler.postDelayed(tickRunnable, TICK_SECONDS * 1000L)
    }

    private fun stopTracking() {
        tracking = false
        handler.removeCallbacks(tickRunnable)
    }

    private fun block(reason: String) {
        performGlobalAction(GLOBAL_ACTION_HOME)
        val i = Intent(this, BlockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(BlockActivity.EXTRA_REASON, reason)
        startActivity(i)
    }

    override fun onInterrupt() {}

    companion object {
        private const val TICK_SECONDS = 15
    }
}
