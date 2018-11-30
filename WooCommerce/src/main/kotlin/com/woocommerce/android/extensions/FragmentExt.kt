package com.woocommerce.android.extensions

import android.support.v4.app.Fragment

interface FragmentScrollListener {
    fun onFragmentScrollUp()
    fun onFragmentScrollDown()
}

fun Fragment.onScrollUp() {
    (activity as? FragmentScrollListener)?.onFragmentScrollUp()
}

fun Fragment.onScrollDown() {
    (activity as? FragmentScrollListener)?.onFragmentScrollDown()
}
