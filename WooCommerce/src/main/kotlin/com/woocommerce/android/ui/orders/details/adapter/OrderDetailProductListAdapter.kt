package com.woocommerce.android.ui.orders.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderDetailProductItemView
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.ViewAddonClickListener
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class OrderDetailProductListAdapter(
    private val orderItems: List<Order.Item>,
    private val productImageMap: ProductImageMap,
    private val formatCurrencyForDisplay: (BigDecimal) -> String,
    private val productItemListener: OrderProductActionListener,
    private val onViewAddonsClick: ViewAddonClickListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class ProductViewHolder(val view: OrderDetailProductItemView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view: OrderDetailProductItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.order_detail_product_list_item, parent, false)
            as OrderDetailProductItemView
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = orderItems[position]
        val imageSize = holder.itemView.resources.getDimensionPixelSize(R.dimen.image_minor_100)
        val productImage = PhotonUtils.getPhotonImageUrl(productImageMap.get(item.uniqueId), imageSize, imageSize)
        (holder as ProductViewHolder).view.initView(
            orderItems[position],
            productImage,
            formatCurrencyForDisplay,
            onViewAddonsClick
        )
        holder.view.setOnClickListener {
            if (item.isVariation) {
                productItemListener.openOrderProductVariationDetail(item.productId, item.variationId)
            } else {
                productItemListener.openOrderProductDetail(item.productId)
            }
        }
    }

    override fun getItemCount() = orderItems.size

    fun notifyProductChanged(productId: Long) {
        for (position in orderItems.indices) {
            if (orderItems[position].productId == productId) {
                notifyItemChanged(position)
            }
        }
    }
}
