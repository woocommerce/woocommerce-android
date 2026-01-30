package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState
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
        return when (refundResult) {
            is RefundsFetchResult.Success -> {
                val amounts = refundResult.refunds.map { "-${formatPrice(it.amount, order.currency)}" }
                val total = refundResult.refunds.sumOf { it.amount }
                RefundInfo(amounts, total)
            }
            is RefundsFetchResult.Error -> {
                val amounts =
                    if (order.refundTotal > BigDecimal.ZERO) {
                        listOf(resourceProvider.getString(R.string.woopos_orders_details_refund_error))
                    } else {
                        emptyList()
                    }
                RefundInfo(amounts, BigDecimal.ZERO)
            }
        }
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

        return WooPosOrdersState.OrderDetailsViewState.Computed.Details.TotalsBreakdown(
            products = formatPrice(order.productsTotal, order.currency),
            discount = order.discountTotal.takeIf { !it.isZero() }
                ?.let { "-${formatPrice(it, order.currency)}" },
            discountCode = discountCode,
            taxes = formatPrice(order.totalTax, order.currency),
            shipping = order.shippingTotal.takeIf { !it.isZero() }?.let { formatPrice(it, order.currency) },
            refunds = refundInfo.refundAmounts,
            netPayment = netPayment
        )
    }
}

data class RefundInfo(
    val refundAmounts: List<String>,
    val totalRefunded: BigDecimal
)

private fun BigDecimal.isZero() = this.compareTo(BigDecimal.ZERO) == 0
