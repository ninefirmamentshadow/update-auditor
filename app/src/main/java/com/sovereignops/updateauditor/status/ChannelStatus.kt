package com.sovereignops.updateauditor.status

import java.time.Instant

data class ChannelStatus(
    val channel: UpdateChannel,
    val verdict: UpdateVerdict,
    val localValue: String,
    val upstreamValue: String? = null,
    val source: String? = null,
    val checkedAt: Instant? = null,
    val detail: String? = null,
)
