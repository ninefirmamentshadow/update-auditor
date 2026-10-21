package com.sovereignops.updateauditor.testing

import com.sovereignops.updateauditor.model.DeviceState
import com.sovereignops.updateauditor.model.InstallSource
import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.model.PackageSource
import com.sovereignops.updateauditor.model.PackageState
import com.sovereignops.updateauditor.model.SignerInfo

/** Deterministic synthetic observations. Nothing here is read from a real device. */
object Fixtures {
    const val DIGEST_A = "aa11aa11aa11aa11aa11aa11aa11aa11aa11aa11aa11aa11aa11aa11aa11aa11"
    const val DIGEST_B = "bb22bb22bb22bb22bb22bb22bb22bb22bb22bb22bb22bb22bb22bb22bb22bb22"
    const val DIGEST_C = "cc33cc33cc33cc33cc33cc33cc33cc33cc33cc33cc33cc33cc33cc33cc33cc33"

    const val STORE_INSTALLER = "com.example.store"

    fun device() = DeviceState(
        manufacturer = "ExampleCo",
        model = "EX-100",
        device = "ex100",
        product = "ex100xx",
        androidVersion = "15",
        sdkInt = 35,
        securityPatch = "2026-09-05",
        buildId = "AP4A.250101.001",
        fingerprint = "ExampleCo/ex100xx/ex100:15/AP4A.250101.001/1:user/release-keys",
        baseband = "EX100.001",
    )

    fun signers(vararg digests: String, past: List<String> = emptyList(), platform: Boolean = false) =
        SignerInfo(
            currentDigests = digests.toList(),
            pastDigests = past,
            multipleSigners = digests.size > 1,
            platformSigned = platform,
        )

    fun store(installer: String? = STORE_INSTALLER, source: PackageSource = PackageSource.STORE) =
        InstallSource(
            installingPackage = installer,
            initiatingPackage = installer,
            originatingPackage = null,
            updateOwnerPackage = null,
            packageSource = source,
        )

    fun pkg(
        label: String = "Example App",
        packageName: String = "com.example.app",
        origin: PackageOrigin = PackageOrigin.USER,
        systemApp: Boolean = false,
        updatedSystemApp: Boolean = false,
        apex: Boolean = false,
        removedForUser: Boolean = false,
        enabled: Boolean = true,
        debuggable: Boolean = false,
        versionName: String = "1.2.3",
        versionCode: Long = 123,
        firstInstallTime: Long = 1_700_000_000_000,
        lastUpdateTime: Long = 1_750_000_000_000,
        signers: SignerInfo = signers(DIGEST_A),
        installSource: InstallSource = store(),
        minSdk: Int = 26,
        targetSdk: Int = 35,
        compileSdk: Int? = 35,
    ) = PackageState(
        label = label,
        packageName = packageName,
        versionName = versionName,
        versionCode = versionCode,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        enabled = enabled,
        debuggable = debuggable,
        systemApp = systemApp,
        updatedSystemApp = updatedSystemApp,
        apex = apex,
        removedForUser = removedForUser,
        minSdk = minSdk,
        targetSdk = targetSdk,
        compileSdk = compileSdk,
        signers = signers,
        installSource = installSource,
        origin = origin,
    )

    /** A small mixed inventory that exercises every origin and flag the UI shows. */
    fun inventory(): List<PackageState> = listOf(
        pkg(
            label = "Clock Tools", packageName = "com.clocktools.timetable",
            origin = PackageOrigin.FIRST_PARTY, signers = signers(DIGEST_A),
            installSource = store(installer = null, source = PackageSource.LOCAL_FILE),
        ),
        pkg(
            label = "Drafts", packageName = "com.drafts.compose",
            origin = PackageOrigin.UNVERIFIED, signers = signers(DIGEST_B),
            installSource = store(installer = "com.google.android.packageinstaller", source = PackageSource.DOWNLOADED_FILE),
        ),
        pkg(
            label = "Settings", packageName = "com.android.settings",
            origin = PackageOrigin.SYSTEM, systemApp = true, signers = signers(DIGEST_C, platform = true),
            installSource = InstallSource.UNKNOWN, versionName = "15", versionCode = 35,
            firstInstallTime = 0, lastUpdateTime = 0,
        ),
        pkg(
            label = "Browser Core", packageName = "com.example.webview",
            origin = PackageOrigin.SYSTEM, systemApp = true, updatedSystemApp = true,
            signers = signers(DIGEST_C, past = listOf(DIGEST_A)),
        ),
        pkg(
            label = "Media Module", packageName = "com.android.mediaprovider",
            origin = PackageOrigin.SYSTEM, systemApp = true, apex = true,
            signers = signers(DIGEST_C, platform = true), installSource = InstallSource.UNKNOWN,
        ),
        pkg(label = "Example App", packageName = "com.example.app"),
        pkg(
            label = "Removed Game", packageName = "com.example.game",
            removedForUser = true, enabled = false, signers = SignerInfoNone,
        ),
        pkg(
            label = "Dual Signed Tool", packageName = "com.example.tool",
            signers = signers(DIGEST_A, DIGEST_B), debuggable = true,
        ),
    )

    private val SignerInfoNone = SignerInfo.NONE
}
