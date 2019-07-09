package com.woocommerce.android.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Special interface for top-level fragments hosted by the bottom bar.
 */
interface TopLevelFragmentView : BaseFragmentView {
    var isActive: Boolean

    /**
     * Inflate the fragment view and return to be added to the parent
     * container.
     */
    fun onCreateFragmentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?

    /**
     * Refresh this top-level fragment data and reset its state.
     */
    fun refreshFragmentState()

    /**
     * Scroll to the top of this view
     */
    fun scrollToTop()

    /**
     * User returned to this top level fragment from a nav component fragment
     */
    fun onReturnedFromChildFragment()
}
