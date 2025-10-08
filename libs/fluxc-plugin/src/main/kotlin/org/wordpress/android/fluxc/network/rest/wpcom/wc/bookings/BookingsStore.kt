package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.persistence.dao.BookingsDao
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.HeadersParser
import org.wordpress.android.util.AppLog
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingsStore @Inject constructor(
    private val bookingsRestClient: BookingsRestClient,
    private val ordersRestClient: OrderRestClient,
    private val bookingsDao: BookingsDao,
    private val headersParser: HeadersParser,
    private val coroutineEngine: CoroutineEngine,
) {
    suspend fun fetchBookings(
        site: SiteModel,
        perPage: Int = BookingsRestClient.DEFAULT_PER_PAGE,
        page: Int = 1,
        query: String? = null,
        filters: List<BookingsFilterOption> = emptyList(),
        order: BookingsOrderOption
    ): WooResult<BookingsFetchResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchBookings") {
            val response = bookingsRestClient.fetchBookings(site, perPage, page, query, filters, order)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    if (page == 1 && filters.isEmpty() && query.isNullOrEmpty()) {
                        // Clear existing bookings when fetching the first page
                        bookingsDao.deleteAllForSite(site.localId())
                    }

                    val orderIds = response.result.map { it.orderId }.distinct().filterNot { it == 0L }
                    val ordersResult = fetchOrders(site, orderIds)
                    if (ordersResult.isError) {
                        return@withDefaultContext WooResult(ordersResult.error)
                    }

                    val entities = response.result.map {
                        it.toEntity(
                            localSiteId = site.localId(),
                            orderEntity = ordersResult.model?.get(it.orderId)
                        )
                    }
                    bookingsDao.insertOrReplace(entities)
                    val totalPages = headersParser.getTotalPages(response.headers)
                    // Determine if we can load more from the total pages header if available, otherwise
                    // infer it from the number of items returned
                    val hasMorePages = (totalPages?.let { it > page }) ?: (entities.size == perPage)
                    WooResult(
                        BookingsFetchResult(
                            bookings = entities,
                            hasMorePages = hasMorePages
                        )
                    )
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun observeBookings(
        site: SiteModel,
        limit: Int? = null,
        filters: List<BookingsFilterOption> = emptyList(),
        order: BookingsOrderOption
    ): Flow<List<BookingEntity>> = bookingsDao.observeBookings(site.localId(), limit, filters, order)

    fun observeBooking(
        site: SiteModel,
        bookingId: Long
    ): Flow<BookingEntity?> = bookingsDao.observeBooking(site.localId(), bookingId)

    private suspend fun fetchOrders(
        site: SiteModel,
        orderIds: List<Long>
    ): WooResult<Map<Long, OrderEntity>> {
        if (orderIds.isEmpty()) { return WooResult(emptyMap()) }

        val result = ordersRestClient.fetchOrdersByIdsSync(site, orderIds)
        return if (result.isError) {
            WooResult(error = result.error?.networkError?.toWooError() ?: WooError(GENERIC_ERROR, UNKNOWN))
        } else {
            WooResult(result.fetchedOrders.map { (order, _) -> order }.associateBy { it.orderId })
        }
    }

    private suspend fun BookingDto.toEntity(
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
        order = orderEntity?.toBookingOrderInfo(productId) ?: BookingOrderInfo()
    )

    private suspend fun OrderEntity.toBookingOrderInfo(
        productId: Long
    ): BookingOrderInfo {
        return BookingOrderInfo(
            status = status,
            productInfo = BookingProductInfo(
                name = getLineItemList().firstOrNull { it.productId == productId }?.name.orEmpty()
            ),
            customerInfo = BookingCustomerInfo(
                billingFirstName = billingFirstName,
                billingLastName = billingLastName,
                billingCompany = billingCompany,
                billingAddress1 = billingAddress1,
                billingAddress2 = billingAddress2,
                billingCity = billingCity,
                billingState = billingState,
                billingPostcode = billingPostcode,
                billingCountry = billingCountry,
                billingEmail = billingEmail,
                billingPhone = billingPhone,
            )
        )
    }
}
