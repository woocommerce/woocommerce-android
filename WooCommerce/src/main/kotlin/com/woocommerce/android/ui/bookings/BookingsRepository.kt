package com.woocommerce.android.ui.bookings

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingUpdatePayload
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

    suspend fun getBooking(bookingId: Long): Booking? {
        return bookingsStore.observeBooking(
            site = selectedSite.get(),
            bookingId = bookingId
        ).first()
    }

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

    fun observeResource(resourceId: Long): Flow<BookingResource?> {
        return if (resourceId == 0L) {
            flowOf(null)
        } else {
            bookingsStore.observeResource(
                site = selectedSite.get(),
                resourceId = resourceId
            )
        }
    }

    suspend fun updateAttendanceStatus(
        bookingId: Long,
        attendanceStatus: BookingEntity.AttendanceStatus,
    ): Result<Unit> {
        val result = bookingsStore.updateBooking(
            site = selectedSite.get(),
            bookingId = bookingId,
            bookingUpdatePayload = BookingUpdatePayload(attendanceStatus = attendanceStatus)
        )
        return if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(Unit)
        }
    }

    suspend fun updateNote(
        bookingId: Long,
        note: String,
    ): Result<Unit> {
        val result = bookingsStore.updateBooking(
            site = selectedSite.get(),
            bookingId = bookingId,
            bookingUpdatePayload = BookingUpdatePayload(note = note)
        )
        return if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(Unit)
        }
    }

    data class FetchResult(
        val bookings: List<Booking>,
        val hasMorePages: Boolean
    )
}

typealias Booking = BookingEntity
typealias BookingResource = BookingResourceEntity
