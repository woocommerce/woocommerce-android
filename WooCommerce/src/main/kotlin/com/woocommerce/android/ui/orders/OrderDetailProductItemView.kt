package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import kotlinx.android.synthetic.main.order_detail_product_item.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils
import java.text.NumberFormat

class OrderDetailProductItemView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    private var imageSize = 0

    init {
        val dimen = context.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
        imageSize = DisplayUtils.dpToPx(context, dimen)
        View.inflate(context, R.layout.order_detail_product_item, this)
    }

    fun initView(
        item: WCOrderModel.LineItem,
        productImage: String?,
        expanded: Boolean,
        formatCurrencyForDisplay: (String?) -> String
    ) {
        productInfo_name.text = item.name

        val numberFormatter = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 2
        }
        productInfo_qty.text = numberFormatter.format(item.quantity)

        // Modify views for expanded or collapsed mode
        val viewMode = if (expanded) View.VISIBLE else View.GONE
        productInfo_icon.visibility = viewMode
        productInfo_productTotal.visibility = viewMode
        productInfo_totalTax.visibility = viewMode
        productInfo_lblTax.visibility = viewMode
        productInfo_name.setSingleLine(!expanded)

        if (item.sku.isNullOrEmpty() || !expanded) {
            productInfo_lblSku.visibility = View.GONE
            productInfo_sku.visibility = View.GONE
        } else {
            productInfo_lblSku.visibility = View.VISIBLE
            productInfo_sku.visibility = View.VISIBLE
            productInfo_sku.text = item.sku
        }

        if (expanded) {
            // Populate formatted total and tax values
            val res = context.resources
            val orderTotal = formatCurrencyForDisplay(item.total)
            val productPrice = formatCurrencyForDisplay(item.price)

            item.quantity?.takeIf { it > 1 }?.let {
                val itemQty = numberFormatter.format(it)
                productInfo_productTotal.text = res.getString(
                        R.string.orderdetail_product_lineitem_total_multiple, orderTotal, productPrice, itemQty
                )
            } ?: run {
                productInfo_productTotal.text = res.getString(
                        R.string.orderdetail_product_lineitem_total_single, orderTotal
                )
            }

            productInfo_totalTax.text = formatCurrencyForDisplay(item.totalTax)

            item.productId?.let { productId ->
                productImage?.let {
                    val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
                    GlideApp.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_product)
                            .into(productInfo_icon)
                } ?: productInfo_icon.setImageResource(R.drawable.ic_product)
            }
        }

        // we need to put some space before the product name when the product image is showing
        val margin: Int = if (expanded) {
            context.resources.getDimensionPixelSize(R.dimen.margin_large)
        } else {
            0
        }
        (productInfo_name.layoutParams as MarginLayoutParams).marginStart = margin
    }
}
