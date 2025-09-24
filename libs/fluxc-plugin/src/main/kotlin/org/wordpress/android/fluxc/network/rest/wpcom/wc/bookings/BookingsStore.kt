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
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingsStore @Inject constructor(
    private val bookingsRestClient: BookingsRestClient,
    private val bookingsDao: BookingsDao,
    private val coroutineEngine: CoroutineEngine,
) {
    suspend fun fetchBookings(
        site: SiteModel,
        perPage: Int = BookingsRestClient.DEFAULT_PER_PAGE,
        page: Int = 1
    ): WooResult<List<BookingEntity>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchBookings") {
            val response = bookingsRestClient.fetchBookings(site, perPage, page)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val entities = response.result.map { it.toEntity(site.localId()) }
                    bookingsDao.insertOrReplace(entities)
                    WooResult(entities)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun observeBookings(site: SiteModel): Flow<List<BookingEntity>> =
        bookingsDao.observeBookings(site.localId())

    private fun BookingDto.toEntity(localSiteId: LocalId): BookingEntity = BookingEntity(
        id = RemoteId(id),
        localSiteId = localSiteId,
        start = start,
        end = end,
        allDay = allDay,
        status = status,
        cost = cost,
        currency = currency,
        customerId = customerId,
        productId = productId,
        resourceId = resourceId,
        dateCreated = dateCreated,
        dateModified = dateModified,
        googleCalendarEventId = googleCalendarEventId,
        orderId = orderId,
        orderItemId = orderItemId,
        parentId = parentId,
        personCounts = personCounts?.map { it.toLong() },
        localTimezone = localTimezone
    )
}
