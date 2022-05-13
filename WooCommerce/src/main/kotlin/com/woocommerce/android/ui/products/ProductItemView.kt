package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductItemViewBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.OnBackorder
import com.woocommerce.android.ui.products.ProductStockStatus.OutOfStock
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils

/**
 * ProductItemView is a reusable view for showing basic product information.
 * We use this in multiple places to provide a consistent product view.
 */
class ProductItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    val binding = ProductItemViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private val bullet = "\u2022"
    private val statusColor = ContextCompat.getColor(context, R.color.product_status_fg_other)
    private val statusPendingColor = ContextCompat.getColor(context, R.color.product_status_fg_pending)
    private val selectedBackgroundColor = ContextCompat.getColor(context, R.color.color_primary)
    private val unSelectedBackgroundColor = ContextCompat.getColor(context, R.color.white)

    fun bind(
        product: Product,
        currencyFormatter: CurrencyFormatter,
        isActivated: Boolean = false
    ) {
        binding.productName.text = if (product.name.isEmpty()) {
            context.getString(R.string.untitled)
        } else {
            HtmlUtils.fastStripHtml(product.name)
        }

        val stockStatusPrice = getProductStockStatusPriceText(context, product, currencyFormatter)
        with(binding.productStockAndStatus) {
            if (stockStatusPrice.isNotEmpty()) {
                visibility = View.VISIBLE
                text = HtmlCompat.fromHtml(
                    stockStatusPrice,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            } else {
                visibility = View.GONE
            }
        }

        with(binding.productSku) {
            if (product.sku.isNotEmpty()) {
                visibility = View.VISIBLE
                text = context.getString(R.string.orderdetail_product_lineitem_sku_value, product.sku)
            } else {
                visibility = View.GONE
            }
        }

        showProductImage(product, binding, isActivated)
    }

    private fun showProductImage(
        product: Product,
        viewBinding: ProductItemViewBinding,
        isActivated: Boolean
    ) {
        val firstImage = product.firstImageUrl
        val size: Int
        when {
            isActivated -> {
                size = imageSize / 2
                viewBinding.productImage.setImageResource(R.drawable.ic_menu_action_mode_check)
                viewBinding.productImageFrame.setBackgroundColor(selectedBackgroundColor)
            }
            firstImage.isNullOrEmpty() -> {
                size = imageSize / 2
                viewBinding.productImageFrame.setBackgroundColor(unSelectedBackgroundColor)
                viewBinding.productImage.setImageResource(R.drawable.ic_product)
            }
            else -> {
                size = imageSize
                viewBinding.productImageFrame.setBackgroundColor(unSelectedBackgroundColor)
                val imageUrl = PhotonUtils.getPhotonImageUrl(firstImage, imageSize, imageSize)
                GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(viewBinding.productImage)
            }
        }

        viewBinding.productImage.layoutParams.apply {
            height = size
            width = size
        }
    }

    private fun getProductStockStatusPriceText(
        context: Context,
        product: Product,
        currencyFormatter: CurrencyFormatter
    ): String {
        val statusHtml = product.status?.let {
            when {
                it == ProductStatus.PENDING -> {
                    "<font color=$statusPendingColor>${product.status.toLocalizedString(context)}</font>"
                }
                it != ProductStatus.PUBLISH -> {
                    "<font color=$statusColor>${product.status.toLocalizedString(context)}</font>"
                }
                else -> {
                    null
                }
            }
        }

        val stock = getStockText(product)
        val stockAndStatus = if (statusHtml != null) "$statusHtml $bullet $stock" else stock

        return if (product.price != null) {
            val fmtPrice = currencyFormatter.formatCurrency(product.price)
            "$stockAndStatus $bullet $fmtPrice"
        } else {
            stockAndStatus
        }
    }

    private fun getStockText(product: Product): String {
        return when (product.stockStatus) {
            InStock -> {
                if (product.productType == VARIABLE) {
                    if (product.numVariations > 0) {
                        context.getString(
                            R.string.product_stock_status_instock_with_variations,
                            product.numVariations
                        )
                    } else {
                        context.getString(R.string.product_stock_status_instock)
                    }
                } else {
                    if (product.stockQuantity > 0) {
                        context.getString(
                            R.string.product_stock_count,
                            StringUtils.formatCountDecimal(product.stockQuantity)
                        )
                    } else {
                        context.getString(R.string.product_stock_status_instock)
                    }
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
}
