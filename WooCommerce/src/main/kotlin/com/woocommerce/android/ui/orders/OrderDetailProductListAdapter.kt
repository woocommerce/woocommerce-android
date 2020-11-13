package com.woocommerce.android.ui.orders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_PRODUCT_TAPPED
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.products.ProductHelper
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class OrderDetailProductListAdapter(
    private val orderItems: List<Order.Item>,
    private val productImageMap: ProductImageMap,
    private val formatCurrencyForDisplay: (BigDecimal) -> String,
    private var isExpanded: Boolean,
    private val productListener: OrderProductActionListener?
) : RecyclerView.Adapter<OrderDetailProductListAdapter.ViewHolder>() {
    class ViewHolder(val view: OrderDetailProductItemView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: OrderDetailProductItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.order_detail_product_list_item, parent, false)
            as OrderDetailProductItemView
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = orderItems[position]
        val productId = ProductHelper.productOrVariationId(item.productId, item.variationId)
        val imageSize = holder.view.resources.getDimensionPixelSize(R.dimen.image_minor_100)
        val productImage = PhotonUtils.getPhotonImageUrl(productImageMap.get(productId), imageSize, imageSize)
        holder.view.initView(orderItems[position], productImage, isExpanded, formatCurrencyForDisplay)
        holder.view.setOnClickListener {
            AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_TAPPED)
            productListener?.openOrderProductDetail(productId)
        }
    }

    override fun getItemCount() = orderItems.size
}
