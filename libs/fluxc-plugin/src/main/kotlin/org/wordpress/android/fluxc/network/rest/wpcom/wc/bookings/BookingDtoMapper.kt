package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

internal class BookingDtoMapper @Inject constructor(
    private val productsDao: ProductsDao
) {
    private val OrderEntity.hasBillingInfo
        get() = billingFirstName.isNotEmpty() ||
            billingLastName.isNotEmpty() ||
            billingCompany.isNotEmpty() ||
            billingAddress1.isNotEmpty() ||
            billingAddress2.isNotEmpty() ||
            billingCity.isNotEmpty() ||
            billingState.isNotEmpty() ||
            billingPostcode.isNotEmpty() ||
            billingCountry.isNotEmpty() ||
            billingEmail.isNotEmpty() ||
            billingPhone.isNotEmpty()

    suspend fun BookingDto.toEntity(
        localSiteId: LocalId,
        orderEntity: OrderEntity?
    ): BookingEntity = BookingEntity(
        id = RemoteId(id),
        localSiteId = localSiteId,
        start = Instant.ofEpochSecond(start),
        end = Instant.ofEpochSecond(end),
        allDay = allDay,
        status = BookingEntity.Status.fromKey(status),
        cost = cost,
        currency = currency,
        customerId = customerId,
        productId = productId,
        resourceId = resourceId,
        dateCreated = Instant.ofEpochSecond(dateCreated),
        dateModified = Instant.ofEpochSecond(dateModified),
        googleCalendarEventId = googleCalendarEventId,
        orderId = orderId,
        orderItemId = orderItemId,
        parentId = parentId,
        personCounts = personCounts?.map { it.toLong() },
        localTimezone = localTimezone,
        order = orderEntity?.toBookingOrderInfo(productId) ?: BookingOrderInfo(
            productInfo = productsDao.getProduct(localSiteId.value, productId)?.let {
                BookingProductInfo(name = it.name)
            }
        )
    )

    private suspend fun OrderEntity.toBookingOrderInfo(
        productId: Long
    ): BookingOrderInfo {
        val lineItem = getLineItemList().firstOrNull { it.productId == productId }
        return BookingOrderInfo(
            status = status,
            productInfo = BookingProductInfo(
                name = lineItem?.name.orEmpty()
            ),
            customerInfo = if (hasBillingInfo) {
                BookingCustomerInfo(
                    billingFirstName = billingFirstName.ifEmpty { null },
                    billingLastName = billingLastName.ifEmpty { null },
                    billingCompany = billingCompany.ifEmpty { null },
                    billingAddress1 = billingAddress1.ifEmpty { null },
                    billingAddress2 = billingAddress2.ifEmpty { null },
                    billingCity = billingCity.ifEmpty { null },
                    billingState = billingState.ifEmpty { null },
                    billingPostcode = billingPostcode.ifEmpty { null },
                    billingCountry = billingCountry.ifEmpty { null },
                    billingEmail = billingEmail.ifEmpty { null },
                    billingPhone = billingPhone.ifEmpty { null },
                )
            } else {
                null
            },
            paymentInfo = lineItem?.let {
                BookingPaymentInfo(
                    subtotal = it.subtotal?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    subtotalTax = it.subtotalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    total = it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    totalTax = it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                )
            }
        )
    }
}
