package com.sovereignops.updateauditor.audit

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.sovereignops.updateauditor.model.PackageState

/**
 * Enumerates installed packages and reads local facts about each. Read-only: it
 * never installs, updates, enables or disables anything.
 */
class PackageAuditor(
    context: Context,
    private val pins: FirstPartyPins = FirstPartyPins.NONE,
) {
    private val packageManager: PackageManager = context.packageManager
    private val signerReader = SignerReader(packageManager)
    private val installSourceReader = InstallSourceReader(packageManager)

    fun read(): List<PackageState> {
        val flags = QueryFlags.forSdk(Build.VERSION.SDK_INT)
        val packages = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(flags.toInt())
        }
        return packages.map(::toState)
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /** One package by name, or null if it is not present for any user visible to this app. */
    fun read(packageName: String): PackageState? {
        val flags = QueryFlags.forSdk(Build.VERSION.SDK_INT)
        val info = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags.toInt())
            }
        }.getOrNull() ?: return null
        return toState(info)
    }

    private fun toState(info: PackageInfo): PackageState {
        val appInfo: ApplicationInfo? = info.applicationInfo
        val appFlags = appInfo?.flags ?: 0
        val apex = Build.VERSION.SDK_INT >= 29 && info.isApex
        val systemApp = apex || appFlags and ApplicationInfo.FLAG_SYSTEM != 0
        val packageName = info.packageName
        val signers = signerReader.read(info)

        return PackageState(
            label = appInfo?.let { runCatching { it.loadLabel(packageManager).toString() }.getOrNull() }
                ?: packageName,
            packageName = packageName,
            versionName = info.versionName ?: "",
            versionCode = if (Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            },
            firstInstallTime = info.firstInstallTime,
            lastUpdateTime = info.lastUpdateTime,
            enabled = appInfo?.enabled ?: false,
            debuggable = appFlags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
            systemApp = systemApp,
            updatedSystemApp = appFlags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
            apex = apex,
            removedForUser = appInfo != null && appFlags and ApplicationInfo.FLAG_INSTALLED == 0,
            minSdk = appInfo?.minSdkVersion ?: 0,
            targetSdk = appInfo?.targetSdkVersion ?: 0,
            compileSdk = if (Build.VERSION.SDK_INT >= 31) {
                appInfo?.compileSdkVersion?.takeIf { it > 0 }
            } else {
                null
            },
            signers = signers,
            installSource = installSourceReader.read(packageName),
            origin = PackageClassifier.classify(
                PackageClassifier.Input(
                    packageName = packageName,
                    isSystemApp = systemApp,
                    platformSigned = signers.platformSigned,
                    signerDigests = signers.currentDigests,
                ),
                pins,
            ),
        )
    }
}
