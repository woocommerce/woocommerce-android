package org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.dao.BookingsDao
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.HeadersParser
import org.wordpress.android.util.AppLog
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingsStore @Inject constructor(
    private val bookingsRestClient: BookingsRestClient,
    private val bookingsDao: BookingsDao,
    private val headersParser: HeadersParser,
    private val coroutineEngine: CoroutineEngine,
) {
    suspend fun fetchBookings(
        site: SiteModel,
        perPage: Int = BookingsRestClient.DEFAULT_PER_PAGE,
        page: Int = 1
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchBookings") {
            val response = bookingsRestClient.fetchBookings(site, perPage, page)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    if (page == 1) {
                        // Clear existing bookings when fetching the first page
                        // TODO when supporting filters, we should only clear bookings if no filters are applied
                        bookingsDao.deleteAllForSite(site.localId())
                    }
                    val entities = response.result.map { it.toEntity(site.localId()) }
                    bookingsDao.insertOrReplace(entities)
                    val totalPages = headersParser.getTotalPages(response.headers)
                    // Determine if we can load more from the total pages header if available, otherwise
                    // infer it from the number of items returned
                    val canLoadMore = (totalPages?.let { it > page }) ?: (entities.size == perPage)
                    WooResult(canLoadMore)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun observeBookings(site: SiteModel, limit: Int? = null): Flow<List<BookingEntity>> =
        bookingsDao.observeBookings(site.localId(), limit)

    private fun BookingDto.toEntity(localSiteId: LocalId): BookingEntity = BookingEntity(
        id = RemoteId(id),
        localSiteId = localSiteId,
        start = Instant.ofEpochSecond(start),
        end = Instant.ofEpochSecond(end),
        allDay = allDay,
        status = status,
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
        localTimezone = localTimezone
    )
}
