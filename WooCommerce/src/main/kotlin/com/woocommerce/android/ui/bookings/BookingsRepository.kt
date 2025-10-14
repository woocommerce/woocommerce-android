package com.woocommerce.android.ui.bookings

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsOrderOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsStore
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity
import javax.inject.Inject

class BookingsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val bookingsStore: BookingsStore
) {
    suspend fun fetchBookings(
        page: Int,
        perPage: Int,
        query: String? = null,
        filters: List<BookingsFilterOption> = emptyList(),
        order: BookingsOrderOption
    ): Result<FetchResult> {
        val result = bookingsStore.fetchBookings(
            site = selectedSite.get(),
            perPage = perPage,
            page = page,
            query = query,
            filters = filters,
            order = order
        )
        return if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(
                result.model!!.let {
                    FetchResult(
                        bookings = it.bookings,
                        hasMorePages = it.hasMorePages
                    )
                }
            )
        }
    }

    fun observeBookings(
        limit: Int? = null,
        filters: List<BookingsFilterOption> = emptyList(),
        order: BookingsOrderOption
    ): Flow<List<Booking>> =
        bookingsStore.observeBookings(
            site = selectedSite.get(),
            limit = limit,
            filters = filters,
            order = order
        )

    fun observeBooking(bookingId: Long): Flow<Booking?> =
        bookingsStore.observeBooking(
            site = selectedSite.get(),
            bookingId = bookingId
        )

    suspend fun fetchBooking(
        bookingId: Long
    ): Result<Booking> {
        val result = bookingsStore.fetchBooking(
            site = selectedSite.get(),
            bookingId = bookingId
        )
        return if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(result.model!!)
        }
    }

    suspend fun fetchResource(
        resourceId: Long
    ): Result<Unit> {
        val result = bookingsStore.fetchResource(
            site = selectedSite.get(),
            resourceId = resourceId
        )
        return if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(Unit)
        }
    }

    fun observeResource(booking: Booking): Flow<BookingResource?> {
        return if (booking.resourceId == 0L) {
            flowOf(null)
        } else {
            bookingsStore.observeResource(
                site = selectedSite.get(),
                resourceId = booking.resourceId
            )
        }
    }

    data class FetchResult(
        val bookings: List<Booking>,
        val hasMorePages: Boolean
    )
}

typealias Booking = BookingEntity
typealias BookingResource = BookingResourceEntity
