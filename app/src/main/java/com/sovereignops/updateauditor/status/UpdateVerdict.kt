package com.sovereignops.updateauditor.status

enum class UpdateVerdict {
    VERIFIED_CURRENT,
    VERIFIED_UPDATE_AVAILABLE,
    LOCAL_AHEAD_OF_SOURCE,
    MANUAL_CHECK_REQUIRED,
    UNSUPPORTED,
    UNVERIFIED,
    ERROR,
}
