package com.woocommerce.android.ui.base

import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter

/**
 * The main fragments hosted by the bottom bar should extend this class
 */
abstract class TopLevelFragment : BaseFragment(), TopLevelFragmentView {
    /**
     * The extending class may use this variable to defer a part of its
     * normal initialization until manually requested.
     */
    var deferInit: Boolean = false

    override var isActive: Boolean = false
        get() {
            return if (isAdded && !isHidden) {
                (activity as? MainNavigationRouter)?.isAtNavigationRoot() ?: false
            } else {
                false
            }
        }

    abstract fun isScrolledToTop(): Boolean

    /**
     * Called when the fragment shows a search view so the toolbar size is shrunk
     * to a non-expanded size
     */
    fun expandMainToolbar(expand: Boolean, animate: Boolean) {
        (activity as? MainActivity)?.expandToolbar(expand, animate)
    }

    /**
     * Called when the fragment hides a search view so the toolbar can be re-expanded
     * if scrolled to the top
     */
    fun restoreMainToolbar() {
        if (isScrolledToTop()) {
            expandMainToolbar(true, true)
        }
    }
}
