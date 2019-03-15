package com.woocommerce.android.extensions

import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout

interface FragmentScrollListener {
    fun onFragmentScrollUp()
    fun onFragmentScrollDown()
}

fun Fragment.onScrollUp(refreshLayout: SwipeRefreshLayout? = null) {
    enableSwipeRefresh(refreshLayout, true)
    (activity as? FragmentScrollListener)?.onFragmentScrollUp()
}

fun Fragment.onScrollDown(refreshLayout: SwipeRefreshLayout? = null) {
    enableSwipeRefresh(refreshLayout, false)
    (activity as? FragmentScrollListener)?.onFragmentScrollDown()
}

/**
 * This function was added to allow the enable and disable of the [SwipeRefreshLayout] isEnabled property.
 * When the gesture for refresh operation is enabled (isEnabled property is true), this interfere with the
 * overScrollMode of an eventual scrollable view that is inside (ie NestedScrollView or RecyclerView).
 *
 * This function should be used to set to false the SwipeRefreshLayout.isEnabled when scrolling down (so to
 * allow the overScrollMode to do its job) and to set to true the SwipeRefreshLayout.isEnabled when scrolling up (so
 * to allow the SwipeRefreshLayout to refresh the view)
 *
 * @param refreshLayout The SwipeRefreshLayout to manage
 * @param enable Set to true to enable, false to disable
 *
 */
private fun enableSwipeRefresh(refreshLayout: SwipeRefreshLayout?, enable: Boolean) {
    if (refreshLayout?.isEnabled != enable) refreshLayout?.isEnabled = enable
}
