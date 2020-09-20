package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.getMediumDate
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailRefundsAdapter
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
        formatCurrencyForDisplay: (BigDecimal) -> String,
        onIssueRefundClickListener: ((view: View) -> Unit)
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

        // Populate or hide discounts section
        if (order.discountTotal isEqualTo BigDecimal.ZERO) {
            paymentInfo_discountSection.hide()
        } else {
            paymentInfo_discountSection.show()
            paymentInfo_discountTotal.text = context.getString(
                R.string.orderdetail_customer_note,
                formatCurrencyForDisplay(order.discountTotal)
            )
            paymentInfo_discountItems.text = context.getString(
                R.string.orderdetail_discount_items,
                order.discountCodes
            )
        }

        // Populate or hide refund section
        if (order.refundTotal > BigDecimal.ZERO) {
            paymentInfo_refundSection.show()
            val newTotal = order.total - order.refundTotal
            paymentInfo_newTotal.text = formatCurrencyForDisplay(newTotal)
        } else {
            paymentInfo_refundSection.hide()
        }

        paymentInfo_issueRefundButton.setOnClickListener(onIssueRefundClickListener)
    }

    fun showRefunds(
        order: Order,
        refunds: List<Refund>,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        val adapter = paymentInfo_refunds.adapter as? OrderDetailRefundsAdapter
            ?: OrderDetailRefundsAdapter(order.isCashPayment, order.paymentMethodTitle, formatCurrencyForDisplay)
        paymentInfo_refunds.adapter = adapter
        adapter.refundList = refunds

        paymentInfo_refunds.show()
        paymentInfo_refundTotalSection.hide()

        var availableRefundQuantity = order.availableRefundQuantity
        refunds.flatMap { it.items }.groupBy { it.uniqueId }.forEach { productRefunds ->
            val refundedCount = productRefunds.value.sumBy { it.quantity }
            availableRefundQuantity -= refundedCount
        }

        // TODO: Once the refund by amount is supported again, this condition will need to be updated
        paymentInfo_issueRefundButtonSection.isVisible = availableRefundQuantity > 0 && order.isRefundAvailable
    }

    fun showRefundTotal(
        show: Boolean,
        refundTotal: BigDecimal,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        paymentInfo_refundTotal.text = formatCurrencyForDisplay(refundTotal)
        paymentInfo_refunds.hide()
        paymentInfo_refundTotalSection.show()
        paymentInfo_issueRefundButtonSection.isVisible = show
    }
}
