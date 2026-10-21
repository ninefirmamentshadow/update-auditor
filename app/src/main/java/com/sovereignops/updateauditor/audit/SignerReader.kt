package com.sovereignops.updateauditor.audit

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.sovereignops.updateauditor.model.SignerInfo

/**
 * Reads signing certificates from a [PackageInfo] fetched with [QueryFlags], and asks
 * the platform whether the package shares the platform's own signature. Local reads
 * only; nothing is verified against anything remote.
 *
 * Informed by the signer-digest approach of AppVerifier (ISC); no code was copied.
 */
internal class SignerReader(private val packageManager: PackageManager) {

    fun read(info: PackageInfo): SignerInfo {
        val current: List<Signature>
        val past: List<Signature>
        val multiple: Boolean
        if (Build.VERSION.SDK_INT >= 28) {
            val signingInfo = info.signingInfo
            multiple = signingInfo?.hasMultipleSigners() == true
            current = signingInfo?.apkContentsSigners?.toList().orEmpty()
            // With a rotation lineage the history lists oldest to newest, ending in the
            // current certificate. It is null for multi-signer packages.
            val history = if (signingInfo?.hasPastSigningCertificates() == true) {
                signingInfo.signingCertificateHistory?.toList().orEmpty()
            } else {
                emptyList()
            }
            val currentDigests = current.map(::digest).toSet()
            past = history.filter { digest(it) !in currentDigests }
        } else {
            @Suppress("DEPRECATION")
            current = info.signatures?.toList().orEmpty()
            multiple = current.size > 1
            past = emptyList()
        }
        return SignerInfo(
            currentDigests = current.map(::digest),
            pastDigests = past.map(::digest),
            multipleSigners = multiple,
            platformSigned = isPlatformSigned(info.packageName),
        )
    }

    /** `checkSignatures(pkg, "android")` matches only when both carry the same certificate set. */
    private fun isPlatformSigned(packageName: String): Boolean = runCatching {
        packageManager.checkSignatures(packageName, PLATFORM_PACKAGE) == PackageManager.SIGNATURE_MATCH
    }.getOrDefault(false)

    private fun digest(signature: Signature): String = CertDigest.sha256Hex(signature.toByteArray())

    private companion object {
        const val PLATFORM_PACKAGE = "android"
    }
}
