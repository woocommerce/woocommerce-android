package com.woocommerce.android.ui.products

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_PRODUCT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.model.Product
import com.woocommerce.android.util.CurrencyFormatter

typealias OnProductClickListener = (remoteProductId: Long, sharedView: View?, product: Product) -> Unit

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
        val view = ProductListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductItemViewHolder(
            view
        )
    }

    override fun onCurrentListChanged(previousList: MutableList<Product>, currentList: MutableList<Product>) {
        super.onCurrentListChanged(previousList, currentList)
        Log.d("AAA", "Current list changed")
    }

    override fun onBindViewHolder(holder: ProductItemViewHolder, position: Int) {
        val product = getItem(position)

        holder.bind(
            product,
            currencyFormatter,
            isActivated = tracker?.isSelected(product.remoteId) ?: false
        )

        holder.itemView.setOnClickListener {
            if (product.numVariations == 0) {
                product.isSelected = !product.isSelected
            }
            if (product.isSelected) {
                holder.viewBinding.productItemView.binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        holder.viewBinding.productItemView.binding.root.context, R.color.color_primary
                    )
                )
                holder.viewBinding.productItemView.binding.productImageSelected.isVisible = true
                holder.viewBinding.productItemView.binding.root.background.alpha = 80
            } else {
                holder.viewBinding.productItemView.binding.productImageSelected.isVisible = false
                holder.viewBinding.productItemView.binding.root.background = null
            }
            AnalyticsTracker.track(PRODUCT_LIST_PRODUCT_TAPPED)
            clickListener?.invoke(product.remoteId, holder.itemView, product)
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }
}
