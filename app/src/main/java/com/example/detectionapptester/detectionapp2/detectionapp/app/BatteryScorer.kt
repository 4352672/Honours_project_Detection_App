package com.example.detectionapptester.detectionapp2.detectionapp.app

class BatteryScorer {
    companion object {
        private const val HIGH_BATTERY_SIGNAL = 15//Battery usage signal weights(%)
        private const val MEDIUM_BATTERY_SIGNAL = 8
        private const val LOW_BATTERY_SIGNAL = 0

    }

    fun score(result: AppScanResult): Int {
        val hasBootPermission = result.permissions.map { it.removePrefix("android.permission.") }
            .any { it == "RECEIVE_BOOT_COMPLETED" }
        val hasBackgroundActivity = result.networkStats?.hasBackgroundActivity == true
        return when {
            hasBootPermission && hasBackgroundActivity -> HIGH_BATTERY_SIGNAL//High battery usage signal:persists across reboots and active in background
            !hasBootPermission && hasBackgroundActivity -> MEDIUM_BATTERY_SIGNAL//Medium battery usage signal:active in background and does not persists boot
            else -> LOW_BATTERY_SIGNAL//Low battery usage signal-no background activity signal
        }
    }
}