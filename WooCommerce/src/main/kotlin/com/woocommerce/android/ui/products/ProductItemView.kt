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
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.ProductUIModel
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.OnBackorder
import com.woocommerce.android.ui.products.ProductStockStatus.OutOfStock
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils
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
    private val bullet = "\u2022"
    private val statusColor = ContextCompat.getColor(context, R.color.product_status_fg_other)
    private val statusPendingColor = ContextCompat.getColor(context, R.color.product_status_fg_pending)
    private val selectedBackgroundColor = ContextCompat.getColor(context, R.color.color_primary)
    private val unSelectedBackgroundColor = ContextCompat.getColor(context, R.color.white)

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
        productUIModel: ProductUIModel,
        currencyFormatter: CurrencyFormatter,
        currencyCode: String? = null
    ) {
        showProductName(productUIModel.item.name)
        showProductSku(productUIModel.item.sku)
        showProductImage(productUIModel.imageUrl)
        val decimalFormatter = getDecimalFormatter(currencyFormatter, currencyCode)

        binding.productStockAndStatus.text = buildString {
            if (productUIModel.item.isVariation && productUIModel.item.attributesDescription.isNotEmpty()) {
                append(productUIModel.item.attributesDescription)
            } else {
                if (productUIModel.isStockManaged && productUIModel.stockStatus == InStock) {
                    append(
                        context.getString(
                            R.string.order_creation_product_stock_quantity,
                            productUIModel.stockQuantity.formatToString()
                        )
                    )
                } else {
                    append(context.getString(productUIModel.stockStatus.stringResource))
                }
            }
            append(" $bullet ")
            append(decimalFormatter(productUIModel.item.total).replace(" ", "\u00A0"))
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
            isActivated -> {
                size = imageSize / 2
                binding.productImage.setImageResource(R.drawable.ic_menu_action_mode_check)
                binding.productImageFrame.setBackgroundColor(selectedBackgroundColor)
            }
            imageUrl.isNullOrEmpty() -> {
                size = imageSize / 2
                binding.productImageFrame.setBackgroundColor(unSelectedBackgroundColor)
                binding.productImage.setImageResource(R.drawable.ic_product)
            }
            else -> {
                size = imageSize
                binding.productImageFrame.setBackgroundColor(unSelectedBackgroundColor)
                val photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, imageSize)
                GlideApp.with(context)
                    .load(photonUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(binding.productImage)
            }
        }

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
        val stock = getStockText(product)
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
