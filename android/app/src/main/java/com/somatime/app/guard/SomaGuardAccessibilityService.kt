package com.somatime.app.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.somatime.app.MainActivity
import com.somatime.app.data.SomaTimeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SomaGuardAccessibilityService : AccessibilityService() {

    private var lastRelaunchMs: Long = 0L
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lockWatcherJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val store = SomaTimeStore.getInstance(this)
        lockWatcherJob?.cancel()
        lockWatcherJob = serviceScope.launch {
            var wasTakeoverActive = false
            while (isActive) {
                val uiState = store.state.value
                val isTakeoverActive = uiState.isTakeoverLocked && !store.isGuardSuppressed()
                if (isTakeoverActive && !store.isMainActivityForeground()) {
                    val activePkg = runCatching { rootInActiveWindow?.packageName?.toString() }.getOrNull()
                        ?.takeIf { it != packageName && it != "com.android.systemui" }
                        ?: latestActivePackage
                        ?: uiState.lastInterruptedAppPackage
                        ?: "screen_timer_expired"
                    if (!wasTakeoverActive || System.currentTimeMillis() - lastRelaunchMs >= 1000L) {
                        bringSomaTimeToFront(activePkg)
                    }
                }
                wasTakeoverActive = isTakeoverActive
                delay(300L)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        val store = SomaTimeStore.getInstance(this)

        if (pkg != packageName && pkg != "com.android.systemui") {
            latestActivePackage = pkg
            store.updateForegroundApp(pkg)
        }

        val uiState = store.state.value
        val isTakeoverActive = uiState.isTakeoverLocked && !store.isGuardSuppressed()
        if (!isTakeoverActive) return
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

        // Intercept Home launcher, Recents, Settings, YouTube, TikTok (both window transitions & live video content updates)
        val shouldIntercept = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && !store.isMainActivityForeground())
        if (shouldIntercept) {
            bringSomaTimeToFront(pkg)
        }
    }

    private fun bringSomaTimeToFront(interceptedPkg: String) {
        val now = System.currentTimeMillis()
        if (now - lastRelaunchMs < 450L) return
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

    override fun onDestroy() {
        lockWatcherJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        var latestActivePackage: String? = null
    }
}
