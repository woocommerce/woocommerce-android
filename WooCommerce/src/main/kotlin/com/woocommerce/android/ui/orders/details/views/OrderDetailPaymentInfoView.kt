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
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.GiftCardSummary
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.details.OrderDetailViewState
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailRefundsAdapter
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailRefundsLineBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class OrderDetailPaymentInfoView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    @Inject lateinit var orderDetailRefundsLineBuilder: OrderDetailRefundsLineBuilder

    private val binding = OrderDetailPaymentInfoBinding.inflate(LayoutInflater.from(ctx), this)

    @Suppress("LongParameterList")
    fun updatePaymentInfo(
        order: Order,
        receiptButtonStatus: OrderDetailViewState.ReceiptButtonStatus,
        isPaymentCollectableWithCardReader: Boolean,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        onSeeReceiptClickListener: (view: View) -> Unit,
        onIssueRefundClickListener: (view: View) -> Unit,
        onCollectPaymentClickListener: (view: View) -> Unit,
        onPrintingInstructionsClickListener: (view: View) -> Unit
    ) {
        binding.paymentInfoProductsTotal.text = formatCurrencyForDisplay(order.productsTotal)
        binding.paymentInfoShippingTotal.text = formatCurrencyForDisplay(order.shippingTotal)
        binding.paymentInfoTaxesTotal.text = formatCurrencyForDisplay(order.totalTax)
        binding.paymentInfoTotal.text = formatCurrencyForDisplay(order.total)
        binding.paymentInfoLblTitle.text = context.getString(R.string.order_detail_payment_header)

        with(binding.paymentInfoRefunds) {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
        if (order.total.compareTo(BigDecimal.ZERO) == 0) {
            hidePaymentSubDetails()
        } else {
            showPaymentSubDetails()
        }

        if (order.datePaid == null) {
            binding.paymentInfoAmountPaidSection.hide()
            binding.paymentInfoPaymentMsg.text = if (order.paymentMethodTitle.isNotEmpty()) {
                context.getString(
                    R.string.orderdetail_payment_summary_onhold,
                    order.paymentMethodTitle
                )
            } else {
                context.getString(R.string.orderdetail_payment_summary_onhold_plain)
            }
        } else {
            binding.paymentInfoAmountPaidSection.show()
            binding.paymentInfoPaid.text = formatCurrencyForDisplay(order.total)

            val dateStr = order.datePaid.getMediumDate(context)
            binding.paymentInfoPaymentMsg.text = if (order.paymentMethodTitle.isNotEmpty()) {
                context.getString(
                    R.string.orderdetail_payment_summary_completed,
                    dateStr,
                    order.paymentMethodTitle
                )
            } else {
                dateStr
            }
        }

        updateDiscountsSection(order, formatCurrencyForDisplay)
        updateFeesSection(order, formatCurrencyForDisplay)
        updateRefundSection(order, formatCurrencyForDisplay, onIssueRefundClickListener)
        updateCollectPaymentSection(order, onCollectPaymentClickListener)
        updateSeeReceiptSection(receiptButtonStatus, onSeeReceiptClickListener)
        updatePrintingInstructionSection(isPaymentCollectableWithCardReader, onPrintingInstructionsClickListener)
    }

    private fun showPaymentSubDetails() {
        binding.paymentInfoProductsTotalSection.show()
        binding.paymentInfoDiscountSection.show()
        binding.paymentInfoFeesSection.show()
        binding.paymentInfoShippingSection.show()
        binding.paymentInfoTaxesSection.show()
    }

    private fun hidePaymentSubDetails() {
        binding.paymentInfoProductsTotalSection.hide()
        binding.paymentInfoDiscountSection.hide()
        binding.paymentInfoGiftCardSection.hide()
        binding.paymentInfoFeesSection.hide()
        binding.paymentInfoShippingSection.hide()
        binding.paymentInfoTaxesSection.hide()
    }

    private fun updateDiscountsSection(
        order: Order,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        if (order.discountTotal isEqualTo BigDecimal.ZERO) {
            binding.paymentInfoDiscountSection.hide()
        } else {
            binding.paymentInfoDiscountSection.show()
            binding.paymentInfoDiscountTotal.text = context.getString(
                R.string.negative_currency,
                formatCurrencyForDisplay(order.discountTotal)
            )
            binding.paymentInfoDiscountItems.text = getDiscountItemsLabel(order)
        }
    }

    private fun getDiscountItemsLabel(order: Order) = if (order.discountCodes.isNotNullOrEmpty()) {
        context.getString(R.string.orderdetail_discount_items, order.discountCodes)
    } else {
        ""
    }

    fun updateGiftCardSection(
        giftCardSummaries: List<GiftCardSummary>,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        binding.paymentInfoGiftCardSection.isVisible = giftCardSummaries.isEmpty().not()
        val giftCardCodes = giftCardSummaries.joinToString(",\n") { it.code }
        binding.paymentInfoGiftCardItems.text = context.getString(
            R.string.orderdetail_discount_items,
            giftCardCodes
        )
        val giftCardTotal = giftCardSummaries.sumOf { summary -> summary.used }
        binding.paymentInfoGiftCardTotal.text = context.getString(
            R.string.negative_currency,
            formatCurrencyForDisplay(giftCardTotal)
        )
    }

    private fun updateFeesSection(
        order: Order,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        binding.paymentInfoLblFees.text = context.getString(R.string.custom_amounts)
        if (order.feesTotal isEqualTo BigDecimal.ZERO) {
            binding.paymentInfoFeesSection.hide()
        } else {
            binding.paymentInfoFeesSection.show()
            binding.paymentInfoFees.text = formatCurrencyForDisplay(order.feesTotal)
        }
    }

    private fun updateRefundSection(
        order: Order,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        onIssueRefundClickListener: (view: View) -> Unit
    ) {
        if (order.refundTotal > BigDecimal.ZERO) {
            binding.paymentInfoRefundSection.show()
            val newTotal = order.total - order.refundTotal
            binding.paymentInfoNewTotal.text = formatCurrencyForDisplay(newTotal)
        } else {
            binding.paymentInfoRefundSection.hide()
        }

        binding.paymentInfoIssueRefundButton.setOnClickListener(onIssueRefundClickListener)
    }

    private fun updateCollectPaymentSection(
        order: Order,
        onCollectPaymentClickListener: (view: View) -> Unit
    ) {
        if (order.isOrderPaid) {
            binding.paymentInfoCollectCardPresentPaymentButton.visibility = GONE
        } else {
            binding.paymentInfoCollectCardPresentPaymentButton.visibility = VISIBLE
            binding.paymentInfoCollectCardPresentPaymentButton.setOnClickListener(
                onCollectPaymentClickListener
            )
        }
    }

    private fun updateSeeReceiptSection(
        receiptButtonStatus: OrderDetailViewState.ReceiptButtonStatus,
        onSeeReceiptClickListener: (view: View) -> Unit
    ) {
        when (receiptButtonStatus) {
            OrderDetailViewState.ReceiptButtonStatus.Loading -> {
                binding.paymentInfoSeeReceiptButton.visibility = VISIBLE
                binding.paymentInfoSeeReceiptButton.isEnabled = false
                binding.paymentInfoSeeReceiptButtonProgressBar.visibility = VISIBLE
            }
            OrderDetailViewState.ReceiptButtonStatus.Hidden -> {
                binding.paymentInfoSeeReceiptButton.visibility = GONE
                binding.paymentInfoSeeReceiptButtonProgressBar.visibility = GONE
            }
            OrderDetailViewState.ReceiptButtonStatus.Visible -> {
                binding.paymentInfoSeeReceiptButtonProgressBar.visibility = GONE
                binding.paymentInfoSeeReceiptButton.isEnabled = true
                binding.paymentInfoSeeReceiptButton.visibility = VISIBLE
                binding.paymentInfoSeeReceiptButton.setOnClickListener(
                    onSeeReceiptClickListener
                )
            }
        }
    }

    private fun updatePrintingInstructionSection(
        isPaymentCollectableWithCardReader: Boolean,
        onPrintingInstructionsClickListener: (view: View) -> Unit
    ) {
        if (isPaymentCollectableWithCardReader) {
            binding.paymentInfoPrintingInstructions.setOnClickListener(
                onPrintingInstructionsClickListener
            )
        } else {
            binding.paymentInfoPrintingInstructions.visibility = GONE
        }
    }

    fun showRefunds(
        order: Order,
        refunds: List<Refund>,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        val adapter = binding.paymentInfoRefunds.adapter as? OrderDetailRefundsAdapter
            ?: OrderDetailRefundsAdapter(
                order.isCashPayment,
                order.paymentMethodTitle,
                orderDetailRefundsLineBuilder,
                formatCurrencyForDisplay,
            )
        binding.paymentInfoRefunds.adapter = adapter
        adapter.refundList = refunds

        binding.paymentInfoRefunds.show()
        binding.paymentInfoRefundTotalSection.hide()

        var availableRefundQuantity = order.quantityOfItemsWhichPossibleToRefund
        refunds.flatMap { it.items }.groupBy { it.orderItemId }.forEach { productRefunds ->
            val refundedCount = productRefunds.value.sumOf { it.quantity }
            availableRefundQuantity -= refundedCount
        }

        // TODO: Once the refund by amount is supported again, this condition will need to be updated
        binding.paymentInfoIssueRefundButton.isVisible = availableRefundQuantity > 0 && order.isRefundAvailable
    }

    fun showRefundTotal(
        show: Boolean,
        refundTotal: BigDecimal,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        binding.paymentInfoRefundTotal.text = formatCurrencyForDisplay(refundTotal)
        binding.paymentInfoRefunds.hide()
        binding.paymentInfoRefundTotalSection.show()
        binding.paymentInfoIssueRefundButton.isVisible = show
    }
}
