package com.somatime.app.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.somatime.app.MainActivity
import com.somatime.app.data.SomaTimeStore

class SomaGuardAccessibilityService : AccessibilityService() {

    private var lastRelaunchMs: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val store = SomaTimeStore.getInstance(this)
        val uiState = store.state.value
        val isTakeoverActive = uiState.isTakeoverLocked

        if (!isTakeoverActive) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val cls = event.className?.toString()?.lowercase() ?: ""

        // Dismiss notification shade / quick settings swipe-down or system dialogs immediately
        if (pkg == "com.android.systemui") {
            val isShadeOrRecents = cls.contains("notification") ||
                cls.contains("shade") ||
                cls.contains("qs") ||
                cls.contains("quick") ||
                cls.contains("expanded") ||
                cls.contains("panel") ||
                cls.contains("status") ||
                cls.contains("scrim") ||
                cls.contains("recents") ||
                cls.contains("globalactions") ||
                cls.contains("dialog") ||
                cls.contains("volume") ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            if (isShadeOrRecents) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                }
                performGlobalAction(GLOBAL_ACTION_BACK)
                bringSomaTimeToFront(pkg)
            }
            return
        }

        // Intercept Home launcher, Recents, Settings, YouTube, TikTok, or any other app immediately
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            bringSomaTimeToFront(pkg)
        }
    }

    private fun bringSomaTimeToFront(interceptedPkg: String) {
        val now = System.currentTimeMillis()
        if (now - lastRelaunchMs < 250L) return
        lastRelaunchMs = now

        val lockIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            putExtra(ScreenTimeGuardService.EXTRA_INTERCEPTED_APP, interceptedPkg)
        }
        runCatching { startActivity(lockIntent) }
    }

    override fun onInterrupt() {
        // No-op
    }
}
