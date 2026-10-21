package com.sovereignops.updateauditor.audit

import android.os.Build
import com.sovereignops.updateauditor.model.DeviceState

/** Local build identifiers. A value Android does not expose is an empty string; the UI shows it as unavailable. */
object DeviceAuditor {
    fun read(): DeviceState = DeviceState(
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        androidVersion = Build.VERSION.RELEASE,
        sdkInt = Build.VERSION.SDK_INT,
        securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty(),
        buildId = Build.ID,
        fingerprint = Build.FINGERPRINT,
        baseband = Build.getRadioVersion().orEmpty(),
    )
}
