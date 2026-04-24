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
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundsState
import com.woocommerce.android.ui.woopos.orders.details.refund.RefundInfo
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class WooPosOrderDetailsMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val getProductById: WooPosGetProductById,
    private val formatPrice: WooPosFormatPrice,
    private val orderStatusMapper: WooPosOrderStatusMapper,
    private val refundInfoBuilder: WooPosRefundInfoBuilder,
    private val orderActionsProvider: WooPosOrderActionsProvider,
    private val bookingInfoMapper: WooPosBookingInfoMapper,
    private val getNonRefundedItems: WooPosGetNonRefundedItems,
    private val groupRefundedItems: WooPosGroupRefundedItems,
) {
    suspend fun mapOrderDetails(
        order: Order,
        historicalRefundsResult: RefundsFetchResult
    ): WooPosOrdersState.OrderDetailsViewState.Computed.Details = coroutineScope {
        val status = orderStatusMapper.mapOrderStatus(order.status)
        val refunds = when (historicalRefundsResult) {
            is RefundsFetchResult.Success -> historicalRefundsResult.refunds
            is RefundsFetchResult.Error -> emptyList()
        }
        val nonRefundedItems = getNonRefundedItems(order, refunds)
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
        val isFullyRefunded = order.status == Order.Status.Refunded
        val hasPartialRefund = order.refundTotal > BigDecimal.ZERO && !isFullyRefunded
        val lineItems = when {
            isFullyRefunded -> LineItemsState.Loaded(emptyList())
            hasPartialRefund -> LineItemsState.Loading
            else -> LineItemsState.Loaded(buildLineItems(order))
        }
        val refundedLineItems = when {
            isFullyRefunded || hasPartialRefund -> LineItemsState.Loading
            else -> LineItemsState.Loaded(emptyList())
        }
        val hasRefunds = isFullyRefunded || hasPartialRefund
        val refundInfo = RefundInfo(emptyList(), BigDecimal.ZERO)
        val breakdown = refundInfoBuilder.buildTotalsBreakdown(order, refundInfo).let {
            if (hasRefunds) it.copy(refundsState = RefundsState.Loading) else it
        }

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
            actionsState = WooPosOrdersState.OrderActionsState.Loaded(
                orderActionsProvider.getAvailableActions(order)
            )
        )
    }

    suspend fun buildRefundedLineItems(
        order: Order,
        refundResult: RefundsFetchResult
    ): List<LineItemRow> {
        val refunds = when (refundResult) {
            is RefundsFetchResult.Success -> refundResult.refunds
            is RefundsFetchResult.Error -> return emptyList()
        }
        return buildLineItemsFromRefunds(order, refunds)
    }

    suspend fun buildNonRefundedLineItems(
        order: Order,
        refundResult: RefundsFetchResult
    ): List<LineItemRow> {
        val refunds = when (refundResult) {
            is RefundsFetchResult.Success -> refundResult.refunds
            is RefundsFetchResult.Error -> emptyList()
        }
        val items = getNonRefundedItems(order, refunds)
        return buildLineItems(order, items)
    }

    suspend fun buildLineItemsForSingleRefund(
        order: Order,
        refund: Refund
    ): List<LineItemRow> = buildLineItemsFromRefunds(order, listOf(refund))

    private suspend fun buildLineItemsFromRefunds(
        order: Order,
        refunds: List<Refund>
    ): List<LineItemRow> = coroutineScope {
        val groupedItems = groupRefundedItems(refunds)
        groupedItems.map { refundItem ->
            async {
                val orderItem = order.items.find { it.itemId == refundItem.orderItemId }
                val name = orderItem?.name ?: refundItem.name
                val attributesDescription = orderItem?.attributesDescription?.takeIf { it.isNotEmpty() }
                val absQuantity = kotlin.math.abs(refundItem.quantity)
                val total = refundItem.total
                val unitPrice = if (absQuantity != 0) {
                    total.divide(
                        BigDecimal.valueOf(absQuantity.toLong()),
                        total.scale(),
                        RoundingMode.HALF_UP
                    )
                } else {
                    total
                }
                val product = getProductById(refundItem.productId)
                LineItemRow(
                    id = refundItem.orderItemId,
                    name = name,
                    attributesDescription = attributesDescription,
                    qtyAndUnitPrice = "$absQuantity x ${formatPrice(unitPrice.abs(), order.currency)}",
                    lineTotal = formatPrice(total.abs(), order.currency),
                    imageUrl = product?.firstImageUrl,
                )
            }
        }.awaitAll()
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
                    qtyAndUnitPrice = "${item.quantity.toInt()} x ${formatPrice(unitPrice, order.currency)}",
                    lineTotal = formatPrice(item.total, order.currency),
                    imageUrl = product?.firstImageUrl,
                    bookingInfo = bookingInfo,
                )
            }
        }.awaitAll()
    }
}
