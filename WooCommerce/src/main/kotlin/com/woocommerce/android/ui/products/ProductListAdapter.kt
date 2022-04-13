package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_PRODUCT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.databinding.ProductsListSortFilterItemBinding
import com.woocommerce.android.model.Product

typealias OnProductClickListener = (remoteProductId: Long, sharedView: View?) -> Unit

class ProductListAdapter(
    private val productSortAndFilterListener: ProductSortAndFiltersCard.ProductSortAndFilterListener? = null,
    private inline val clickListener: OnProductClickListener? = null,
    private val loadMoreListener: OnLoadMoreListener
) : ListAdapter<ProductListAdapter.ListItem, RecyclerView.ViewHolder>(ProductListItemDiffCallback) {
    companion object {
        private const val VIEW_TYPE_SORT_FILTERS_ITEM = 0
        private const val VIEW_TYPE_PRODUCT_ITEM = 1
        private const val ITEM_ID_SORT_FILTERS_ITEM = -1L
    }

    var products: List<Product> = emptyList()
        set(value) {
            val list = mutableListOf<ListItem>()

            // Add sort filter item
            sortFilterItem?.let {
                if (sortFilterItem?.show == true) {
                    list.add(it)
                }
            }

            // Add products
            list.addAll(value.map { ListItem.ProductItem(product = it) })

            field = value
            submitList(list)
        }

    private var sortFilterItem: ListItem.SortFilterItem? = null
        set(value) {
            val list = mutableListOf<ListItem>()

            // Add sort filter item
            if (value != null && value.show == true) {
                list.add(value)
            }

            // Add products
            list.addAll(products.map { ListItem.ProductItem(product = it) })

            field = value
            submitList(list)
        }

    fun updateSortFilterItem(
        title: String? = sortFilterItem?.title,
        show: Boolean? = sortFilterItem?.show,
        filterCount: Int? = sortFilterItem?.filterCount
    ) {
        sortFilterItem = if (sortFilterItem == null) {
            ListItem.SortFilterItem(title, show, filterCount)
        } else {
            sortFilterItem?.copy(title = title, show = show, filterCount = filterCount)
        }
    }

    // allow the selection library to track the selections of the user
    var tracker: SelectionTracker<Long>? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = when (getItem(position)) {
        is ListItem.SortFilterItem -> ITEM_ID_SORT_FILTERS_ITEM
        is ListItem.ProductItem -> (getItem(position) as ListItem.ProductItem).product.remoteId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SORT_FILTERS_ITEM -> {
                SortFilterItemViewHolder(
                    ProductsListSortFilterItemBinding.inflate(layoutInflater, parent, false)
                )
            }
            VIEW_TYPE_PRODUCT_ITEM -> {
                ProductItemViewHolder(ProductListItemBinding.inflate(layoutInflater, parent, false))
            }
            else -> throw  IllegalArgumentException()
        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ListItem.SortFilterItem -> VIEW_TYPE_SORT_FILTERS_ITEM
        is ListItem.ProductItem -> VIEW_TYPE_PRODUCT_ITEM
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.SortFilterItem -> (holder as SortFilterItemViewHolder).bind(item)
            is ListItem.ProductItem -> {
                val product = item.product
                (holder as ProductItemViewHolder).bind(
                    product,
                    tracker?.isSelected(product.remoteId) ?: false
                )

                holder.itemView.setOnClickListener {
                    AnalyticsTracker.track(PRODUCT_LIST_PRODUCT_TAPPED)
                    clickListener?.invoke(product.remoteId, holder.itemView)
                }

                if (position == itemCount - 1) {
                    loadMoreListener.onRequestLoadMore()
                }
            }
        }
    }

    private inner class SortFilterItemViewHolder(
        private val binding: ProductsListSortFilterItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sortFilterItem: ListItem.SortFilterItem) {
            productSortAndFilterListener?.let { binding.productsSortFilterCard.initView(it) }

            // Set title
            sortFilterItem.title?.let { binding.productsSortFilterCard.setSortingTitle(it) }

            // Set filter count
            sortFilterItem.filterCount?.let { binding.productsSortFilterCard.updateFilterSelection(it) }
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
