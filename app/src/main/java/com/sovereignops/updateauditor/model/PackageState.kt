package com.sovereignops.updateauditor.model

enum class PackageOrigin {
    FIRST_PARTY,
    SYSTEM,
    USER,
}

data class PackageState(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val enabled: Boolean,
    val debuggable: Boolean,
    val systemApp: Boolean,
    val installerPackageName: String?,
    val origin: PackageOrigin,
)
