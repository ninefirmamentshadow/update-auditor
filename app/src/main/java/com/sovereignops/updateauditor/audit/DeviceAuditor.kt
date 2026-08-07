package com.sovereignops.updateauditor.audit

import android.os.Build
import com.sovereignops.updateauditor.model.DeviceState

object DeviceAuditor {
    fun read(): DeviceState = DeviceState(
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        androidVersion = Build.VERSION.RELEASE,
        sdkInt = Build.VERSION.SDK_INT,
        securityPatch = Build.VERSION.SECURITY_PATCH.ifBlank { "Unavailable" },
        buildId = Build.ID,
        fingerprint = Build.FINGERPRINT,
        baseband = Build.getRadioVersion()?.takeIf { it.isNotBlank() } ?: "Unavailable",
    )
}
