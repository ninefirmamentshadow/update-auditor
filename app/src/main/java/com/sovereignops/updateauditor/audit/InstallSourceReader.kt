package com.sovereignops.updateauditor.audit

import android.content.pm.InstallSourceInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.sovereignops.updateauditor.model.InstallSource
import com.sovereignops.updateauditor.model.PackageSource

/**
 * Reads the install-source chain Android exposes to an ordinary app. Fields the
 * platform withholds or the API level lacks come back null, not guessed.
 */
internal class InstallSourceReader(private val packageManager: PackageManager) {

    fun read(packageName: String): InstallSource = runCatching {
        if (Build.VERSION.SDK_INT >= 30) {
            fromInfo(packageManager.getInstallSourceInfo(packageName))
        } else {
            @Suppress("DEPRECATION")
            InstallSource(
                installingPackage = packageManager.getInstallerPackageName(packageName),
                initiatingPackage = null,
                originatingPackage = null,
                updateOwnerPackage = null,
                packageSource = PackageSource.UNAVAILABLE,
            )
        }
    }.getOrDefault(InstallSource.UNKNOWN)

    @RequiresApi(30)
    private fun fromInfo(info: InstallSourceInfo): InstallSource = InstallSource(
        installingPackage = info.installingPackageName,
        initiatingPackage = info.initiatingPackageName,
        originatingPackage = info.originatingPackageName,
        updateOwnerPackage = if (Build.VERSION.SDK_INT >= 34) info.updateOwnerPackageName else null,
        packageSource = if (Build.VERSION.SDK_INT >= 33) sourceOf(info.packageSource) else PackageSource.UNAVAILABLE,
    )

    @RequiresApi(33)
    private fun sourceOf(value: Int): PackageSource = when (value) {
        PackageInstaller.PACKAGE_SOURCE_OTHER -> PackageSource.OTHER
        PackageInstaller.PACKAGE_SOURCE_STORE -> PackageSource.STORE
        PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE -> PackageSource.LOCAL_FILE
        PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE -> PackageSource.DOWNLOADED_FILE
        else -> PackageSource.UNSPECIFIED
    }
}
