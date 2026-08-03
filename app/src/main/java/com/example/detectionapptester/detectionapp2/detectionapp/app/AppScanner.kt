package com.example.detectionapptester.detectionapp2.detectionapp.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class AppScanner(private val context :Context) {
    fun scan(): List<AppScanResult> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val checker = SimilarityChecker()
        val networkMonitor = NetworkMonitor(context)
        val results = mutableListOf<AppScanResult>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue//Skip known apps

            if (pkg.packageName in WhiteList.packageNames) continue//Skip system apps

            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue

            val appName = pm.getApplicationLabel(appInfo).toString()
            val permissions = pkg.requestedPermissions?.toList() ?: emptyList()
            val impersonation = checker.check(pkg.packageName, appName)

            val networkStats = networkMonitor.getNetworkStats(appInfo.uid)

            results.add(
                AppScanResult(
                    packageName = pkg.packageName,
                    appName = appName,
                    permissions = permissions,
                    isImpersonator = impersonation.isImpersonator,
                    impersonating = impersonation.impersonating,
                    networkStats = networkStats
                )
            )
        }
        return results
    }
}