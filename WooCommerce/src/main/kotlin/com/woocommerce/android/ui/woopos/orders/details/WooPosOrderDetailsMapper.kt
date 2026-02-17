package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrderActionsProvider
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.BookingInfo
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow
import com.woocommerce.android.ui.woopos.orders.details.refund.RefundInfo
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.ext.formatToMMMddYYYYAtHHmm
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.DateFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.math.BigDecimal
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

class WooPosOrderDetailsMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val getProductById: WooPosGetProductById,
    private val formatPrice: WooPosFormatPrice,
    private val orderStatusMapper: WooPosOrderStatusMapper,
    private val refundInfoBuilder: WooPosRefundInfoBuilder,
    private val orderActionsProvider: WooPosOrderActionsProvider,
    private val getLineItemBookingIds: WooPosGetLineItemBookingIds,
    private val bookingsRepository: BookingsRepository,
    private val dateFormatter: DateFormatter,
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
            totalPaid = formatPrice(order.total, order.currency),
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
            totalPaid = formatPrice(order.total),
            paymentMethodTitle = order.paymentMethodTitle.takeIf { it.isNotBlank() },
            actionsState = WooPosOrdersState.OrderActionsState.Loading
        )
    }

    fun formatBookingInfoForLineItem(
        lineItem: LineItemRow,
        booking: BookingEntity,
        bookingId: Long
    ): LineItemRow {
        return lineItem.copy(
            bookingInfo = BookingInfo.Loaded(formatBookingInfo(bookingId, booking))
        )
    }

    private suspend fun buildLineItems(
        order: Order
    ): List<LineItemRow> = coroutineScope {
        val bookingIds = getLineItemBookingIds(order.id)
        order.items.map { item ->
            async {
                val unitPrice =
                    if (item.quantity == 0f) {
                        item.total
                    } else {
                        item.total / item.quantity.toBigDecimal()
                    }
                val product = getProductById(item.productId)
                val bookingInfo = resolveBookingInfo(item.itemId, bookingIds)
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

    private suspend fun resolveBookingInfo(
        itemId: Long,
        bookingIds: Map<Long, Long>
    ): BookingInfo? {
        val bookingId = bookingIds[itemId] ?: return null
        val booking = bookingsRepository.getBooking(bookingId)
        return if (booking != null) {
            BookingInfo.Loaded(formatBookingInfo(bookingId, booking))
        } else {
            BookingInfo.Loading(bookingId)
        }
    }

    private fun formatBookingInfo(bookingId: Long, booking: BookingEntity): String {
        val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withZone(ZoneOffset.UTC)
        val date = dateFormat.format(booking.start)
        val startTime = dateFormatter.formatTime(booking.start)
        val endTime = dateFormatter.formatTime(booking.end)
        return resourceProvider.getString(
            R.string.woopos_orders_details_booking_info,
            bookingId,
            date,
            startTime,
            endTime
        )
    }
}
