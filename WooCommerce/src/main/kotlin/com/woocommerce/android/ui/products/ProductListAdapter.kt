package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_PRODUCT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.model.Product

typealias OnProductClickListener = (remoteProductId: Long, sharedView: View?) -> Unit

class ProductListAdapter(
    private inline val clickListener: OnProductClickListener? = null,
    private val loadMoreListener: OnLoadMoreListener
) : ListAdapter<Product, ProductItemViewHolder>(ProductListItemDiffCallback) {
    // allow the selection library to track the selections of the user
    var tracker: SelectionTracker<Long>? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).remoteId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductItemViewHolder {
        return ProductItemViewHolder(
            ProductListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductItemViewHolder, position: Int) {
        val product = getItem(position)

        holder.bind(product, tracker?.isSelected(product.remoteId) ?: false)

        holder.itemView.setOnClickListener {
            AnalyticsTracker.track(PRODUCT_LIST_PRODUCT_TAPPED)
            clickListener?.invoke(product.remoteId, holder.itemView)
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    object ProductListItemDiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem) = when (oldItem) {
            is ListItem.SortFilterItem -> {
                // There should be only one SortFilterItem
                newItem is ListItem.SortFilterItem
            }
            is ListItem.ProductItem -> oldItem.product.remoteId == (newItem as? ListItem.ProductItem)?.product?.remoteId
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem) = oldItem == newItem
    }

    sealed class ListItem {
        data class SortFilterItem(val title: String?, val show: Boolean?, val filterCount: Int?) : ListItem()
        data class ProductItem(val product: Product) : ListItem()
    }
}
