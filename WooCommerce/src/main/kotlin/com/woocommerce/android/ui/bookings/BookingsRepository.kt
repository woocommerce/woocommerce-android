package com.woocommerce.android.ui.bookings

import com.woocommerce.android.WooException
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingAvailabilityDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingUpdatePayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsOrderOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsStore
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import org.wordpress.android.fluxc.persistence.entity.BookingResourceEntity
import java.time.LocalDateTime
import javax.inject.Inject

class BookingsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val bookingsStore: BookingsStore,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    suspend fun fetchBookings(
        page: Int,
        perPage: Int,
        query: String? = null,
        filters: BookingFilters = BookingFilters.EMPTY,
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
        filters: BookingFilters? = null,
        order: BookingsOrderOption
    ): Flow<List<Booking>> =
        bookingsStore.observeBookings(
            site = selectedSite.get(),
            limit = limit,
            filters = filters,
            order = order
        )

    suspend fun getBookingsList(
        limit: Int? = null,
        filters: BookingFilters? = null,
        order: BookingsOrderOption
    ): List<Booking> =
        bookingsStore.getBookings(
            site = selectedSite.get(),
            limit = limit,
            filters = filters,
            order = order
        )

    fun observeBookingsCount(): Flow<Long> = bookingsStore.observeBookingCount(site = selectedSite.get())

    fun observeBooking(bookingId: Long): Flow<Booking?> =
        bookingsStore.observeBooking(
            site = selectedSite.get(),
            bookingId = bookingId
        )

    suspend fun getBooking(bookingId: Long): Booking? {
        return bookingsStore.getBooking(
            site = selectedSite.get(),
            bookingId = bookingId
        )
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

    suspend fun fetchResources(): Result<Unit> {
        val result = bookingsStore.fetchResources(site = selectedSite.get())
        val model = result.model
        return if (result.isError || model == null) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(Unit)
        }
    }

    suspend fun getResource(
        resourceId: Long
    ): BookingResource? = bookingsStore.getResource(site = selectedSite.get(), resourceId = resourceId)

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

    fun observeResources(): Flow<List<BookingResource>> =
        bookingsStore.observeResources(site = selectedSite.get())

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
    ): Result<Unit> = appCoroutineScope.async {
        val result = bookingsStore.updateBooking(
            site = selectedSite.get(),
            bookingId = bookingId,
            bookingUpdatePayload = BookingUpdatePayload(note = note)
        )
        if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(Unit)
        }
    }.await()

    suspend fun cancelBooking(
        bookingId: Long,
    ): Result<Unit> = appCoroutineScope.async {
        val result = bookingsStore.updateBooking(
            site = selectedSite.get(),
            bookingId = bookingId,
            bookingUpdatePayload = BookingUpdatePayload(status = BookingEntity.Status.Cancelled),
            refreshOrder = true,
        )
        if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(Unit)
        }
    }.await()

    suspend fun fetchProductAvailability(
        productId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        resourceId: Long,
    ): Result<BookingAvailabilityDto> {
        val result = bookingsStore.fetchProductAvailability(
            site = selectedSite.get(),
            productId = productId,
            startDate = startDate,
            endDate = endDate,
            resourceId = resourceId,
        )
        val model = result.model
        return when {
            result.isError -> Result.failure(WooException(result.error))
            model != null -> Result.success(model)
            else -> Result.failure(WooException(WooError(GENERIC_ERROR, UNKNOWN)))
        }
    }

    suspend fun fetchProductBookingLocation(productId: Long, bookingId: Long? = null): Result<String?> {
        val result = bookingsStore.fetchProductBookingLocation(
            site = selectedSite.get(),
            productId = productId,
            bookingId = bookingId
        )
        return if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(result.model)
        }
    }

    suspend fun markAsPaid(
        bookingId: Long,
    ): Result<Unit> = appCoroutineScope.async {
        val result = bookingsStore.updateBooking(
            site = selectedSite.get(),
            bookingId = bookingId,
            bookingUpdatePayload = BookingUpdatePayload(status = BookingEntity.Status.Paid),
            refreshOrder = true,
        )
        if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(Unit)
        }
    }.await()

    data class FetchResult(
        val bookings: List<Booking>,
        val hasMorePages: Boolean
    )
}

typealias Booking = BookingEntity
typealias BookingResource = BookingResourceEntity
