package com.woocommerce.android.ui.base

/**
 * Special interface for top-level fragments hosted by the bottom bar.
 */
interface TopLevelFragmentView : BaseFragmentView {
    /**
     * Scroll to the top of this view
     */
    fun scrollToTop()
}
