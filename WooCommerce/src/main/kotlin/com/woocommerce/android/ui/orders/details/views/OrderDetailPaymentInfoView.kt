package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailPaymentInfoBinding
import com.woocommerce.android.extensions.getMediumDate
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailRefundsAdapter
import java.math.BigDecimal

class OrderDetailPaymentInfoView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailPaymentInfoBinding.inflate(LayoutInflater.from(ctx), this)

    fun updatePaymentInfo(
        order: Order,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        onIssueRefundClickListener: ((view: View) -> Unit)
    ) {
        binding.paymentInfoProductsTotal.text = formatCurrencyForDisplay(order.productsTotal)
        binding.paymentInfoShippingTotal.text = formatCurrencyForDisplay(order.shippingTotal)
        binding.paymentInfoTaxesTotal.text = formatCurrencyForDisplay(order.totalTax)
        binding.paymentInfoTotal.text = formatCurrencyForDisplay(order.total)
        binding.paymentInfoLblTitle.text = context.getString(R.string.payment)

        with(binding.paymentInfoRefunds) {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }

        if (order.isOrderPaid) {
            binding.paymentInfoPaymentMsg.hide()
            binding.paymentInfoPaidSection.hide()
        } else {
            binding.paymentInfoPaymentMsg.show()

            if (order.isAwaitingPayment) {
                binding.paymentInfoPaid.text = formatCurrencyForDisplay(BigDecimal.ZERO)
                binding.paymentInfoPaymentMsg.text = context.getString(
                    R.string.orderdetail_payment_summary_onhold, order.paymentMethodTitle
                )
            } else {
                binding.paymentInfoPaid.text = formatCurrencyForDisplay(order.total)

                val dateStr = order.datePaid?.getMediumDate(context)
                binding.paymentInfoPaymentMsg.text = if (order.paymentMethodTitle.isNotEmpty()) {
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
            binding.paymentInfoDiscountSection.hide()
        } else {
            binding.paymentInfoDiscountSection.show()
            binding.paymentInfoDiscountTotal.text = context.getString(
                R.string.orderdetail_customer_note,
                formatCurrencyForDisplay(order.discountTotal)
            )
            binding.paymentInfoDiscountItems.text = context.getString(
                R.string.orderdetail_discount_items,
                order.discountCodes
            )
        }

        // Populate or hide the fees section
        if (order.feesTotal isEqualTo BigDecimal.ZERO) {
            binding.paymentInfoFeesSection.hide()
        } else {
            binding.paymentInfoFeesSection.show()
            binding.paymentInfoFees.text = formatCurrencyForDisplay(order.feesTotal)
        }

        // Populate or hide refund section
        if (order.refundTotal > BigDecimal.ZERO) {
            binding.paymentInfoRefundSection.show()
            val newTotal = order.total - order.refundTotal
            binding.paymentInfoNewTotal.text = formatCurrencyForDisplay(newTotal)
        } else {
            binding.paymentInfoRefundSection.hide()
        }

        binding.paymentInfoIssueRefundButton.setOnClickListener(onIssueRefundClickListener)
    }

    fun showRefunds(
        order: Order,
        refunds: List<Refund>,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        val adapter = binding.paymentInfoRefunds.adapter as? OrderDetailRefundsAdapter
            ?: OrderDetailRefundsAdapter(order.isCashPayment, order.paymentMethodTitle, formatCurrencyForDisplay)
        binding.paymentInfoRefunds.adapter = adapter
        adapter.refundList = refunds

        binding.paymentInfoRefunds.show()
        binding.paymentInfoRefundTotalSection.hide()

        var availableRefundQuantity = order.availableRefundQuantity
        refunds.flatMap { it.items }.groupBy { it.uniqueId }.forEach { productRefunds ->
            val refundedCount = productRefunds.value.sumBy { it.quantity }
            availableRefundQuantity -= refundedCount
        }

        // TODO: Once the refund by amount is supported again, this condition will need to be updated
        binding.paymentInfoIssueRefundButtonSection.isVisible = availableRefundQuantity > 0 && order.isRefundAvailable
    }

    fun showRefundTotal(
        show: Boolean,
        refundTotal: BigDecimal,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        binding.paymentInfoRefundTotal.text = formatCurrencyForDisplay(refundTotal)
        binding.paymentInfoRefunds.hide()
        binding.paymentInfoRefundTotalSection.show()
        binding.paymentInfoIssueRefundButtonSection.isVisible = show
    }
}
