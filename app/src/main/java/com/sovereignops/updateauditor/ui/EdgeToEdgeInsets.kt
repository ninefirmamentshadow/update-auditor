package com.sovereignops.updateauditor.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Keeps content clear of system bars once the window draws edge to edge (always the
 * case at targetSdk 35). The top view is padded by the status bar and cutout, the
 * bottom view by the navigation bar or keyboard, whichever is taller, and both by
 * the side insets. Each view's own padding is preserved and added to.
 */
object EdgeToEdgeInsets {
    fun install(root: View, top: View, bottom: View) {
        val topBase = Sides(top.paddingLeft, top.paddingTop, top.paddingRight, top.paddingBottom)
        val bottomBase = Sides(bottom.paddingLeft, bottom.paddingTop, bottom.paddingRight, bottom.paddingBottom)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            top.updatePadding(
                left = topBase.left + bars.left,
                top = topBase.top + bars.top,
                right = topBase.right + bars.right,
            )
            bottom.updatePadding(
                left = bottomBase.left + bars.left,
                right = bottomBase.right + bars.right,
                bottom = bottomBase.bottom + maxOf(bars.bottom, keyboard.bottom),
            )
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }

    private data class Sides(val left: Int, val top: Int, val right: Int, val bottom: Int)
}
