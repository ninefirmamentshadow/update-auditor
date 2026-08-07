package com.sovereignops.updateauditor.upstream.samsung

import com.sovereignops.updateauditor.upstream.ProviderResult
import com.sovereignops.updateauditor.upstream.UpdateProvider
import java.net.URL
import java.time.Instant
import javax.net.ssl.HttpsURLConnection

class SamsungHistoryProvider : UpdateProvider<List<SamsungRelease>> {
    override fun fetch(key: String): ProviderResult<List<SamsungRelease>> {
        val source = SamsungSourceRegistry.sourceFor(key)
            ?: return ProviderResult.Unsupported("No Samsung history source is registered for $key")

        val connection = runCatching {
            URL(source).openConnection() as HttpsURLConnection
        }.getOrElse { error ->
            return ProviderResult.Failure("Could not open Samsung source: ${error.javaClass.simpleName}")
        }

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "UpdateAuditor/0.2")

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                ProviderResult.Failure("Samsung source returned HTTP $responseCode")
            } else {
                val html = connection.inputStream.bufferedReader().use { it.readText() }
                val releases = runCatching { SamsungHistoryParser.parse(html) }
                    .getOrElse { error ->
                        return ProviderResult.Failure(
                            "Samsung source could not be parsed: ${error.javaClass.simpleName}",
                        )
                    }

                ProviderResult.Success(
                    value = releases,
                    source = source,
                    checkedAt = Instant.now(),
                )
            }
        } catch (error: Exception) {
            ProviderResult.Failure("Samsung check failed: ${error.javaClass.simpleName}")
        } finally {
            connection.disconnect()
        }
    }
}
