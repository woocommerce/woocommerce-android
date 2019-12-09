package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
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
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils

class ProductListAdapter(
    private val context: Context,
    private val clickListener: OnProductClickListener
) : RecyclerView.Adapter<ProductViewHolder>() {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
    private val productList = ArrayList<Product>()
    private val bullet = "\u2022"
    private val statusColor = ContextCompat.getColor(context, R.color.blue_50)
    private val stockColor = ContextCompat.getColor(context, R.color.wc_grey_mid)

    interface OnProductClickListener {
        fun onProductClick(remoteProductId: Long)
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productList[position].remoteId

    override fun getItemCount() = productList.size

    private fun getProductStockStatusText(product: Product): String? {
        val statusHtml = if (product.status != null && product.status != ProductStatus.PUBLISH) {
            "<font color=$statusColor>${product.status.toString(context)}</font>"
        } else {
            null
        }

        if (!product.manageStock) {
            return statusHtml
        }

        val stock = when (product.stockStatus) {
            InStock -> {
                if (product.type == VARIABLE) {
                    if (product.numVariations > 0) {
                        context.getString(
                                R.string.product_stock_status_instock_with_variations,
                                product.numVariations
                        )
                    } else {
                        context.getString(R.string.product_stock_status_instock)
                    }
                } else {
                    context.getString(
                            R.string.product_stock_count,
                            FormatUtils.formatInt(product.stockQuantity)
                    )
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
        val stockHtml = "<font color=$stockColor>$stock</font>"

        return if (statusHtml != null) "$statusHtml $bullet $stockHtml" else stockHtml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val holder = ProductViewHolder(LayoutInflater.from(context).inflate(R.layout.product_list_item, parent, false))
        holder.imgProduct.clipToOutline = true
        return holder
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.txtProductName.text = if (product.name.isEmpty()) {
            context.getString(R.string.untitled)
        } else {
            HtmlUtils.fastStripHtml(product.name)
        }

        val stockAndStatus = getProductStockStatusText(product)
        if (stockAndStatus != null) {
            holder.txtProductStockAndStatus.visibility = View.VISIBLE
            holder.txtProductStockAndStatus.text = HtmlCompat.fromHtml(
                    stockAndStatus,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        } else {
            holder.txtProductStockAndStatus.visibility = View.GONE
        }

        val firstImage = product.firstImageUrl
        val size: Int
        if (firstImage.isNullOrEmpty()) {
            size = imageSize / 2
            holder.imgProduct.setImageResource(R.drawable.ic_product)
        } else {
            size = imageSize
            val imageUrl = PhotonUtils.getPhotonImageUrl(firstImage, imageSize, imageSize)
            GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(holder.imgProduct)
        }
        holder.imgProduct.layoutParams.apply {
            height = size
            width = size
        }

        holder.itemView.setOnClickListener {
            AnalyticsTracker.track(PRODUCT_LIST_PRODUCT_TAPPED)
            clickListener.onProductClick(product.remoteId)
        }
    }

    fun setProductList(products: List<Product>) {
        fun isSameList(): Boolean {
            if (products.size != productList.size) {
                return false
            }
            for (index in products.indices) {
                val oldItem = productList[index]
                val newItem = products[index]
                if (!oldItem.isSameProduct(newItem)) {
                    return false
                }
            }
            return true
        }

        if (isSameList()) {
            return
        }

        productList.clear()
        productList.addAll(products)
        notifyDataSetChanged()
    }

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.productImage
        val txtProductName: TextView = view.productName
        val txtProductStockAndStatus: TextView = view.productStockAndStatus
    }
}
