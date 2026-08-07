package com.sovereignops.updateauditor.audit

import com.sovereignops.updateauditor.model.PackageOrigin

object PackageClassifier {
    private val firstPartyPackages = setOf(
        "com.clocktools.timetable",
        "com.clocktools.timetable.debug",
        "com.drafts.compose",
        "com.drafts.compose.debug",
        "com.sovereignops.fieldwatch",
        "com.sovereignops.fieldwatch.debug",
        "com.sovereignops.ledger",
        "com.sovereignops.ledger.debug",
        "com.sovereignops.updateauditor",
        "com.sovereignops.updateauditor.debug",
    )

    fun classify(packageName: String, isSystemApp: Boolean): PackageOrigin = when {
        packageName in firstPartyPackages -> PackageOrigin.FIRST_PARTY
        isSystemApp -> PackageOrigin.SYSTEM
        else -> PackageOrigin.USER
    }
}
