package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.order_detail_payment_info.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import java.util.Currency

class OrderDetailPaymentView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_payment_info, this)
    }

    fun initView(order: WCOrderModel) {
        var currencySymbol = ""
        try {
            currencySymbol = Currency.getInstance(order.currency).symbol
        } catch (e: IllegalArgumentException) {
            Log.e(OrderListAdapter.TAG, "Error finding valid currency symbol for currency code [${order.currency}]", e)
        }

        // todo - if price includes tax, hide tax section

        paymentInfo_subTotal.text = context.getString(
                R.string.currency_total, currencySymbol, order.getOrderSubtotal().toFloat())
        paymentInfo_shippingTotal.text = context.getString(
                R.string.currency_total, currencySymbol, order.shippingTotal.toFloat())
        paymentInfo_taxesTotal.text = context.getString(
                R.string.currency_total, currencySymbol, order.totalTax.toFloat())
        paymentInfo_total.text = context.getString(R.string.currency_total, currencySymbol, order.total.toFloat())

        paymentInfo_paymentMsg.text = context.getString(
                R.string.orderdetail_payment_summary,
                context.getString(R.string.currency_total, currencySymbol, order.total.toFloat()), order.paymentMethodTitle)

        // todo process refund type
        //      - update title
        //      - show refund and new total sections

        // Populate discounts.
        // - If no discounts, hide section
        if (order.discountTotal == "0.00") {
            paymentInfo_discountSection.visibility = View.GONE
        } else {
            paymentInfo_discountSection.visibility = View.VISIBLE
            paymentInfo_discountTotal.text = context.getString(
                    R.string.currency_total_negative, currencySymbol, order.discountTotal.toFloat())
            paymentInfo_discountItems.text = context.getString(R.string.orderdetail_discount_items, order.discountCodes)
        }
    }
}
