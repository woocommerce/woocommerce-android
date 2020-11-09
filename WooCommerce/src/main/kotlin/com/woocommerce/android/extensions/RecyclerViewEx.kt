package com.woocommerce.android.extensions

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Returns True if the recycler view is scrolled
 */
fun RecyclerView.isScrolledToTop(): Boolean {
    (layoutManager as? LinearLayoutManager)?.let {
        return it.findFirstCompletelyVisibleItemPosition() <= 1
    }
    return true
}
