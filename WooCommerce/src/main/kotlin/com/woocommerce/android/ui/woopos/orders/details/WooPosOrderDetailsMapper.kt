package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrderActionsProvider
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemsState
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
import kotlin.math.abs

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
        val nonRefundedItems = getNonRefundedItems(order, historicalRefundsResult)
        val lineItems = buildLineItems(order, nonRefundedItems)
        val refundInfo = refundInfoBuilder.buildRefundInfo(order, historicalRefundsResult)
        val breakdown = refundInfoBuilder.buildTotalsBreakdown(order, refundInfo)
        val actions = orderActionsProvider.getAvailableActions(order)
        val refundedLineItems = buildRefundedLineItems(order, historicalRefundsResult)

        WooPosOrdersState.OrderDetailsViewState.Computed.Details(
            id = order.id,
            number = "#${order.number}",
            dateTime = order.dateCreated.formatToMMMddYYYYAtHHmm(
                atWord = resourceProvider.getString(R.string.date_time_connector)
            ),
            customerEmail = order.customer?.email ?: order.billingAddress.email,
            status = status,
            lineItems = LineItemsState.Loaded(lineItems),
            refundedLineItems = LineItemsState.Loaded(refundedLineItems),
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
        val mayHaveRefunds = order.refundTotal > BigDecimal.ZERO ||
            order.status == Order.Status.Refunded
        val lineItems = if (mayHaveRefunds) LineItemsState.Loading else LineItemsState.Loaded(buildLineItems(order))
        val refundedLineItems =
            if (mayHaveRefunds) LineItemsState.Loading else LineItemsState.Loaded(emptyList())
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
            refundedLineItems = refundedLineItems,
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

    suspend fun buildRefundedLineItems(
        order: Order,
        refundResult: RefundsFetchResult
    ): List<LineItemRow> = coroutineScope {
        val refunds = when (refundResult) {
            is RefundsFetchResult.Success -> refundResult.refunds
            is RefundsFetchResult.Error -> return@coroutineScope emptyList()
        }

        val allRefundItems = refunds.flatMap { it.items }
        if (allRefundItems.isEmpty()) return@coroutineScope emptyList()

        data class AggregatedRefundItem(
            val orderItemId: Long,
            val quantity: Int,
            val total: BigDecimal,
            val refundItem: Refund.Item
        )

        val aggregated = allRefundItems
            .groupBy { it.orderItemId }
            .map { (orderItemId, items) ->
                AggregatedRefundItem(
                    orderItemId = orderItemId,
                    quantity = items.sumOf { it.quantity },
                    total = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.total },
                    refundItem = items.first()
                )
            }

        aggregated.map { agg ->
            async {
                val orderItem = order.items.find { it.itemId == agg.orderItemId }
                val name = orderItem?.name ?: agg.refundItem.name
                val attributesDescription = orderItem?.attributesDescription?.takeIf { it.isNotEmpty() }
                val unitPrice = agg.refundItem.price
                val product = getProductById(agg.refundItem.productId)
                LineItemRow(
                    id = agg.orderItemId,
                    name = name,
                    attributesDescription = attributesDescription,
                    qtyAndUnitPrice = "${agg.quantity} x ${formatPrice(unitPrice, order.currency)}",
                    lineTotal = "-${formatPrice(agg.total, order.currency)}",
                    imageUrl = product?.firstImageUrl,
                )
            }
        }.awaitAll()
    }

    suspend fun buildNonRefundedLineItems(
        order: Order,
        refundResult: RefundsFetchResult
    ): List<LineItemRow> {
        val items = getNonRefundedItems(order, refundResult)
        return buildLineItems(order, items)
    }

    private fun getNonRefundedItems(
        order: Order,
        refundResult: RefundsFetchResult
    ): List<Order.Item> {
        val refunds = when (refundResult) {
            is RefundsFetchResult.Success -> refundResult.refunds
            is RefundsFetchResult.Error -> return order.items
        }
        if (refunds.isEmpty()) return order.items

        val refundedByItemId = refunds
            .flatMap { it.items }
            .groupingBy { it.orderItemId }
            .fold(0) { acc, item -> acc + item.quantity }

        return order.items.mapNotNull { item ->
            val refundedQty = abs(refundedByItemId[item.itemId] ?: 0).toFloat()
            val remaining = item.quantity - refundedQty
            when {
                remaining <= 0f -> null
                remaining == item.quantity -> item
                else -> {
                    val newTotal = item.total * (remaining / item.quantity).toBigDecimal()
                    item.copy(quantity = remaining, total = newTotal)
                }
            }
        }
    }

    private suspend fun buildLineItems(
        order: Order,
        items: List<Order.Item> = order.items
    ): List<LineItemRow> = coroutineScope {
        items.map { item ->
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
