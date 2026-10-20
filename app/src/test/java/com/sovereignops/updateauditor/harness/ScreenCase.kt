package com.sovereignops.updateauditor.harness

/**
 * One cell of the regression-screenshot matrix: a screen width, a system font
 * scale, and light or dark. 320 dp is the narrowest phone we care about and
 * 411 dp a common modern phone; font scale 2.0 is the largest accessibility
 * setting, where layouts break first.
 */
data class ScreenCase(
    val widthDp: Int,
    val heightDp: Int,
    val fontScale: Float,
    val night: Boolean,
) {
    /** Robolectric qualifier string; order follows Android's qualifier precedence. */
    val qualifiers: String
        get() = "w${widthDp}dp-h${heightDp}dp-${if (night) "night" else "notnight"}-mdpi"

    /** File-name-safe identifier, e.g. `w320_fs2p0_dark`. */
    val slug: String
        get() = "w${widthDp}_fs${fontScale.toString().replace('.', 'p')}_${if (night) "dark" else "light"}"

    override fun toString(): String = slug

    companion object {
        fun matrix(): List<ScreenCase> {
            val sizes = listOf(320 to 640, 411 to 891)
            val scales = listOf(1.0f, 2.0f)
            return sizes.flatMap { (w, h) ->
                scales.flatMap { scale ->
                    listOf(false, true).map { night -> ScreenCase(w, h, scale, night) }
                }
            }
        }

        /** JUnit `Parameterized` shape: one single-element array per case. */
        fun parameters(): List<Array<Any>> = matrix().map { arrayOf<Any>(it) }
    }
}
