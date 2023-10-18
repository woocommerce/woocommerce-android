package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductItemViewBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.orders.creation.product.discount.CalculateItemDiscountAmount
import com.woocommerce.android.ui.orders.creation.product.discount.GetItemDiscountAmountText
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.getStockText
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

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
    private val imageCornerRadius = context.resources.getDimensionPixelSize(R.dimen.corner_radius_image)
    private val bullet = "\u2022"
    private val statusColor = ContextCompat.getColor(context, R.color.product_status_fg_other)
    private val statusPendingColor = ContextCompat.getColor(context, R.color.product_status_fg_pending)

    fun bind(
        product: Product,
        currencyFormatter: CurrencyFormatter,
        currencyCode: String? = null,
        isActivated: Boolean = false
    ) {
        showProductName(product.name)
        showProductSku(product.sku)
        showProductImage(product.firstImageUrl, isActivated)
        showProductStockStatusPrice(product, currencyFormatter, currencyCode)
    }

    fun bind(
        orderCreationProduct: OrderCreationProduct,
        currencyFormatter: CurrencyFormatter,
        currencyCode: String? = null,
        showDiscount: Boolean = false,
    ) {
        showProductName(orderCreationProduct.item.name)
        showProductSku(orderCreationProduct.item.sku)
        showProductImage(orderCreationProduct.productInfo.imageUrl)
        val discountAmount = CalculateItemDiscountAmount()(orderCreationProduct.item)
        if (showDiscount && currencyCode != null && discountAmount > BigDecimal.ZERO) {
            binding.productDiscount.isVisible = true
            binding.productDiscount.text =
                context.getString(
                    R.string.order_creation_discount_value,
                    GetItemDiscountAmountText(currencyFormatter)(discountAmount, currencyCode)
                )
        } else {
            binding.productDiscount.isVisible = false
        }

        binding.productStockAndStatus.text = buildString {
            if (orderCreationProduct.item.isVariation && orderCreationProduct.item.attributesDescription.isNotEmpty()) {
                append(orderCreationProduct.item.attributesDescription)
            } else {
                append(orderCreationProduct.getStockText(context))
            }
            append(" $bullet ")
            val decimalFormatter = getDecimalFormatter(currencyFormatter, currencyCode)
            append(decimalFormatter(orderCreationProduct.item.total).replace(" ", "\u00A0"))
        }
    }

    private fun showProductName(productName: String) {
        binding.productName.text = if (productName.isEmpty()) {
            context.getString(R.string.untitled)
        } else {
            HtmlUtils.fastStripHtml(productName)
        }
    }

    private fun showProductSku(sku: String) {
        with(binding.productSku) {
            if (sku.isNotEmpty()) {
                visibility = View.VISIBLE
                text = context.getString(R.string.orderdetail_product_lineitem_sku_value, sku)
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun showProductImage(
        imageUrl: String?,
        isActivated: Boolean = false
    ) {
        val size: Int
        when {
            imageUrl.isNullOrEmpty() -> {
                size = imageSize / 2
                binding.productImage.setImageResource(R.drawable.ic_product)
            }
            else -> {
                size = imageSize
                val photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, imageSize)
                GlideApp.with(context)
                    .load(photonUrl)
                    .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
                    .placeholder(R.drawable.ic_product)
                    .into(binding.productImage)
            }
        }
        binding.productImageSelected.visibility = if (isActivated) View.VISIBLE else View.GONE
        binding.productImage.layoutParams.apply {
            height = size
            width = size
        }
    }

    private fun getDecimalFormatter(
        currencyFormatter: CurrencyFormatter,
        currencyCode: String? = null
    ): (BigDecimal) -> String {
        return currencyCode?.let {
            currencyFormatter.buildBigDecimalFormatter(it)
        } ?: currencyFormatter.buildBigDecimalFormatter()
    }

    private fun showProductStockStatusPrice(
        product: Product,
        currencyFormatter: CurrencyFormatter,
        currencyCode: String? = null
    ) {
        val decimalFormatter = getDecimalFormatter(currencyFormatter, currencyCode)

        val statusHtml = getProductStatusHtml(product.status)
        val stock = product.getStockText(context)
        val stockAndStatus = if (statusHtml != null) "$statusHtml $bullet $stock" else stock
        val stockStatusPrice = if (product.price != null) {
            val fmtPrice = decimalFormatter(product.price)
            "$stockAndStatus $bullet $fmtPrice"
        } else {
            stockAndStatus
        }

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
    }

    private fun getProductStatusHtml(productStatus: ProductStatus?): String? {
        return productStatus?.let {
            when {
                it == ProductStatus.PENDING -> {
                    "<font color=$statusPendingColor>${productStatus.toLocalizedString(context)}</font>"
                }
                it != ProductStatus.PUBLISH -> {
                    "<font color=$statusColor>${productStatus.toLocalizedString(context)}</font>"
                }
                else -> {
                    null
                }
            }
        }
    }
}
