package com.sovereignops.updateauditor.upstream

import java.time.Instant

interface UpdateProvider<T> {
    fun fetch(key: String): ProviderResult<T>
}

sealed class ProviderResult<out T> {
    data class Success<T>(
        val value: T,
        val source: String,
        val checkedAt: Instant,
    ) : ProviderResult<T>()

    data class Unsupported(
        val reason: String,
    ) : ProviderResult<Nothing>()

    data class Failure(
        val reason: String,
    ) : ProviderResult<Nothing>()
}
