package com.sovereignops.updateauditor.audit

import com.sovereignops.updateauditor.model.InstallSource
import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.model.PackageSource
import com.sovereignops.updateauditor.model.PackageState

/** The list filters. Pure Kotlin so the rules are unit-tested. */
enum class PackageFilter {
    ALL,
    FIRST_PARTY,
    SYSTEM,
    UPDATED_SYSTEM,
    USER,
    UNVERIFIED,
    SIDELOADED;

    fun matches(state: PackageState): Boolean = when (this) {
        ALL -> true
        FIRST_PARTY -> state.origin == PackageOrigin.FIRST_PARTY
        SYSTEM -> state.origin == PackageOrigin.SYSTEM
        UPDATED_SYSTEM -> state.updatedSystemApp
        USER -> state.origin == PackageOrigin.USER
        UNVERIFIED -> state.origin == PackageOrigin.UNVERIFIED
        SIDELOADED -> isSideloaded(state)
    }

    companion object {
        /** System package-installer screens: an install through one of these has no store behind it. */
        private val PACKAGE_INSTALLER_UIS = setOf(
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller",
        )

        /**
         * "Sideloaded" means installed outside any recorded installer app: not a system or
         * Mainline package, and either no installer is recorded, the recorded installer is a
         * system package-installer screen, or Android labels the source a local or downloaded
         * file. It is a provenance observation, not a safety verdict.
         */
        fun isSideloaded(state: PackageState): Boolean {
            if (state.systemApp || state.updatedSystemApp || state.apex) return false
            val source: InstallSource = state.installSource
            val installer = source.installingPackage
            return installer == null ||
                installer in PACKAGE_INSTALLER_UIS ||
                source.packageSource == PackageSource.LOCAL_FILE ||
                source.packageSource == PackageSource.DOWNLOADED_FILE
        }

        /** Applies [filter], then a case-insensitive substring [query] on label, package name and installer. */
        fun apply(packages: List<PackageState>, filter: PackageFilter, query: String): List<PackageState> {
            val needle = query.trim().lowercase()
            return packages.filter { state ->
                filter.matches(state) && (
                    needle.isEmpty() ||
                        state.label.lowercase().contains(needle) ||
                        state.packageName.lowercase().contains(needle) ||
                        (state.installerPackageName?.lowercase()?.contains(needle) == true)
                    )
            }
        }
    }
}
