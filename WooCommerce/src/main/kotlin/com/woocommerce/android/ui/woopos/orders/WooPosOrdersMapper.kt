package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject

class WooPosOrdersMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val locale: Locale,
    private val getProductById: WooPosGetProductById,
    private val formatPrice: WooPosFormatPrice,
    private val getRefundableItems: WooPosGetRefundableItems,
) {
    suspend fun mapOrderItem(order: Order, selectedId: Long?): WooPosOrdersState.OrderItemViewState {
        val statusText = order.status.localizedLabel(resourceProvider, locale)

        return WooPosOrdersState.OrderItemViewState(
            id = order.id,
            title = "#${order.number}",
            date = order.dateCreated.formatToMMMddYYYYAtHHmm(
                atWord = resourceProvider.getString(R.string.date_time_connector)
            ),
            total = formatPrice(order.total, order.currency),
            customerEmail = order.customer?.email ?: order.billingAddress.email,
            isSelected = order.id == selectedId,
            status = PosOrderStatus(
                text = statusText,
                colorKey = OrderStatusColorKey.fromStatus(order.status)
            ),
            statusSlug = order.status.toString(),
            createdAtMillis = order.dateCreated.time
        )
    }

    suspend fun mapOrderDetails(
        order: Order,
        historicalRefundsResult: RefundsFetchResult
    ): WooPosOrdersState.OrderDetailsViewState.Computed.Details = coroutineScope {
        val status = mapOrderStatus(order)
        val lineItems = buildLineItems(order)
        val refundInfo = buildRefundInfo(order, historicalRefundsResult)
        val breakdown = buildTotalsBreakdown(order, refundInfo)
        val actions = getAvailableActions(order, historicalRefundsResult)

        WooPosOrdersState.OrderDetailsViewState.Computed.Details(
            id = order.id,
            number = "#${order.number}",
            dateTime = order.dateCreated.formatToMMMddYYYYAtHHmm(
                atWord = resourceProvider.getString(R.string.date_time_connector)
            ),
            customerEmail = order.customer?.email ?: order.billingAddress.email,
            status = status,
            lineItems = lineItems,
            breakdown = breakdown,
            total = formatPrice(order.total, order.currency),
            totalPaid = formatPrice(order.total, order.currency),
            paymentMethodTitle = order.paymentMethodTitle.takeIf { it.isNotBlank() },
            actionsState = WooPosOrdersState.OrderActionsState.Loaded(actions)
        )
    }

    suspend fun mapOrderDetailsWithoutActions(
        order: Order
    ): WooPosOrdersState.OrderDetailsViewState.Computed.Details = coroutineScope {
        val status = mapOrderStatus(order)
        val lineItems = buildLineItems(order)
        val refundInfo = RefundInfo(emptyList(), BigDecimal.ZERO)
        val breakdown = buildTotalsBreakdown(order, refundInfo)

        WooPosOrdersState.OrderDetailsViewState.Computed.Details(
            id = order.id,
            number = "#${order.number}",
            dateTime = order.dateCreated.formatToMMMddYYYYAtHHmm(
                atWord = resourceProvider.getString(R.string.date_time_connector)
            ),
            customerEmail = order.customer?.email ?: order.billingAddress.email,
            status = status,
            lineItems = lineItems,
            breakdown = breakdown,
            total = formatPrice(order.total),
            totalPaid = formatPrice(order.total),
            paymentMethodTitle = order.paymentMethodTitle.takeIf { it.isNotBlank() },
            actionsState = WooPosOrdersState.OrderActionsState.Loading
        )
    }

    fun getAvailableActions(
        order: Order,
        refundResult: RefundsFetchResult
    ): List<WooPosOrdersState.OrderAction> {
        return buildList {
            if (FeatureFlag.POS_REFUNDS.isEnabled() && hasRefundableItems(order, refundResult)) {
                add(WooPosOrdersState.OrderAction.IssueRefund(order.id))
            }
            add(WooPosOrdersState.OrderAction.EmailReceipt(order.id))
        }
    }

    suspend fun buildRefundInfo(
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

    suspend fun buildTotalsBreakdown(
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

    private fun mapOrderStatus(order: Order): PosOrderStatus {
        val statusText = order.status.localizedLabel(resourceProvider, locale)
        return PosOrderStatus(
            text = statusText,
            colorKey = OrderStatusColorKey.fromStatus(order.status)
        )
    }

    private suspend fun buildLineItems(
        order: Order
    ): List<WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow> = coroutineScope {
        order.items.map { item ->
            async {
                val unitPrice =
                    if (item.quantity == 0f) {
                        item.total
                    } else {
                        item.total / item.quantity.toBigDecimal()
                    }
                val product = getProductById(item.productId)
                WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                    id = item.itemId,
                    name = item.name,
                    attributesDescription = item.attributesDescription.takeIf { it.isNotEmpty() },
                    qtyAndUnitPrice = "${item.quantity.toInt()} x ${formatPrice(unitPrice)}",
                    lineTotal = formatPrice(item.total, order.currency),
                    imageUrl = product?.firstImageUrl
                )
            }
        }.awaitAll()
    }

    private fun hasRefundableItems(order: Order, refundResult: RefundsFetchResult): Boolean {
        val refunds = when (refundResult) {
            is RefundsFetchResult.Success -> refundResult.refunds
            is RefundsFetchResult.Error -> emptyList()
        }
        return getRefundableItems(order, refunds).isNotEmpty()
    }
}

data class RefundInfo(
    val refundAmounts: List<String>,
    val totalRefunded: BigDecimal
)

private fun Order.Status.localizedLabel(resourceProvider: ResourceProvider, locale: Locale): String {
    return when (this) {
        Order.Status.Cancelled -> resourceProvider.getString(R.string.woopos_orders_status_cancelled)
        Order.Status.Completed -> resourceProvider.getString(R.string.woopos_orders_status_completed)
        is Order.Status.Custom ->
            value.replaceFirstChar { it.titlecase(locale) }.replace("-", " ")
        Order.Status.Failed -> resourceProvider.getString(R.string.woopos_orders_status_failed)
        Order.Status.OnHold -> resourceProvider.getString(R.string.woopos_orders_status_on_hold)
        Order.Status.Pending -> resourceProvider.getString(R.string.woopos_orders_status_pending)
        Order.Status.Processing -> resourceProvider.getString(R.string.woopos_orders_status_processing)
        Order.Status.Refunded -> resourceProvider.getString(R.string.woopos_orders_status_refunded)
    }
}

private fun BigDecimal.isZero() = this.compareTo(BigDecimal.ZERO) == 0
