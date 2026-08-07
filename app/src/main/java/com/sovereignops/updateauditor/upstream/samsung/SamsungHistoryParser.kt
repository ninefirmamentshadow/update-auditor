package com.sovereignops.updateauditor.upstream.samsung

import org.jsoup.Jsoup

object SamsungHistoryParser {
    private const val BUILD_PREFIX = "Build Number :"
    private const val ANDROID_PREFIX = "Android version :"
    private const val RELEASE_DATE_PREFIX = "Release Date :"
    private const val PATCH_PREFIX = "Security patch level :"

    fun parse(html: String): List<SamsungRelease> {
        val lines = Jsoup.parse(html)
            .wholeText()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()

        val releases = mutableListOf<SamsungRelease>()
        var index = 0

        while (index < lines.size) {
            val line = lines[index]
            if (!line.startsWith(BUILD_PREFIX)) {
                index++
                continue
            }

            val buildNumber = line.valueAfter(BUILD_PREFIX)
            var androidVersion: String? = null
            var releaseDate: String? = null
            var securityPatch: String? = null

            var cursor = index + 1
            while (cursor < lines.size && !lines[cursor].startsWith(BUILD_PREFIX)) {
                val candidate = lines[cursor]
                when {
                    candidate.startsWith(ANDROID_PREFIX) ->
                        androidVersion = candidate.valueAfter(ANDROID_PREFIX)
                    candidate.startsWith(RELEASE_DATE_PREFIX) ->
                        releaseDate = candidate.valueAfter(RELEASE_DATE_PREFIX)
                    candidate.startsWith(PATCH_PREFIX) ->
                        securityPatch = candidate.valueAfter(PATCH_PREFIX)
                }

                if (androidVersion != null && releaseDate != null && securityPatch != null) {
                    break
                }
                cursor++
            }

            if (
                buildNumber.isNotBlank() &&
                !androidVersion.isNullOrBlank() &&
                !releaseDate.isNullOrBlank() &&
                !securityPatch.isNullOrBlank()
            ) {
                releases += SamsungRelease(
                    buildNumber = buildNumber,
                    androidVersion = androidVersion,
                    releaseDate = releaseDate,
                    securityPatchLevel = securityPatch,
                )
            }

            index = if (cursor > index) cursor else index + 1
        }

        require(releases.isNotEmpty()) { "Samsung history contained no valid release entries" }
        return releases
    }

    private fun String.valueAfter(prefix: String): String =
        removePrefix(prefix).trim()
}
