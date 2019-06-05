package com.woocommerce.android.extensions

interface FragmentScrollListener {
    fun onFragmentScrollUp()
    fun onFragmentScrollDown()
}

fun androidx.fragment.app.Fragment.onScrollUp() {
    (activity as? FragmentScrollListener)?.onFragmentScrollUp()
}

fun androidx.fragment.app.Fragment.onScrollDown() {
    (activity as? FragmentScrollListener)?.onFragmentScrollDown()
}
