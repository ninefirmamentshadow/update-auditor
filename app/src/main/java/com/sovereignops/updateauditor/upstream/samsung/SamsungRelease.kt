package com.sovereignops.updateauditor.upstream.samsung

data class SamsungRelease(
    val buildNumber: String,
    val androidVersion: String,
    val releaseDate: String,
    val securityPatchLevel: String,
)
