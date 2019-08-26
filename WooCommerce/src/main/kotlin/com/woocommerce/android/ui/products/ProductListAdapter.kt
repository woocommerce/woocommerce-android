package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_LIST_PRODUCT_TAPPED
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductListAdapter.ProductViewHolder
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.OnBackorder
import com.woocommerce.android.ui.products.ProductStockStatus.OutOfStock
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import kotlinx.android.synthetic.main.product_list_item.view.*
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils

class ProductListAdapter(
    private val context: Context,
    private val clickListener: OnProductClickListener,
    private val loadMoreListener: OnLoadMoreListener
) : RecyclerView.Adapter<ProductViewHolder>() {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
    private val productList = ArrayList<Product>()

    interface OnProductClickListener {
        fun onProductClick(remoteProductId: Long)
    }

    interface OnLoadMoreListener {
        fun onRequestLoadMore()
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productList[position].remoteId

    override fun getItemCount() = productList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(LayoutInflater.from(context).inflate(R.layout.product_list_item, parent, false))
    }

    private fun getProductStockStatusText(product: Product): String {
        return when (product.stockStatus) {
            InStock -> {
                if (product.type == VARIABLE) {
                    if (product.numVariations > 0) {
                        context.getString(R.string.product_stock_status_instock_with_variations, product.numVariations)
                    } else {
                        context.getString(R.string.product_stock_status_instock)
                    }
                } else {
                    context.getString(R.string.product_stock_count, FormatUtils.formatInt(product.stockQuantity))
                }
            }
            OutOfStock -> {
                context.getString(R.string.product_stock_status_out_of_stock)
            }
            OnBackorder -> {
                context.getString(R.string.product_stock_status_on_backorder)
            }
            else -> {
                product.stockStatus.value
            }
        }
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        val productName = if (product.name.isEmpty()) {
            context.getString(R.string.untitled)
        } else {
            product.name
        }
        holder.txtProductName.text = productName

        if (product.manageStock) {
            holder.txtProductStock.visibility = View.VISIBLE
            holder.txtProductStock.text = getProductStockStatusText(product)
        } else {
            holder.txtProductStock.visibility = View.GONE
        }

        product.firstImageUrl?.let {
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
            GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(holder.imgProduct)
        } ?: holder.imgProduct.setImageResource(R.drawable.ic_product)

        if (product.status != null && product.status != ProductStatus.PUBLISH) {
            holder.statusFrame.visibility = View.VISIBLE
            holder.txtStatus.text = product.status.toString(context)
        } else {
            holder.statusFrame.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            AnalyticsTracker.track(PRODUCT_LIST_PRODUCT_TAPPED)
            clickListener.onProductClick(product.remoteId)
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    private class ProductItemDiffUtil(val items: List<Product>, val result: List<Product>) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem.stockQuantity == newItem.stockQuantity &&
                    oldItem.stockStatus == newItem.stockStatus &&
                    oldItem.status == newItem.status &&
                    oldItem.manageStock == newItem.manageStock &&
                    oldItem.type == newItem.type &&
                    oldItem.numVariations == newItem.numVariations &&
                    oldItem.name == newItem.name
        }

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return items[oldItemPosition] == result[newItemPosition]
        }
    }

    fun setProductList(products: List<Product>) {
        val diffResult = DiffUtil.calculateDiff(ProductItemDiffUtil(productList, products))
        productList.clear()
        productList.addAll(products)
        diffResult.dispatchUpdatesTo(this)
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.productImage
        val txtProductName: TextView = view.productName
        val txtProductStock: TextView = view.productStock
        val txtStatus: TextView = view.statusBadge
        val statusFrame: View = view.statusFrame
    }
}
