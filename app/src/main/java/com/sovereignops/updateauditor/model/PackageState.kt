package com.sovereignops.updateauditor.model

/**
 * Local provenance bucket for an installed package.
 *
 * [UNVERIFIED] means the package name matches a known first-party app but nothing
 * proves it is that app (neither a platform signature nor a pinned signer digest).
 * It is an observation about missing evidence, never a claim about update status.
 */
enum class PackageOrigin {
    FIRST_PARTY,
    SYSTEM,
    USER,
    UNVERIFIED,
}

/** Where Android says the package source was, from `InstallSourceInfo.getPackageSource` (API 33+). */
enum class PackageSource {
    /** The platform predates the API (below Android 13). */
    UNAVAILABLE,
    UNSPECIFIED,
    OTHER,
    STORE,
    LOCAL_FILE,
    DOWNLOADED_FILE,
}

/**
 * The install-source chain Android exposes. Every field is nullable because the
 * platform hides most of it from ordinary apps; null means "not exposed or not
 * recorded", never "none".
 */
data class InstallSource(
    /** The app recorded as the installer (`installingPackageName`). */
    val installingPackage: String?,
    /** The app that asked for the install (`initiatingPackageName`, API 30+). */
    val initiatingPackage: String?,
    /** The app the APK came from (`originatingPackageName`, API 30+; mostly hidden). */
    val originatingPackage: String?,
    /** The app allowed to update this one (`updateOwnerPackageName`, API 34+). */
    val updateOwnerPackage: String?,
    val packageSource: PackageSource,
) {
    companion object {
        val UNKNOWN = InstallSource(null, null, null, null, PackageSource.UNAVAILABLE)
    }
}

/**
 * Signing certificates observed for a package. Digests are lowercase hex SHA-256 of
 * the DER-encoded certificate. Empty [currentDigests] means Android exposed none.
 */
data class SignerInfo(
    /** Digests of the certificates currently signing the package (several if multi-signer). */
    val currentDigests: List<String>,
    /** Digests of earlier certificates in a key-rotation lineage, oldest first; empty if none. */
    val pastDigests: List<String>,
    /** True when the platform reports more than one current signer. */
    val multipleSigners: Boolean,
    /**
     * True when `PackageManager.checkSignatures(pkg, "android")` returned a match, i.e. the
     * package carries the same signing certificates as the platform itself.
     */
    val platformSigned: Boolean,
) {
    val rotated: Boolean get() = pastDigests.isNotEmpty()

    companion object {
        val NONE = SignerInfo(emptyList(), emptyList(), multipleSigners = false, platformSigned = false)
    }
}

data class PackageState(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    /** Epoch milliseconds; 0 means Android reported nothing and must be shown as unknown. */
    val firstInstallTime: Long,
    /** Epoch milliseconds; 0 means Android reported nothing and must be shown as unknown. */
    val lastUpdateTime: Long,
    val enabled: Boolean,
    val debuggable: Boolean,
    val systemApp: Boolean,
    /** A system app that has been updated over the factory image (`FLAG_UPDATED_SYSTEM_APP`). */
    val updatedSystemApp: Boolean,
    /** A Mainline/APEX module rather than an ordinary APK. */
    val apex: Boolean,
    /** Present on the device but removed for this user (`FLAG_INSTALLED` not set). */
    val removedForUser: Boolean,
    val minSdk: Int,
    val targetSdk: Int,
    /** 0 or unknown is null. */
    val compileSdk: Int?,
    val signers: SignerInfo,
    val installSource: InstallSource,
    val origin: PackageOrigin,
) {
    /** The installer-of-record, kept as a convenience for list rows. */
    val installerPackageName: String? get() = installSource.installingPackage
}
