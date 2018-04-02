package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.order_detail_product_item.view.*
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailProductItemView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_product_item, this)
    }

    fun initView(item: WCOrderModel.LineItem, currencySymbol: String) {
        productInfo_name.text = item.name
        productInfo_sku.text = item.sku
        productInfo_qty.text = item.quantity.toString()

        // Populate formatted total and tax values
        val res = context.resources
        val orderTotal = res.getString(R.string.currency_total, currencySymbol, item.total?.toFloat())
        val productPrice = res.getString(R.string.currency_total, currencySymbol, item.price?.toFloat())
        productInfo_productTotal.text = res.getString(
                R.string.orderdetail_product_lineitem_total, orderTotal, productPrice, item.quantity)
        productInfo_totalTax.text = res.getString(R.string.currency_total, currencySymbol, item.totalTax?.toFloat())

        // todo Product Image
    }
}
