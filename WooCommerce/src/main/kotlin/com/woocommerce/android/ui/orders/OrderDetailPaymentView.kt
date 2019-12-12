package com.woocommerce.android.ui.orders

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import kotlinx.android.synthetic.main.order_detail_payment_info.view.*
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal

class OrderDetailPaymentView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    private lateinit var formatCurrency: (BigDecimal) -> String
    private lateinit var actionListener: OrderRefundActionListener
    private lateinit var order: Order

    init {
        View.inflate(context, R.layout.order_detail_payment_info, this)
        orientation = VERTICAL
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

        if (order.paymentMethodTitle.isEmpty()) {
            paymentInfo_paymentMsg.hide()
            paymentInfo_total_paid_divider.hide()
            paymentInfo_paidSection.hide()
            paymentInfo_issueRefundButtonSection.hide()
        } else {
            paymentInfo_paymentMsg.show()
            paymentInfo_total_paid_divider.show()

            if (order.status == CoreOrderStatus.PENDING ||
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
        refunds.flatMap { it.items }.groupBy { it.productId }.forEach { productRefunds ->
            val refundedCount = productRefunds.value.sumBy { it.quantity }
            availableRefundQuantity -= refundedCount
        }

        if (availableRefundQuantity > 0) {
            paymentInfo_issueRefundButtonSection.show()
        } else {
            paymentInfo_issueRefundButtonSection.hide()
        }
    }

    fun showRefundTotal(refundTotal: BigDecimal) {
        paymentInfo_refundTotal.text = formatCurrency(refundTotal)

        paymentInfo_refunds.hide()
        paymentInfo_refundTotalSection.show()
    }
}
