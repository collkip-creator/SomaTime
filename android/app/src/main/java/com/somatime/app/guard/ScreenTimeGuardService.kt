package com.somatime.app.guard

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.somatime.app.MainActivity
import com.somatime.app.R
import com.somatime.app.data.SomaTimeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScreenTimeGuardService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private lateinit var overlayController: LockOverlayController

    override fun onCreate() {
        super.onCreate()
        overlayController = LockOverlayController(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            overlayController.hideAll()
            cancelTakeoverAlertNotification()
            SomaTimeStore.getInstance(this).setGuardServiceRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannels()
        val notification = buildNotification("Monitoring Kenyan CBC screen time limits")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val store = SomaTimeStore.getInstance(this)
        store.setGuardServiceRunning(true)

        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            var lastLaunchAttemptMs = 0L
            var tickCount = 0
            while (isActive) {
                delay(300L)
                tickCount += 1
                if (tickCount % 16 == 0) {
                    runCatching { store.syncPairedChildWithCloud() }
                }

                val usagePkg = UsageAccessHelper.getForegroundPackageName(this@ScreenTimeGuardService)
                val a11yPkg = SomaGuardAccessibilityService.latestActivePackage
                val isSomaTimeForeground = store.isMainActivityForeground()
                val effectivePkg = when {
                    isSomaTimeForeground -> packageName
                    usagePkg != null -> usagePkg
                    else -> a11yPkg
                }
                if (effectivePkg != null) {
                    store.updateForegroundApp(effectivePkg)
                }

                val uiState = store.state.value
                val isTakeoverEnforced = uiState.isTakeoverLocked && !store.isGuardSuppressed()

                if (!isTakeoverEnforced) {
                    lastLaunchAttemptMs = 0L
                    overlayController.hideAll()
                    cancelTakeoverAlertNotification()
                    continue
                }

                // Keep top status-bar swipe-down blocker active while device is in locked takeover
                overlayController.showTopSwipeBlocker()

                // Cover & interrupt whenever SomaTime is NOT on screen or another app opened
                val otherAppVisible = !isSomaTimeForeground ||
                    (usagePkg != null && usagePkg != packageName)

                if (otherAppVisible) {
                    overlayController.showFullLockOverlay()

                    val now = System.currentTimeMillis()
                    if (now - lastLaunchAttemptMs >= 900L) {
                        lastLaunchAttemptMs = now
                        val intercepted = when {
                            UsageAccessHelper.isTrackableInterruptedApp(this@ScreenTimeGuardService, effectivePkg) -> effectivePkg
                            UsageAccessHelper.isTrackableInterruptedApp(this@ScreenTimeGuardService, a11yPkg) -> a11yPkg
                            else -> uiState.lastInterruptedAppPackage ?: "screen_timer_expired"
                        }
                        launchLockScreen(intercepted ?: "screen_timer_expired")
                    }
                } else {
                    overlayController.hideFullLockOverlay()
                    cancelTakeoverAlertNotification()
                }
            }
        }

        return START_STICKY
    }

    private fun launchLockScreen(interceptedApp: String) {
        val lockIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            putExtra(EXTRA_INTERCEPTED_APP, interceptedApp)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val options = ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            val pi = PendingIntent.getActivity(
                this,
                TAKEOVER_NOTIFICATION_ID,
                lockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                options.toBundle()
            )
            val sent = runCatching { pi.send() }.isSuccess
            if (!sent) {
                runCatching { startActivity(lockIntent, options.toBundle()) }
            }
            showTakeoverAlertNotification(pi)
        } else {
            runCatching { startActivity(lockIntent) }
            val pi = PendingIntent.getActivity(
                this,
                TAKEOVER_NOTIFICATION_ID,
                lockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            showTakeoverAlertNotification(pi)
        }
    }

    private fun showTakeoverAlertNotification(fullScreenPendingIntent: PendingIntent) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alert = NotificationCompat.Builder(this, TAKEOVER_CHANNEL_ID)
            .setContentTitle("SomaTime Screen Time Expired")
            .setContentText("Complete a Kenyan CBC activity to unlock your apps.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(TAKEOVER_NOTIFICATION_ID, alert) }
    }

    private fun cancelTakeoverAlertNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.cancel(TAKEOVER_NOTIFICATION_ID) }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        if (::overlayController.isInitialized) {
            overlayController.hideAll()
        }
        cancelTakeoverAlertNotification()
        SomaTimeStore.getInstance(this).setGuardServiceRunning(false)
        super.onDestroy()
    }

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val guardChannel = NotificationChannel(
            CHANNEL_ID,
            "SomaTime Screen Guard",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors active screen time and enforces Kenyan CBC learning locks."
        }
        val takeoverChannel = NotificationChannel(
            TAKEOVER_CHANNEL_ID,
            "SomaTime Screen Lock Interruption",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Interrupts entertainment apps when watch minutes expire."
        }
        manager.createNotificationChannel(guardChannel)
        manager.createNotificationChannel(takeoverChannel)
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SomaTime Guard Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "somatime_guard_channel"
        const val TAKEOVER_CHANNEL_ID = "somatime_takeover_alert_channel"
        const val NOTIFICATION_ID = 1001
        const val TAKEOVER_NOTIFICATION_ID = 1002
        const val ACTION_STOP = "com.somatime.app.STOP_GUARD"
        const val EXTRA_INTERCEPTED_APP = "extra_intercepted_app"

        fun start(context: Context) {
            val intent = Intent(context, ScreenTimeGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenTimeGuardService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
