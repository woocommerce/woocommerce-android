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
import java.time.LocalDateTime
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
        filters: BookingFilters,
        order: BookingsOrderOption
    ): WooResult<BookingsFetchResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchBookings") {
            val restFilters = filters.withInclusiveDateRangeOffset()
            val response = bookingsRestClient.fetchBookings(site, perPage, page, query, restFilters, order)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val orderIds = response.result.map { it.orderId }.distinct().filterNot { it == 0L }
                    val ordersResult = fetchOrders(site, orderIds)
                    if (ordersResult.isError) {
                        return@withDefaultContext WooResult(ordersResult.error)
                    }

                    val existingLocations = bookingsDao
                        .getBookingsByIds(site.localId(), response.result.map { it.id })
                        .associate { it.id.value to it.location }

                    val entities = response.result.map {
                        with(bookingDtoMapper) {
                            it.toEntity(
                                localSiteId = site.localId(),
                                orderEntity = ordersResult.model?.get(it.orderId),
                                existingLocation = existingLocations[it.id],
                            )
                        }
                    }

                    when {
                        page == 1 && query.isNullOrEmpty() && filters == BookingFilters.EMPTY -> {
                            // Refreshing with no filters applied: perform a bulk delete.
                            bookingsDao.replaceAllForSite(site.localId(), entities)
                        }

                        page == 1 && query.isNullOrEmpty() -> {
                            // Refreshing with filters applied: selectively remove only the stale entries.
                            bookingsDao.cleanAndUpsertBookings(site.localId(), filters, entities)
                        }

                        else -> {
                            bookingsDao.upsert(entities)
                        }
                    }

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
        filters: BookingFilters? = null,
        order: BookingsOrderOption
    ): Flow<List<BookingEntity>> = bookingsDao.observeBookings(site.localId(), limit, filters, order)

    suspend fun getBookings(
        site: SiteModel,
        limit: Int? = null,
        filters: BookingFilters? = null,
        order: BookingsOrderOption
    ): List<BookingEntity> = bookingsDao.getBookings(site.localId(), limit, filters, order)

    fun observeBookingCount(
        site: SiteModel
    ): Flow<Long> = bookingsDao.observeBookingsCount(site.localId())

    fun observeBooking(
        site: SiteModel,
        bookingId: Long
    ): Flow<BookingEntity?> = bookingsDao.observeBooking(site.localId(), bookingId)

    suspend fun getBooking(
        site: SiteModel,
        bookingId: Long
    ): BookingEntity? {
        return bookingsDao.getBooking(site.localId(), bookingId)
    }

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
                    val existingBooking = bookingsDao.getBooking(site.localId(), bookingId)
                    val entity = with(bookingDtoMapper) {
                        bookingDto.toEntity(
                            localSiteId = site.localId(),
                            orderEntity = orderResult?.model,
                            existingLocation = existingBooking?.location,
                        )
                    }
                    bookingsDao.upsert(listOf(entity))
                    WooResult(entity)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun fetchResources(site: SiteModel): WooResult<Array<BookingResourceEntity>> {
        val response = bookingsRestClient.fetchResources(site)
        return when {
            response.isError -> WooResult(response.error)
            response.result != null -> {
                val entities = with(bookingDtoMapper) {
                    response.result.toEntities(site.localId())
                }
                bookingsDao.replaceAllResourcesForSite(site.localId(), entities)
                WooResult(entities.toTypedArray())
            }

            else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
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
                bookingsDao.upsert(entity)
                WooResult(entity)
            }

            else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
        }
    }

    suspend fun getResource(
        site: SiteModel,
        resourceId: Long
    ): BookingResourceEntity? = bookingsDao.getResource(site.localId(), resourceId)

    fun observeResource(
        site: SiteModel,
        resourceId: Long
    ): Flow<BookingResourceEntity?> = bookingsDao.observeResource(site.localId(), resourceId)

    fun observeResources(
        site: SiteModel
    ): Flow<List<BookingResourceEntity>> = bookingsDao.observeResources(site.localId())

    suspend fun fetchProductAvailability(
        site: SiteModel,
        productId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        resourceId: Long,
    ): WooResult<BookingAvailabilityDto> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchProductAvailability") {
            val response = bookingsRestClient.fetchProductAvailability(
                site, productId, startDate, endDate, resourceId
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(response.result)
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun fetchProductBookingLocation(
        site: SiteModel,
        productId: Long,
        bookingId: Long? = null
    ): WooResult<String?> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchProductBookingLocation") {
            if (bookingId != null) {
                val existing = bookingsDao.getBooking(site.localId(), bookingId)
                if (existing?.location != null) {
                    return@withDefaultContext WooResult(existing.location)
                }
            }
            val response = bookingsRestClient.fetchProductBookingLocation(site, productId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val location = response.result.bookingLocation?.takeIf { it.isNotBlank() }
                    if (bookingId != null) {
                        bookingsDao.updateLocation(site.localId(), bookingId, location)
                    }
                    WooResult(location)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun updateBooking(
        site: SiteModel,
        bookingId: Long,
        bookingUpdatePayload: BookingUpdatePayload,
        refreshOrder: Boolean = false,
    ): WooResult<BookingEntity> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "updateBooking") {
            val response = bookingsRestClient.updateBooking(site, bookingId, bookingUpdatePayload)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val bookingDto = response.result

                    val updatedBookingEntity = if (refreshOrder) {
                        getBookingEntityWithRefreshedOrder(site, bookingDto)
                            ?: getBookingEntityWithLocalOrder(site, bookingDto) // Fallback to local
                    } else {
                        getBookingEntityWithLocalOrder(site, bookingDto)
                    }
                    if (updatedBookingEntity == null) {
                        return@withDefaultContext WooResult(WooError(GENERIC_ERROR, UNKNOWN))
                    } else {
                        bookingsDao.upsert(updatedBookingEntity)
                        return@withDefaultContext WooResult(updatedBookingEntity)
                    }
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private suspend fun fetchOrders(
        site: SiteModel,
        orderIds: List<Long>
    ): WooResult<Map<Long, OrderEntity>> {
        if (orderIds.isEmpty()) {
            return WooResult(emptyMap())
        }

        val result = orderStore.fetchOrdersByIds(WCOrderStore.FetchOrdersByIdsPayload(site, orderIds))
        return if (result.isError) {
            WooResult(error = result.error?.networkError?.toWooError() ?: WooError(GENERIC_ERROR, UNKNOWN))
        } else {
            WooResult(result.fetchedOrders.map { (order, _) -> order }.associateBy { it.orderId })
        }
    }

    private suspend fun getBookingEntityWithRefreshedOrder(
        site: SiteModel,
        bookingDto: BookingDto
    ): BookingEntity? {
        val orderResult = orderStore.fetchSingleOrderSync(site, bookingDto.orderId)
        if (orderResult.isError) {
            return null
        } else {
            val existingBooking = bookingsDao.getBooking(site.localId(), bookingDto.id)
            val entity = with(bookingDtoMapper) {
                bookingDto.toEntity(
                    localSiteId = site.localId(),
                    orderEntity = orderResult.model,
                    existingLocation = existingBooking?.location,
                )
            }
            return entity
        }
    }

    private suspend fun getBookingEntityWithLocalOrder(
        site: SiteModel,
        bookingDto: BookingDto
    ): BookingEntity? {
        val storedBooking = bookingsDao.getBooking(
            localSiteId = site.localId(),
            bookingId = bookingDto.id,
        )
        if (storedBooking == null) {
            return null
        }
        val entity = with(bookingDtoMapper) {
            bookingDto.toEntity(
                localSiteId = site.localId(),
                orderEntity = null,
                existingLocation = storedBooking.location,
            ).copy(
                // Preserve fields not returned by the API
                order = storedBooking.order,
            )
        }
        return entity
    }
}

private fun BookingFilters.withInclusiveDateRangeOffset(): BookingFilters {
    return copy(
        dateRange = dateRange.copy(
            before = dateRange.before?.plusSeconds(1),
            after = dateRange.after?.minusSeconds(1),
        )
    )
}
