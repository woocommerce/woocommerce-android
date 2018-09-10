package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyUtils
import kotlinx.android.synthetic.main.order_detail_payment_info.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import kotlin.math.absoluteValue

class OrderDetailPaymentView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_payment_info, this)
        orientation = LinearLayout.VERTICAL
    }

    interface OrderDetailPaymentViewListener {
        fun onRequestPaymentCleared()
    }

    fun initView(order: WCOrderModel, listener: OrderDetailPaymentViewListener) {
        val currencyCode = order.currency

        paymentInfo_subTotal.text = CurrencyUtils.currencyString(context, order.getOrderSubtotal(), currencyCode)
        paymentInfo_shippingTotal.text = CurrencyUtils.currencyString(
                context, order.shippingTotal.toDouble(), currencyCode)
        paymentInfo_taxesTotal.text = CurrencyUtils.currencyString(context, order.totalTax, currencyCode)
        paymentInfo_total.text = CurrencyUtils.currencyString(context, order.total, currencyCode)

        when (order.status) {
            CoreOrderStatus.COMPLETED.value -> {
                paymentInfo_paymentMsg.visibility = View.VISIBLE
                paymentInfo_btnCleared.visibility = View.GONE
                val totalPayment = CurrencyUtils.currencyString(context, order.total, currencyCode)
                paymentInfo_paymentMsg.text = context.getString(
                        R.string.orderdetail_payment_summary_completed, totalPayment, order.paymentMethodTitle)
            }
            CoreOrderStatus.ON_HOLD.value -> {
                paymentInfo_paymentMsg.visibility = View.VISIBLE
                paymentInfo_paymentMsg.text = context.getString(
                        R.string.orderdetail_payment_summary_onhold, order.paymentMethodTitle)
                paymentInfo_btnCleared.visibility = View.VISIBLE
                paymentInfo_btnCleared.setOnClickListener {
                    listener.onRequestPaymentCleared()
                }
            }
            CoreOrderStatus.PROCESSING.value -> {
                paymentInfo_paymentMsg.visibility = View.GONE
                paymentInfo_divider2.visibility = View.GONE
                paymentInfo_btnCleared.visibility = View.GONE
            }
        }

        // Populate or hide refund section
        if (order.refundTotal.absoluteValue > 0) {
            paymentInfo_lblTitle.text = context.getString(R.string.orderdetail_payment_refunded)
            paymentInfo_refundSection.visibility = View.VISIBLE
            paymentInfo_refundTotal.text = CurrencyUtils.currencyString(context, order.refundTotal, currencyCode)
            val newTotal = order.total.toDouble() + order.refundTotal
            paymentInfo_newTotal.text = CurrencyUtils.currencyString(context, newTotal, currencyCode)
        } else {
            paymentInfo_lblTitle.text = context.getString(R.string.payment)
            paymentInfo_refundSection.visibility = View.GONE
        }

        // Populate or hide discounts section
        val discountCheck = order.discountTotal.toDoubleOrNull()
        if (discountCheck == null || discountCheck.compareTo(0) == 0) {
            paymentInfo_discountSection.visibility = View.GONE
        } else {
            paymentInfo_discountSection.visibility = View.VISIBLE
            paymentInfo_discountTotal.text = CurrencyUtils.currencyString(context, order.discountTotal, currencyCode)
            paymentInfo_discountItems.text = context.getString(R.string.orderdetail_discount_items, order.discountCodes)
        }
    }
}
