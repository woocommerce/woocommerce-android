package com.woocommerce.android.ui.orders.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailProductChildItemBinding
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.getColorCompat
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.details.OrderProduct
import com.woocommerce.android.ui.products.ProductImageLoader
import com.woocommerce.android.ui.products.ProductImageViewTarget
import java.math.BigDecimal

class OrderDetailProductChildItemListAdapter(
    private val productItems: List<OrderProduct.ProductItem>,
    private val productImageLoaderFactory: ProductImageLoader.Factory,
    private val formatCurrencyForDisplay: (BigDecimal) -> String,
    private val productItemListener: OrderProductActionListener
) :
    RecyclerView.Adapter<OrderDetailProductChildItemListAdapter.OrderDetailProductChildItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDetailProductChildItemViewHolder {
        val viewBinding = OrderDetailProductChildItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderDetailProductChildItemViewHolder(viewBinding, productImageLoaderFactory)
    }

    override fun onBindViewHolder(holder: OrderDetailProductChildItemViewHolder, position: Int) {
        holder.bind(
            productItems[position],
            productItemListener,
            formatCurrencyForDisplay
        )
    }

    override fun getItemCount(): Int = productItems.size

    class OrderDetailProductChildItemViewHolder(
        private val binding: OrderDetailProductChildItemBinding,
        productImageLoaderFactory: ProductImageLoader.Factory
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private val imageTarget = ProductImageViewTarget(binding.productInfoIcon, productImageLoaderFactory)

        private val context = binding.root.context

        private val emptyTotalColor = context.getColorCompat(R.color.color_on_surface_disabled)
        private val defaultTotalColor = context.getColorCompat(R.color.color_on_surface_high)

        fun bind(
            productItem: OrderProduct.ProductItem,
            productItemListener: OrderProductActionListener,
            formatCurrencyForDisplay: (BigDecimal) -> String
        ) {
            val item = productItem.product
            imageTarget.load(item.uniqueId)

            binding.productInfoName.text = item.name
            val orderTotal = formatCurrencyForDisplay(item.total)
            val totalColor = if (item.total.compareTo(BigDecimal.ZERO) == 0) emptyTotalColor else defaultTotalColor

            binding.productInfoTotal.apply {
                text = orderTotal
                setTextColor(totalColor)
            }

            val productPrice = formatCurrencyForDisplay(item.price)
            with(binding.productInfoAttributes) {
                val attributes = item.displayableAttributes
                isVisible = attributes.isNotEmpty()
                text = attributes.joinToString(separator = "\n") {
                    itemView.resources.getString(
                        R.string.orderdetail_product_lineitem_attribute,
                        it.key,
                        it.value
                    )
                }
            }
            binding.productInfoQuantityAndPrice.text = itemView.resources.getString(
                R.string.orderdetail_product_lineitem_quantity_and_price,
                item.quantity.formatToString(), productPrice
            )

            with(binding.productInfoSKU) {
                isVisible = item.sku.isNotEmpty()
                val productSku = context.getString(R.string.orderdetail_product_lineitem_sku_value, item.sku)
                binding.productInfoSKU.text = productSku
            }

            itemView.setOnClickListener {
                if (item.isVariation) {
                    productItemListener.openOrderProductVariationDetail(item.productId, item.variationId)
                } else {
                    productItemListener.openOrderProductDetail(item.productId)
                }
            }
        }
    }
}
