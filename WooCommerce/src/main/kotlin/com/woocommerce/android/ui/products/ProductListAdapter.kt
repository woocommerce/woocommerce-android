package com.woocommerce.android.ui.products

import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_LIST_PRODUCT_TAPPED
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.isSameList

class ProductListAdapter(
    private val clickListener: OnProductClickListener? = null,
    private val loadMoreListener: OnLoadMoreListener
) : RecyclerView.Adapter<ProductItemViewHolder>() {
    private val productList = ArrayList<Product>()

    // allow the selection library to track the selections of the user
    var tracker: SelectionTracker<Long>? = null

    interface OnProductClickListener {
        fun onProductClick(remoteProductId: Long)
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productList[position].remoteId

    override fun getItemCount() = productList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ProductItemViewHolder(parent)

    override fun onBindViewHolder(holder: ProductItemViewHolder, position: Int) {
        val product = productList[position]

        holder.bind(product, tracker?.isSelected(product.remoteId) ?: false)

        holder.itemView.setOnClickListener {
            AnalyticsTracker.track(PRODUCT_LIST_PRODUCT_TAPPED)
            clickListener?.onProductClick(product.remoteId)
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    fun setProductList(products: List<Product>) {
        if (!productList.isSameList(products)) {
            val diffResult = DiffUtil.calculateDiff(ProductItemDiffUtil(productList, products))
            productList.clear()
            productList.addAll(products)
            diffResult.dispatchUpdatesTo(this)
        }
    }
}
