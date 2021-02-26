package com.woocommerce.android.ui.products

import android.content.Context
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.OnBackorder
import com.woocommerce.android.ui.products.ProductStockStatus.OutOfStock
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils

class ProductItemViewHolder(val viewBinding: ProductListItemBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {
    private val context = viewBinding.root.context
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private val bullet = "\u2022"
    private val statusColor = ContextCompat.getColor(context, R.color.product_status_fg_other)
    private val statusPendingColor = ContextCompat.getColor(context, R.color.product_status_fg_pending)
    private val selectedBackgroundColor = ContextCompat.getColor(context, R.color.color_primary)
    private val unSelectedBackgroundColor = ContextCompat.getColor(context, R.color.white)

    fun bind(
        product: Product,
        isActivated: Boolean = false
    ) {
        viewBinding.root.isActivated = isActivated

        viewBinding.productName.text = if (product.name.isEmpty()) {
            context.getString(R.string.untitled)
        } else {
            HtmlUtils.fastStripHtml(product.name)
        }

        val stockAndStatus = getProductStockStatusText(context, product)
        with(viewBinding.productStockAndStatus) {
            if (stockAndStatus != null) {
                visibility = View.VISIBLE
                text = HtmlCompat.fromHtml(
                    stockAndStatus,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            } else {
                visibility = View.GONE
            }
        }

        val firstImage = product.firstImageUrl
        val size: Int
        when {
            itemView.isActivated -> {
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

    fun setOnDeleteClickListener(
        product: Product,
        onItemDeleted: (product: Product) -> Unit
    ) {
        with(viewBinding.productBtnDelete) {
            isVisible = true
            setOnClickListener { onItemDeleted.invoke(product) }
        }
    }

    private fun getProductStockStatusText(
        context: Context,
        product: Product
    ): String? {
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

        val stock = when (product.stockStatus) {
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
                            FormatUtils.formatInt(product.stockQuantity)
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

        return if (statusHtml != null) "$statusHtml $bullet $stock" else stock
    }

    /**
     * Method to return details associated with a user selection
     */
    fun getItemDetails() =
        object : ItemDetailsLookup.ItemDetails<Long>() {
            override fun getPosition() = adapterPosition
            override fun getSelectionKey() = itemId
            override fun inSelectionHotspot(e: MotionEvent) = true
        }
}
