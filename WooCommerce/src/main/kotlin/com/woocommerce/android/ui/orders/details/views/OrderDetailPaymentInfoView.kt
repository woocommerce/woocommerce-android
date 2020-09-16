package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.getMediumDate
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import kotlinx.android.synthetic.main.order_detail_payment_info.view.*
import java.math.BigDecimal

class OrderDetailPaymentInfoView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_payment_info, this)
    }

    fun updatePaymentInfo(
        order: Order,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        paymentInfo_productsTotal.text = formatCurrencyForDisplay(order.productsTotal)
        paymentInfo_shippingTotal.text = formatCurrencyForDisplay(order.shippingTotal)
        paymentInfo_taxesTotal.text = formatCurrencyForDisplay(order.totalTax)
        paymentInfo_total.text = formatCurrencyForDisplay(order.total)
        paymentInfo_lblTitle.text = context.getString(R.string.payment)

        with(paymentInfo_refunds) {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        if (order.isOrderPaid) {
            paymentInfo_paymentMsg.hide()
            paymentInfo_paidSection.hide()
        } else {
            paymentInfo_paymentMsg.show()

            if (order.isAwaitingPayment) {
                paymentInfo_paid.text = formatCurrencyForDisplay(BigDecimal.ZERO) // Waiting for payment
                paymentInfo_paymentMsg.text = context.getString(
                    R.string.orderdetail_payment_summary_onhold, order.paymentMethodTitle
                )
            } else {
                paymentInfo_paid.text = formatCurrencyForDisplay(order.total)

                val dateStr = order.datePaid?.getMediumDate(context)
                paymentInfo_paymentMsg.text = if (order.paymentMethodTitle.isNotEmpty()) {
                    context.getString(
                        R.string.orderdetail_payment_summary_completed,
                        dateStr,
                        order.paymentMethodTitle
                    )
                } else dateStr
            }
        }
    }
}
