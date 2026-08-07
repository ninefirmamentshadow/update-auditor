package com.sovereignops.updateauditor.upstream.samsung

object SamsungSourceRegistry {
    private val sources = mapOf(
        "SM-A166U" to "https://doc.samsungmobile.com/SM-A166U/031752241216/eng.html",
    )

    fun sourceFor(model: String): String? = sources[model.uppercase()]
}
