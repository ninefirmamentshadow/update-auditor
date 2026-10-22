package com.sovereignops.updateauditor.upstream.samsung

import com.sovereignops.updateauditor.upstream.ProviderResult
import com.sovereignops.updateauditor.upstream.UpdateProvider
import java.net.URL
import java.time.Instant
import javax.net.ssl.HttpsURLConnection

class SamsungHistoryProvider(
    private val htmlLoader: (String) -> String = ::loadHtml,
    private val now: () -> Instant = Instant::now,
) : UpdateProvider<List<SamsungRelease>> {
    override fun fetch(key: String): ProviderResult<List<SamsungRelease>> {
        val source = SamsungSourceRegistry.sourceFor(key)
            ?: return ProviderResult.Unsupported("No Samsung history source is registered for $key")

        return runCatching {
            val html = htmlLoader(source)
            val releases = SamsungHistoryParser.parse(html)
            ProviderResult.Success(
                value = releases,
                source = source,
                checkedAt = now(),
            )
        }.getOrElse { error ->
            ProviderResult.Failure("Samsung check failed: ${error.javaClass.simpleName}")
        }
    }

    companion object {
        private fun loadHtml(source: String): String {
            val connection = URL(source).openConnection() as HttpsURLConnection
            return try {
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("User-Agent", "UpdateAuditor/0.2")

                val responseCode = connection.responseCode
                require(responseCode in 200..299) {
                    "Samsung source returned HTTP $responseCode"
                }

                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
}
