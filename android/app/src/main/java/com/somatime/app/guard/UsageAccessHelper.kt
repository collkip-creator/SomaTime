package com.somatime.app.guard

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

object UsageAccessHelper {

    val restrictedEntertainmentPackages = setOf(
        "com.google.android.youtube",
        "com.zhiliaoapp.musically",
        "com.zhiliaoapp.musically.go",
        "com.ss.android.ugc.trill",
        "com.instagram.android",
        "com.kiloo.subwaysurf",
        "com.roblox.client",
        "com.netflix.mediaclient"
    )

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, SomaGuardAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { entry ->
            val parsed = ComponentName.unflattenFromString(entry.trim())
            parsed == expected || entry.equals(expected.flattenToString(), ignoreCase = true)
        }
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, SomaDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(admin)
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openDeviceAdminSettings(context: Context) {
        val admin = ComponentName(context, SomaDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Protects SomaTime from being uninstalled or force-stopped by learners."
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun getDefaultLauncherPackage(context: Context): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        return runCatching {
            context.packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
        }.getOrNull()
    }

    fun isTrackableInterruptedApp(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName == context.packageName) return false
        if (packageName == "com.android.systemui") return false
        if (packageName == "com.android.settings") return false
        if (packageName == "screen_timer_expired") return false
        if (packageName == getDefaultLauncherPackage(context)) return false
        return true
    }

    fun getAppDisplayLabel(context: Context, packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrNull() ?: packageName
    }

    fun getForegroundPackageName(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 120_000L
        val events = usm.queryEvents(start, end)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTs = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            val isForegroundEvent =
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            if (isForegroundEvent && event.timeStamp >= latestTs) {
                latestTs = event.timeStamp
                latestPackage = event.packageName
            }
        }
        if (!latestPackage.isNullOrBlank()) {
            return latestPackage
        }

        // Fallback when child stays on the same screen (e.g. watching TikTok feed) longer than event lookback
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            end - 3_600_000L,
            end
        )
        return stats
            ?.filter { it.lastTimeUsed > end - 1_800_000L }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }
}
