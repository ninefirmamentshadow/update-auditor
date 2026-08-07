package com.sovereignops.updateauditor.audit

import android.os.Build
import com.sovereignops.updateauditor.model.DeviceState

object DeviceAuditor {
    fun read(): DeviceState {
        val fingerprint = Build.FINGERPRINT
        val baseband = Build.getRadioVersion()?.takeIf { it.isNotBlank() } ?: "Unavailable"
        val firmwareBuild = FirmwareBuildResolver.resolve(
            model = Build.MODEL,
            incremental = Build.VERSION.INCREMENTAL,
            display = Build.DISPLAY,
            fingerprint = fingerprint,
            baseband = baseband,
        )

        return DeviceState(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.ifBlank { "Unavailable" },
            buildId = Build.ID,
            firmwareBuild = firmwareBuild,
            fingerprint = fingerprint,
            baseband = baseband,
        )
    }
}
