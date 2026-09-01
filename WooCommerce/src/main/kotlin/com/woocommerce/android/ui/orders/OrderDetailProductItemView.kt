package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailProductItemBinding
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.loadPhotonUrlWithFallback
import com.woocommerce.android.model.Order
import java.math.BigDecimal

typealias ViewAddonClickListener = (Order.Item) -> Unit

class OrderDetailProductItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailProductItemBinding.inflate(LayoutInflater.from(ctx), this, true)

    fun initView(
        item: Order.Item,
        productImage: String?,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        onViewAddonsClick: ViewAddonClickListener?
    ) {
        binding.productInfoName.text = item.name

        val orderTotal = formatCurrencyForDisplay(item.total)
        binding.productInfoTotal.text = orderTotal

        val productPrice = formatCurrencyForDisplay(item.price)
        with(binding.productInfoAttributes) {
            val attributes = item.displayableAttributes
            isVisible = attributes.isNotEmpty()
            text = attributes.joinToString(separator = "\n") {
                context.getString(R.string.orderdetail_product_lineitem_attribute, it.key, it.value)
            }
        }
        binding.productInfoQuantityAndPrice.text = context.getString(
            R.string.orderdetail_product_lineitem_quantity_and_price,
            item.quantity.formatToString(), productPrice
        )

        with(binding.productInfoSKU) {
            isVisible = item.sku.isNotEmpty()
            val productSku = context.getString(R.string.orderdetail_product_lineitem_sku_value, item.sku)
            binding.productInfoSKU.text = productSku
        }

        onViewAddonsClick?.let { onClick ->
            binding.productInfoAddons.visibility =
                if (item.containsAddons && AppPrefs.isProductAddonsEnabled) {
                    VISIBLE
                } else {
                    GONE
                }
            binding.productInfoAddons.setOnClickListener { onClick(item) }
        } ?: binding.productInfoAddons.let { it.visibility = GONE }

        productImage?.let {
            val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
            val imageCornerRadius = context.resources.getDimensionPixelSize(R.dimen.corner_radius_image)
            Glide.with(context)
                .loadPhotonUrlWithFallback(it, imageSize, imageSize)
                .placeholder(R.drawable.ic_product)
                .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
                .into(binding.productInfoIcon)
        } ?: binding.productInfoIcon.setImageResource(R.drawable.ic_product)
    }

    fun hideProductTotal() {
        binding.productInfoTotal.isVisible = false
    }
}
