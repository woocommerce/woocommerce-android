package com.woocommerce.android.ui.base

import android.support.v4.app.Fragment
import com.woocommerce.android.ui.base.TopLevelFragment.FragmentScrollListener

/**
 * All top-level fragments and their child fragments should extend this class, and if they contain
 * a scrolling view they should implement the two functions below. We use these to tell the main
 * activity to show/hide the bottom navbar when the user scrolls the fragment content
 */
abstract class BaseFragment : Fragment() {
    fun onScrollUp() {
        if (activity is FragmentScrollListener) {
            (activity as FragmentScrollListener).onFragmentScrollUp()
        }
    }

    fun onScrollDown() {
        if (activity is FragmentScrollListener) {
            (activity as FragmentScrollListener).onFragmentScrollDown()
        }
    }
}
