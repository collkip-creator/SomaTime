package com.somatime.app.guard

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class SomaDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Parent authorization is required before disabling SomaTime protection."
    }
}
