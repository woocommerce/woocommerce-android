package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_PRODUCT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.model.Product
import com.woocommerce.android.util.CurrencyFormatter

typealias OnProductClickListener = (remoteProductId: Long, sharedView: View?) -> Unit

class ProductListAdapter(
    private inline val clickListener: OnProductClickListener? = null,
    private val loadMoreListener: OnLoadMoreListener,
    private val currencyFormatter: CurrencyFormatter
) : ListAdapter<Product, ProductItemViewHolder>(ProductItemDiffCallback) {
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

        holder.bind(
            product,
            currencyFormatter,
            isActivated = tracker?.isSelected(product.remoteId) ?: false
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
