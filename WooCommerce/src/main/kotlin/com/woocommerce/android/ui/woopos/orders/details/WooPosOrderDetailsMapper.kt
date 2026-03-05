package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrderActionsProvider
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow
import com.woocommerce.android.ui.woopos.orders.details.refund.RefundInfo
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import javax.inject.Inject

class WooPosOrderDetailsMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val getProductById: WooPosGetProductById,
    private val formatPrice: WooPosFormatPrice,
    private val orderStatusMapper: WooPosOrderStatusMapper,
    private val refundInfoBuilder: WooPosRefundInfoBuilder,
    private val orderActionsProvider: WooPosOrderActionsProvider,
    private val bookingInfoMapper: WooPosBookingInfoMapper,
) {
    suspend fun mapOrderDetails(
        order: Order,
        historicalRefundsResult: RefundsFetchResult
    ): WooPosOrdersState.OrderDetailsViewState.Computed.Details = coroutineScope {
        val status = orderStatusMapper.mapOrderStatus(order.status)
        val lineItems = buildLineItems(order)
        val refundInfo = refundInfoBuilder.buildRefundInfo(order, historicalRefundsResult)
        val breakdown = refundInfoBuilder.buildTotalsBreakdown(order, refundInfo)
        val actions = orderActionsProvider.getAvailableActions(order)

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
            totalPaid = if (order.isOrderPaid) {
                formatPrice(order.total, order.currency)
            } else {
                formatPrice(BigDecimal.ZERO, order.currency)
            },
            paymentMethodTitle = order.paymentMethodTitle.takeIf { it.isNotBlank() },
            actionsState = WooPosOrdersState.OrderActionsState.Loaded(actions)
        )
    }

    suspend fun mapOrderDetailsWithoutActions(
        order: Order
    ): WooPosOrdersState.OrderDetailsViewState.Computed.Details = coroutineScope {
        val status = orderStatusMapper.mapOrderStatus(order.status)
        val lineItems = buildLineItems(order)
        val refundInfo = RefundInfo(emptyList(), BigDecimal.ZERO)
        val breakdown = refundInfoBuilder.buildTotalsBreakdown(order, refundInfo)

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
            totalPaid = if (order.isOrderPaid) {
                formatPrice(order.total)
            } else {
                formatPrice(BigDecimal.ZERO)
            },
            paymentMethodTitle = order.paymentMethodTitle.takeIf { it.isNotBlank() },
            actionsState = WooPosOrdersState.OrderActionsState.Loading
        )
    }

    private suspend fun buildLineItems(
        order: Order
    ): List<LineItemRow> = coroutineScope {
        order.items.map { item ->
            async {
                val unitPrice =
                    if (item.quantity == 0f) {
                        item.total
                    } else {
                        item.total / item.quantity.toBigDecimal()
                    }
                val product = getProductById(item.productId)
                val bookingInfo = item.bookingId?.let { bookingInfoMapper.resolveBookingInfo(it) }
                LineItemRow(
                    id = item.itemId,
                    name = item.name,
                    attributesDescription = item.attributesDescription.takeIf { it.isNotEmpty() },
                    qtyAndUnitPrice = "${item.quantity.toInt()} x ${formatPrice(unitPrice)}",
                    lineTotal = formatPrice(item.total, order.currency),
                    imageUrl = product?.firstImageUrl,
                    bookingInfo = bookingInfo,
                )
            }
        }.awaitAll()
    }
}
