package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyUtils
import kotlinx.android.synthetic.main.order_detail_product_item.view.*
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailProductItemView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_product_item, this)
    }

    fun initView(item: WCOrderModel.LineItem, currencyCode: String) {
        productInfo_name.text = item.name
        productInfo_qty.text = item.quantity.toString()

        if (item.sku.isNullOrEmpty()) {
            productInfo_lblSku.visibility = View.GONE
            productInfo_sku.visibility = View.GONE
        } else {
            productInfo_lblSku.visibility = View.VISIBLE
            productInfo_sku.visibility = View.VISIBLE
            productInfo_sku.text = item.sku
        }

        // Populate formatted total and tax values
        val res = context.resources
        val orderTotal = CurrencyUtils.currencyString(context, item.total, currencyCode)
        val productPrice = CurrencyUtils.currencyString(context, item.price, currencyCode)
        productInfo_productTotal.text = res.getString(
                R.string.orderdetail_product_lineitem_total, orderTotal, productPrice, item.quantity)
        productInfo_totalTax.text = CurrencyUtils.currencyString(context, item.totalTax, currencyCode)

        // todo Product Image
    }
}
