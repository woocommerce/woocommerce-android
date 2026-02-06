package com.woocommerce.android.ui.woopos.bookings.details

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingActionsProvider
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsState
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class WooPosBookingDetailsMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val getProductById: WooPosGetProductById,
    private val formatPrice: WooPosFormatPrice,
    private val bookingStatusMapper: WooPosBookingStatusMapper,
    private val bookingActionsProvider: WooPosBookingActionsProvider,
) {
    suspend fun mapBookingDetails(
        order: Order,
        historicalRefundsResult: RefundsFetchResult
    ): WooPosBookingsState.BookingDetailsViewState.Computed.Details = coroutineScope {
        val status = bookingStatusMapper.mapBookingStatus(order.status)
        val lineItems = buildLineItems(order)

        // val refundInfo = refundInfoBuilder.buildRefundInfo(order, historicalRefundsResult)
        // val breakdown = refundInfoBuilder.buildTotalsBreakdown(order, refundInfo)
        val breakdown = WooPosBookingsState.BookingDetailsViewState.Computed.Details.TotalsBreakdown(
            products = "$999999",
            discount = "-$999999",
            taxes = "999999",
            shipping = null,
            refunds = listOf("-$999999"),
            netPayment = "$999999",
            discountCode = null
        )

        val actions = bookingActionsProvider.getAvailableActions(order, historicalRefundsResult)

        WooPosBookingsState.BookingDetailsViewState.Computed.Details(
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
            actionsState = WooPosBookingsState.BookingActionsState.Loaded(actions)
        )
    }

    suspend fun mapBookingDetailsWithoutActions(
        order: Order
    ): WooPosBookingsState.BookingDetailsViewState.Computed.Details = coroutineScope {
        val status = bookingStatusMapper.mapBookingStatus(order.status)
        val lineItems = buildLineItems(order)
//        val refundInfo = RefundInfo(emptyList(), BigDecimal.ZERO)
//        val breakdown = refundInfoBuilder.buildTotalsBreakdown(order, refundInfo)
        val breakdown = WooPosBookingsState.BookingDetailsViewState.Computed.Details.TotalsBreakdown(
            products = "$999999",
            discount = "-$999999",
            taxes = "999999",
            shipping = null,
            refunds = listOf("-$999999"),
            netPayment = "$999999",
            discountCode = null
        )

        WooPosBookingsState.BookingDetailsViewState.Computed.Details(
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
            actionsState = WooPosBookingsState.BookingActionsState.Loading
        )
    }

    private suspend fun buildLineItems(
        order: Order
    ): List<WooPosBookingsState.BookingDetailsViewState.Computed.Details.LineItemRow> = coroutineScope {
        order.items.map { item ->
            async {
                val unitPrice =
                    if (item.quantity == 0f) {
                        item.total
                    } else {
                        item.total / item.quantity.toBigDecimal()
                    }
                val product = getProductById(item.productId)
                WooPosBookingsState.BookingDetailsViewState.Computed.Details.LineItemRow(
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
}
