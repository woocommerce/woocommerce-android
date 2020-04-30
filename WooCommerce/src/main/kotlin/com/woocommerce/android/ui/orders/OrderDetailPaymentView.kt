package com.woocommerce.android.ui.orders

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import kotlinx.android.synthetic.main.order_detail_payment_info.view.*
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal

class OrderDetailPaymentView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private lateinit var formatCurrency: (BigDecimal) -> String
    private lateinit var actionListener: OrderRefundActionListener
    private lateinit var order: Order

    init {
        View.inflate(context, R.layout.order_detail_payment_info, this)
    }

    @SuppressLint("SetTextI18n")
    fun initView(
        order: Order,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        actionListener: OrderRefundActionListener
    ) {
        this.formatCurrency = formatCurrencyForDisplay
        this.actionListener = actionListener
        this.order = order

        paymentInfo_productsTotal.text = formatCurrencyForDisplay(order.productsTotal)
        paymentInfo_shippingTotal.text = formatCurrencyForDisplay(order.shippingTotal)
        paymentInfo_taxesTotal.text = formatCurrencyForDisplay(order.totalTax)
        paymentInfo_total.text = formatCurrencyForDisplay(order.total)
        paymentInfo_lblTitle.text = context.getString(R.string.payment)

        paymentInfo_refunds.layoutManager = LinearLayoutManager(context)
        paymentInfo_refunds.setHasFixedSize(true)

        if (order.paymentMethodTitle.isEmpty() && order.datePaid == null) {
            paymentInfo_paymentMsg.hide()
            paymentInfo_paidSection.hide()
        } else {
            paymentInfo_paymentMsg.show()

            if (order.status == CoreOrderStatus.PENDING ||
                    order.status == CoreOrderStatus.ON_HOLD ||
                    order.datePaid == null) {
                paymentInfo_paid.text = formatCurrencyForDisplay(BigDecimal.ZERO) // Waiting for payment
                paymentInfo_paymentMsg.text = context.getString(
                        R.string.orderdetail_payment_summary_onhold, order.paymentMethodTitle
                )
            } else {
                paymentInfo_paid.text = formatCurrencyForDisplay(order.total)

                val dateStr = DateFormat.getMediumDateFormat(context).format(order.datePaid)
                if (order.paymentMethodTitle.isNotEmpty()) {
                    paymentInfo_paymentMsg.text = context.getString(
                        R.string.orderdetail_payment_summary_completed,
                        dateStr,
                        order.paymentMethodTitle
                    )
                } else {
                    paymentInfo_paymentMsg.text = dateStr
                }
            }
        }

        // Populate or hide refund section
        if (order.refundTotal > BigDecimal.ZERO) {
            paymentInfo_refundSection.show()
            val newTotal = order.total - order.refundTotal
            paymentInfo_newTotal.text = formatCurrencyForDisplay(newTotal)
        } else {
            paymentInfo_refundSection.hide()
        }

        // Populate or hide discounts section
        if (order.discountTotal isEqualTo BigDecimal.ZERO) {
            paymentInfo_discountSection.hide()
        } else {
            paymentInfo_discountSection.show()
            paymentInfo_discountTotal.text = "-${formatCurrencyForDisplay(order.discountTotal)}"
            paymentInfo_discountItems.text = context.getString(
                    R.string.orderdetail_discount_items,
                    order.discountCodes
            )
        }

        paymentInfo_issueRefundButton.setOnClickListener {
            actionListener.issueOrderRefund(order)
        }
    }

    fun showRefunds(refunds: List<Refund>) {
        var adapter = paymentInfo_refunds.adapter as? OrderDetailRefundListAdapter
        if (adapter == null) {
            adapter = OrderDetailRefundListAdapter(
                    formatCurrency,
                    { orderId, refundId -> actionListener.showRefundDetail(orderId, refundId) },
                    order
            )
            paymentInfo_refunds.adapter = adapter
        }
        adapter.update(refunds)

        paymentInfo_refunds.show()
        paymentInfo_refundTotalSection.hide()

        var availableRefundQuantity = order.items.sumBy { it.quantity }
        refunds.flatMap { it.items }.groupBy { it.uniqueId }.forEach { productRefunds ->
            val refundedCount = productRefunds.value.sumBy { it.quantity }
            availableRefundQuantity -= refundedCount
        }

        // TODO: Once the refund by amount is supported again, this condition will need to be updated
        if (availableRefundQuantity > 0 && order.refundTotal < order.total) {
            paymentInfo_issueRefundButtonSection.show()
        } else {
            paymentInfo_issueRefundButtonSection.hide()
        }
    }

    fun showRefundTotal() {
        paymentInfo_refundTotal.text = formatCurrency(order.refundTotal)

        paymentInfo_refunds.hide()
        paymentInfo_refundTotalSection.show()

        if (order.refundTotal < order.total) {
            paymentInfo_issueRefundButtonSection.show()
        } else {
            paymentInfo_issueRefundButtonSection.hide()
        }
    }
}
