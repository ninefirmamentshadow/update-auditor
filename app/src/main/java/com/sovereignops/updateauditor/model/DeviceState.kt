package com.sovereignops.updateauditor.model

data class DeviceState(
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val buildId: String,
    val firmwareBuild: String,
    val fingerprint: String,
    val baseband: String,
)
