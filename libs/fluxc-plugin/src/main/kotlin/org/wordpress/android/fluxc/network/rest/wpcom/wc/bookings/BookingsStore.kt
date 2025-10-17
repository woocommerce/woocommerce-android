package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.persistence.dao.BookingsDao
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.HeadersParser
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingsStore @Inject internal constructor(
    private val bookingsRestClient: BookingsRestClient,
    private val orderStore: WCOrderStore,
    private val bookingsDao: BookingsDao,
    private val bookingDtoMapper: BookingDtoMapper,
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
                    val orderIds = response.result.map { it.orderId }.distinct().filterNot { it == 0L }
                    val ordersResult = fetchOrders(site, orderIds)
                    if (ordersResult.isError) {
                        return@withDefaultContext WooResult(ordersResult.error)
                    }

                    if (page == 1 && filters.isEmpty() && query.isNullOrEmpty()) {
                        // Clear existing bookings when fetching the first page
                        bookingsDao.deleteAllForSite(site.localId())
                    }

                    val entities = response.result.map {
                        with(bookingDtoMapper) {
                            it.toEntity(
                                localSiteId = site.localId(),
                                orderEntity = ordersResult.model?.get(it.orderId)
                            )
                        }
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

    suspend fun fetchBooking(
        site: SiteModel,
        bookingId: Long
    ): WooResult<BookingEntity> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchBooking") {
            val response = bookingsRestClient.fetchBooking(site, bookingId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val bookingDto = response.result
                    val orderResult = bookingDto.orderId.takeIf { it != 0L }?.let {
                        orderStore.fetchSingleOrderSync(site, bookingDto.orderId)
                    }
                    if (orderResult?.isError == true) {
                        return@withDefaultContext WooResult(orderResult.error)
                    }
                    val entity = with(bookingDtoMapper) {
                        bookingDto.toEntity(
                            localSiteId = site.localId(),
                            orderEntity = orderResult?.model,
                        )
                    }
                    bookingsDao.insertOrReplace(listOf(entity))
                    WooResult(entity)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun fetchResource(
        site: SiteModel,
        resourceId: Long
    ): WooResult<BookingResourceEntity> {
        val response = bookingsRestClient.fetchResource(site, resourceId)
        return when {
            response.isError -> WooResult(response.error)
            response.result != null -> {
                val entity = with(bookingDtoMapper) {
                    response.result.toEntity(site.localId())
                }
                bookingsDao.insertOrReplace(entity)
                WooResult(entity)
            }

            else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
        }
    }

    fun observeResource(
        site: SiteModel,
        resourceId: Long
    ): Flow<BookingResourceEntity?> = bookingsDao.observeResource(site.localId(), resourceId)

    private suspend fun fetchOrders(
        site: SiteModel,
        orderIds: List<Long>
    ): WooResult<Map<Long, OrderEntity>> {
        if (orderIds.isEmpty()) { return WooResult(emptyMap()) }

        val result = orderStore.fetchOrdersByIds(WCOrderStore.FetchOrdersByIdsPayload(site, orderIds))
        return if (result.isError) {
            WooResult(error = result.error?.networkError?.toWooError() ?: WooError(GENERIC_ERROR, UNKNOWN))
        } else {
            WooResult(result.fetchedOrders.map { (order, _) -> order }.associateBy { it.orderId })
        }
    }
}
