package com.woocommerce.android.ui.products

import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.ui.products.list.ProductListAdapter

/**
 * Class provides the selection library access to stable selection keys and identifying items
 * presented by a [RecyclerView] instance.
 */
class ProductSelectionItemKeyProvider(private val recyclerView: RecyclerView) :
    ItemKeyProvider<Long>(SCOPE_MAPPED) {
    override fun getKey(position: Int): Long? {
        return (recyclerView.adapter as? ProductListAdapter)?.currentList?.get(position)?.remoteId
    }

    override fun getPosition(key: Long): Int {
        return (recyclerView.adapter as? ProductListAdapter)?.currentList
            ?.indexOfFirst { product -> product.remoteId == key } ?: RecyclerView.NO_POSITION
    }
}
