package com.sovereignops.updateauditor.audit

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.sovereignops.updateauditor.model.PackageState

class PackageAuditor(context: Context) {
    private val packageManager = context.packageManager

    fun read(): List<PackageState> {
        val packages = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.MATCH_DISABLED_COMPONENTS)
        }

        return packages.mapNotNull { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            val systemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            val packageName = packageInfo.packageName

            PackageState(
                label = runCatching { appInfo.loadLabel(packageManager).toString() }
                    .getOrDefault(packageName),
                packageName = packageName,
                versionName = packageInfo.versionName ?: "Unavailable",
                versionCode = if (Build.VERSION.SDK_INT >= 28) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                firstInstallTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime,
                enabled = appInfo.enabled,
                debuggable = appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
                systemApp = systemApp,
                installerPackageName = installerFor(packageName),
                origin = PackageClassifier.classify(packageName, systemApp),
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    private fun installerFor(packageName: String): String? = runCatching {
        if (Build.VERSION.SDK_INT >= 30) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    }.getOrNull()
}
