package com.woocommerce.android.ui.base

import android.support.v4.app.Fragment

/**
 * All top-level fragments and their child fragments should extend this class, and if they contain
 * a scrolling view they should implement the two functions below. We use these to tell the main
 * activity to show/hide the bottom navbar when the user scrolls the fragment content
 */
abstract class BaseFragment : Fragment() {
    interface BaseFragmentScrollListener {
        fun onFragmentScrollUp()
        fun onFragmentScrollDown()
    }

    fun onScrollUp() {
        if (activity is BaseFragmentScrollListener) {
            (activity as BaseFragmentScrollListener).onFragmentScrollUp()
        }
    }

    fun onScrollDown() {
        if (activity is BaseFragmentScrollListener) {
            (activity as BaseFragmentScrollListener).onFragmentScrollDown()
        }
    }
}
