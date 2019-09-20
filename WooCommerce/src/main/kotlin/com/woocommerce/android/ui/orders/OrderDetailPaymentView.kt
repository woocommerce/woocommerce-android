package com.woocommerce.android.ui.orders

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.extensions.expandHitArea
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import kotlinx.android.synthetic.main.order_detail_payment_info.view.*
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal

class OrderDetailPaymentView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_payment_info, this)
        orientation = LinearLayout.VERTICAL
    }

    fun initView(
        order: Order,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        actionListener: OrderRefundActionListener
    ) {
        paymentInfo_productsTotal.text = formatCurrencyForDisplay(order.productsTotal)
        paymentInfo_shippingTotal.text = formatCurrencyForDisplay(order.shippingTotal)
        paymentInfo_taxesTotal.text = formatCurrencyForDisplay(order.totalTax)
        paymentInfo_total.text = formatCurrencyForDisplay(order.total)
        paymentInfo_lblTitle.text = context.getString(R.string.payment)

        if (order.paymentMethodTitle.isEmpty()) {
            paymentInfo_paymentMsg.hide()
            paymentInfo_total_paid_divider.hide()
            paymentInfo_paidSection.hide()
            paymentInfo_issueRefundButtonSection.hide()
        } else {
            paymentInfo_paymentMsg.show()
            paymentInfo_total_paid_divider.show()

            if (order.status == CoreOrderStatus.PENDING||
                    order.status == CoreOrderStatus.ON_HOLD ||
                    order.datePaid == null) {
                paymentInfo_paid.text = formatCurrencyForDisplay(BigDecimal.ZERO) // Waiting for payment
                paymentInfo_paymentMsg.text = context.getString(
                        R.string.orderdetail_payment_summary_onhold, order.paymentMethodTitle
                )

                // Can't refund if we haven't received the money yet
                paymentInfo_issueRefundButtonSection.hide()
            } else {
                paymentInfo_paid.text = formatCurrencyForDisplay(order.total)

                val dateStr = DateFormat.getMediumDateFormat(context).format(order.datePaid)
                paymentInfo_paymentMsg.text = context.getString(
                        R.string.orderdetail_payment_summary_completed,
                        dateStr,
                        order.paymentMethodTitle
                )

                if (order.total - order.refundTotal > BigDecimal.ZERO) {
                    paymentInfo_issueRefundButtonSection.show()
                } else {
                    paymentInfo_issueRefundButtonSection.hide()
                }
            }
        }

        // Populate or hide refund section
        if (order.refundTotal > BigDecimal.ZERO) {
            paymentInfo_refundSection.show()
            paymentInfo_refundTotal.text = formatCurrencyForDisplay(order.refundTotal)
            val newTotal = order.total - order.refundTotal
            paymentInfo_newTotal.text = formatCurrencyForDisplay(newTotal)
        } else {
            paymentInfo_refundSection.hide()
        }

        // Populate or hide discounts section
        if (order.discountTotal.isEqualTo(BigDecimal.ZERO)) {
            paymentInfo_discountSection.hide()
        } else {
            paymentInfo_discountSection.show()
            paymentInfo_discountTotal.text = formatCurrencyForDisplay(order.discountTotal)
            paymentInfo_discountItems.text = context.getString(
                    R.string.orderdetail_discount_items,
                    order.discountCodes
            )
        }

        paymentInfo_issueRefundButton.expandHitArea(100, 100)
        paymentInfo_issueRefundButton.setOnClickListener {
            actionListener.issueOrderRefund(order)
        }
    }
}
