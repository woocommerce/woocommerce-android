package com.woocommerce.android.ui.base

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Special interface for top-level fragments like those hosted by the bottom bar.
 * Adds an extra layer of management to ensure proper routing and handling of child
 * fragments and their associated back stack.
 */
interface ParentFragmentView : FragmentManager.OnBackStackChangedListener {
    /**
     * Load the provided fragment into the current view and disable the main
     * underlying fragment view.
     */
    fun loadChildFragment(fragment: Fragment)

    /**
     * Inflate the fragment view and return to be added to the parent
     * container.
     */
    fun onCreateFragmentView(inflater: LayoutInflater?,
                             container: ViewGroup?,
                             savedInstanceState: Bundle?): View?

    /**
     * Return the title that should appear in the action bar while this fragment is
     * visible.
     */
    fun getFragmentTitle(): String

    /**
     * Refresh this top-level fragment data and reset its state.
     */
    fun refreshFragmentState()
}
