package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.StringUtils
import kotlinx.android.synthetic.main.order_detail_product_item.view.*
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class OrderDetailProductItemView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_product_item, this)
    }

    fun initView(
        item: Order.Item,
        productImage: String?,
        expanded: Boolean,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        productInfo_name.text = item.name

        val orderTotal = formatCurrencyForDisplay(item.total)
        productInfo_total.text = orderTotal

        // Modify views for expanded or collapsed mode
        productInfo_totalTax.isVisible = expanded
        productInfo_lblTax.isVisible = expanded

        val maxLinesInName = if (expanded) Int.MAX_VALUE else 2
        productInfo_name.maxLines = maxLinesInName

        if (item.sku.isEmpty()) {
            productInfo_sku.visibility = View.GONE
        } else {
            productInfo_sku.visibility = View.VISIBLE
            productInfo_sku.text = context.getString(R.string.orderdetail_product_lineitem_sku_value, item.sku)
        }

        val productPrice = formatCurrencyForDisplay(item.price)
        val attributes = item.attributesList.takeIf { it.isNotEmpty() }?.let { "$it \u25CF " } ?: StringUtils.EMPTY

        productInfo_attributes.text = context.getString(
            R.string.orderdetail_product_lineitem_attributes,
            attributes, item.quantity.toString(), productPrice
        )

        if (expanded) {
            productInfo_totalTax.text = formatCurrencyForDisplay(item.totalTax)
        }

        productImage?.let {
            val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
            GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(productInfo_icon)
        } ?: productInfo_icon.setImageResource(R.drawable.ic_product)
    }
}
