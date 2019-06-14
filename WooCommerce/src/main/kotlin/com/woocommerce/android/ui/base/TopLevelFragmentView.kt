package com.woocommerce.android.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Special interface for top-level fragments hosted by the bottom bar.
 */
interface TopLevelFragmentView : androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
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
     * Return the title that should appear in the action bar while this fragment is
     * visible.
     */
    fun getFragmentTitle(): String

    /**
     * Refresh this top-level fragment data and reset its state.
     */
    fun refreshFragmentState()

    /**
     * Scroll to the top of this view
     */
    fun scrollToTop()
}
