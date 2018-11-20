package com.woocommerce.android.ui.base

import android.support.v4.app.Fragment
import com.woocommerce.android.ui.base.TopLevelFragment.FragmentScrollListener

abstract class BaseFragment : Fragment() {
    /**
     * We use these to tell the main activity to show/hide the bottom navbar when the user scrolls the fragment
     */
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
