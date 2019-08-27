package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.DateUtils
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

    fun initView(order: WCOrderModel, formatCurrencyForDisplay: (String) -> String) {
        paymentInfo_productsTotal.text = formatCurrencyForDisplay(order.getOrderSubtotal().toString())
        paymentInfo_shippingTotal.text = formatCurrencyForDisplay(order.shippingTotal)
        paymentInfo_taxesTotal.text = formatCurrencyForDisplay(order.totalTax)
        paymentInfo_total.text = formatCurrencyForDisplay(order.total)
        paymentInfo_lblTitle.text = context.getString(R.string.payment)

        if (order.paymentMethodTitle.isEmpty()) {
            paymentInfo_paymentMsg.visibility = View.GONE
            paymentInfo_total_paid_divider.visibility = View.GONE
        } else {
            paymentInfo_paymentMsg.visibility = View.VISIBLE
            paymentInfo_total_paid_divider.visibility = View.VISIBLE

            if (order.status == CoreOrderStatus.PENDING.value ||
                    order.status == CoreOrderStatus.ON_HOLD.value ||
                    order.datePaid.isEmpty()) {
                paymentInfo_paid.text = formatCurrencyForDisplay("0") // Waiting for payment
                paymentInfo_paymentMsg.text = context.getString(
                        R.string.orderdetail_payment_summary_onhold, order.paymentMethodTitle
                )
            } else {
                paymentInfo_paid.text = formatCurrencyForDisplay(order.total)

                val dateStr = DateUtils.getMediumDateFromString(context, order.datePaid)
                paymentInfo_paymentMsg.text = context.getString(
                        R.string.orderdetail_payment_summary_completed,
                        dateStr,
                        order.paymentMethodTitle
                )
            }
        }

        // Populate or hide refund section
        if (order.refundTotal.absoluteValue > 0) {
            paymentInfo_refundSection.visibility = View.VISIBLE
            paymentInfo_refundTotal.text = formatCurrencyForDisplay(order.refundTotal.toString())
            val newTotal = order.total.toDouble() + order.refundTotal
            paymentInfo_newTotal.text = formatCurrencyForDisplay(newTotal.toString())
        } else {
            paymentInfo_refundSection.visibility = View.GONE
        }

        // Populate or hide discounts section
        val discountCheck = order.discountTotal.toDoubleOrNull()
        if (discountCheck == null || discountCheck.compareTo(0) == 0) {
            paymentInfo_discountSection.visibility = View.GONE
        } else {
            paymentInfo_discountSection.visibility = View.VISIBLE
            paymentInfo_discountTotal.text = formatCurrencyForDisplay(order.discountTotal)
            paymentInfo_discountItems.text = context.getString(
                    R.string.orderdetail_discount_items,
                    order.discountCodes
            )
        }
    }
}
