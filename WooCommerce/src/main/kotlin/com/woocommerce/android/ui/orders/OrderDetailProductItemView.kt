package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailProductItemBinding
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ProductImageLoader
import com.woocommerce.android.ui.products.ProductImageViewTarget
import java.math.BigDecimal

typealias ViewAddonClickListener = (Order.Item) -> Unit

class OrderDetailProductItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailProductItemBinding.inflate(LayoutInflater.from(ctx), this, true)

    private var imageTarget: ProductImageViewTarget? = null

    fun initView(
        item: Order.Item,
        productImageLoaderFactory: ProductImageLoader.Factory,
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

        val target = imageTarget ?: ProductImageViewTarget(
            binding.productInfoIcon, productImageLoaderFactory
        ).also { imageTarget = it }
        target.load(item.uniqueId)
    }

    fun hideProductTotal() {
        binding.productInfoTotal.isVisible = false
    }
}
