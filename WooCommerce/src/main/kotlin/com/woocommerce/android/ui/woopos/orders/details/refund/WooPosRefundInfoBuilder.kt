package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundsState
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import java.math.BigDecimal
import javax.inject.Inject

class WooPosRefundInfoBuilder @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val formatPrice: WooPosFormatPrice,
) {
    fun buildRefundInfo(
        order: Order,
        refundResult: RefundsFetchResult
    ): RefundInfo {
        val atWord = resourceProvider.getString(R.string.date_time_connector)
        return when (refundResult) {
            is RefundsFetchResult.Success -> {
                val sorted = refundResult.refunds.sortedBy { it.dateCreated }
                val rows = sorted.mapIndexed { index, refund ->
                    buildRefundRowData(refund, index + 1, order, atWord)
                }
                val total = sorted.sumOf { it.amount }
                RefundInfo(rows, total)
            }
            is RefundsFetchResult.Error -> {
                RefundInfo(
                    refundRows = emptyList(),
                    totalRefunded = BigDecimal.ZERO,
                    loadError = resourceProvider.getString(
                        R.string.woopos_orders_details_refund_error
                    )
                )
            }
        }
    }

    private fun buildRefundRowData(
        refund: Refund,
        index: Int,
        order: Order,
        atWord: String
    ): RefundRowData {
        return RefundRowData(
            refund = refund,
            formattedAmount = "-${formatPrice(refund.amount, order.currency)}",
            label = resourceProvider.getString(
                R.string.woopos_orders_details_refund_label_numbered,
                index
            ),
            date = refund.dateCreated.formatToMMMddYYYYAtHHmm(atWord = atWord),
            reason = refund.reason?.takeIf { it.isNotBlank() },
        )
    }

    fun buildTotalsBreakdown(
        order: Order,
        refundInfo: RefundInfo
    ): WooPosOrdersState.OrderDetailsViewState.Computed.Details.TotalsBreakdown {
        val netPayment = if (refundInfo.totalRefunded > BigDecimal.ZERO) {
            formatPrice(order.total - refundInfo.totalRefunded, order.currency)
        } else {
            null
        }

        val discountCode = order.couponLines.firstOrNull()?.code

        val refundsState = when {
            refundInfo.loadError != null -> RefundsState.Error(refundInfo.loadError)
            else -> RefundsState.Loaded(
                refunds = refundInfo.refundRows.map { row ->
                    WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundRow(
                        label = row.label,
                        amount = row.formattedAmount,
                        date = row.date,
                        reason = row.reason,
                    )
                }
            )
        }

        return WooPosOrdersState.OrderDetailsViewState.Computed.Details.TotalsBreakdown(
            products = formatPrice(order.productsTotal, order.currency),
            discount = order.discountTotal.takeIf { !it.isZero() }
                ?.let { "-${formatPrice(it, order.currency)}" },
            discountCode = discountCode,
            taxes = formatPrice(order.totalTax, order.currency),
            shipping = order.shippingTotal.takeIf { !it.isZero() }?.let { formatPrice(it, order.currency) },
            refundsState = refundsState,
            netPayment = netPayment,
        )
    }
}

data class RefundRowData(
    val refund: Refund,
    val formattedAmount: String,
    val label: String,
    val date: String,
    val reason: String?,
)

data class RefundInfo(
    val refundRows: List<RefundRowData>,
    val totalRefunded: BigDecimal,
    val loadError: String? = null
)

private fun BigDecimal.isZero() = this.compareTo(BigDecimal.ZERO) == 0
