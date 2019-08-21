package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
import org.wordpress.android.util.PhotonUtils

class ProductListAdapter(private val context: Context, private val listener: OnProductClickListener) :
        RecyclerView.Adapter<ProductViewHolder>() {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.product_icon_sz)

    var productList: List<Product> = ArrayList()
        set(value) {
            if (!isSameProductList(value)) {
                field = value
                notifyDataSetChanged()
            }
        }

    interface OnProductClickListener {
        fun onProductClick(remoteProductId: Long)
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return productList[position].remoteId
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(LayoutInflater.from(context).inflate(R.layout.product_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.txtProductName.text = product.name

        if (product.manageStock) {
            holder.txtProductStock.visibility = View.VISIBLE
            holder.txtProductStock.text = when (product.stockStatus) {
                InStock -> {
                    if (product.type == VARIABLE) {
                        context.getString(R.string.product_stock_status_instock)
                    } else {
                        context.getString(R.string.product_stock_count, product.stockQuantity)
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
            listener.onProductClick(product.remoteId)
        }
    }

    /**
     * returns true if the passed list of products is the same as the current list
     */
    private fun isSameProductList(products: List<Product>): Boolean {
        if (products.size != productList.size) {
            return false
        }

        fun containsProduct(product: Product): Boolean {
            productList.forEach {
                if (it.remoteId == product.remoteId) {
                    return true
                }
            }
            return false
        }

        products.forEach {
            if (!containsProduct(it)) {
                return false
            }
        }

        return true
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.productImage
        val txtProductName: TextView = view.productName
        val txtProductStock: TextView = view.productStock
        val txtStatus: TextView = view.statusBadge
        val statusFrame: View = view.statusFrame
    }
}
