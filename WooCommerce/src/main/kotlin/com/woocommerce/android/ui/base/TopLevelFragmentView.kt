package com.woocommerce.android.ui.base

/**
 * Special interface for top-level fragments hosted by the bottom bar.
 */
interface TopLevelFragmentView : BaseFragmentView {
    var isActive: Boolean

    /**
     * Refresh this top-level fragment data and reset its state.
     */
    fun refreshFragmentState() { }

    /**
     * Scroll to the top of this view
     */
    fun scrollToTop()

    /**
     * User returned to this top level fragment from a nav component fragment
     */
    fun onReturnedFromChildFragment() {

    }

    /**
     * A child fragment for the active tab has been opened.
     */
    fun onChildFragmentOpened() {
        // Override this method if the top level fragment needs to perform some
        // sort of cleanup once a child fragment has been opened.
    }
}
