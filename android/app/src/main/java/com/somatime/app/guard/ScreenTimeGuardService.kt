package com.somatime.app.guard

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
            SomaTimeStore.getInstance(this).setGuardServiceRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
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
            var takeoverTriggered = false
            var tickCount = 0
            while (isActive) {
                delay(350L)
                tickCount += 1
                if (tickCount % 14 == 0) {
                    runCatching { store.syncPairedChildWithCloud() }
                }

                val uiState = store.state.value
                val child = uiState.selectedChild
                val foregroundPkg = UsageAccessHelper.getForegroundPackageName(this@ScreenTimeGuardService)
                if (foregroundPkg != null) {
                    store.updateForegroundApp(foregroundPkg)
                }

                val isLocked = child.isLocked || child.availableScreenSeconds <= 0
                val isTakeoverEnforced = isLocked && (uiState.handOffActive || child.isPaired)

                if (!isTakeoverEnforced) {
                    takeoverTriggered = false
                    overlayController.hideAll()
                    continue
                }

                // Keep top status-bar swipe-down blocker active while device is in locked takeover
                overlayController.showTopSwipeBlocker()

                val openingOtherApp = foregroundPkg != null && foregroundPkg != packageName
                if (openingOtherApp) {
                    overlayController.showFullLockOverlay()
                } else {
                    overlayController.hideFullLockOverlay()
                }

                val shouldTakeOverNow = !takeoverTriggered || openingOtherApp
                if (shouldTakeOverNow) {
                    takeoverTriggered = true
                    val lockIntent = Intent(this@ScreenTimeGuardService, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        )
                        putExtra(EXTRA_INTERCEPTED_APP, foregroundPkg ?: "screen_timer_expired")
                    }
                    runCatching { startActivity(lockIntent) }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        if (::overlayController.isInitialized) {
            overlayController.hideAll()
        }
        SomaTimeStore.getInstance(this).setGuardServiceRunning(false)
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SomaTime Screen Guard",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors active screen time and enforces Kenyan CBC learning locks."
        }
        manager.createNotificationChannel(channel)
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
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.somatime.app.STOP_GUARD"
        const val EXTRA_INTERCEPTED_APP = "extra_intercepted_app"

        fun start(context: Context) {
            val intent = Intent(context, ScreenTimeGuardService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenTimeGuardService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
