package com.woocommerce.android.ui.orders.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailProductChildItemBinding
import com.bumptech.glide.Glide
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.getColorCompat
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.details.OrderProduct
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class OrderDetailProductChildItemListAdapter(
    private val productItems: List<OrderProduct.ProductItem>,
    private val productImageMap: ProductImageMap,
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
        return OrderDetailProductChildItemViewHolder(viewBinding)
    }

    override fun onBindViewHolder(holder: OrderDetailProductChildItemViewHolder, position: Int) {
        holder.bind(
            productItems[position],
            productImageMap,
            productItemListener,
            formatCurrencyForDisplay
        )
    }

    override fun getItemCount(): Int = productItems.size

    class OrderDetailProductChildItemViewHolder(
        private val binding: OrderDetailProductChildItemBinding
    ) :
        RecyclerView.ViewHolder(binding.root) {

        private val context = binding.root.context

        private val emptyTotalColor = context.getColorCompat(R.color.color_on_surface_disabled)
        private val defaultTotalColor = context.getColorCompat(R.color.color_on_surface_high)

        fun bind(
            productItem: OrderProduct.ProductItem,
            productImageMap: ProductImageMap,
            productItemListener: OrderProductActionListener,
            formatCurrencyForDisplay: (BigDecimal) -> String
        ) {
            val item = productItem.product
            val imageSize = itemView.resources.getDimensionPixelSize(R.dimen.image_minor_100)
            val productImage = PhotonUtils.getPhotonImageUrl(productImageMap.get(item.uniqueId), imageSize, imageSize)

            binding.productInfoName.text = item.name
            val orderTotal = formatCurrencyForDisplay(item.total)
            val totalColor = if (item.total.compareTo(BigDecimal.ZERO) == 0) emptyTotalColor else defaultTotalColor

            binding.productInfoTotal.apply {
                text = orderTotal
                setTextColor(totalColor)
            }

            val productPrice = formatCurrencyForDisplay(item.price)
            val attributes = item.attributesDescription
                .takeIf { it.isNotEmpty() }
                ?.let { "$it \u2981 " }
                ?: StringUtils.EMPTY
            binding.productInfoAttributes.text = itemView.resources.getString(
                R.string.orderdetail_product_lineitem_attributes,
                attributes, item.quantity.formatToString(), productPrice
            )

            productImage?.let {
                val imageCornerRadius = itemView.resources.getDimensionPixelSize(R.dimen.corner_radius_image)
                val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
                Glide.with(binding.productInfoIcon)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
                    .into(binding.productInfoIcon)
            } ?: binding.productInfoIcon.setImageResource(R.drawable.ic_product)

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
