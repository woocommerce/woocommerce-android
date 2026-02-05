package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingUpdatePayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsOrderOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsRestClient
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class WooPosBookingsDataSource @Inject constructor(
    private val restClient: BookingsRestClient,
    private val selectedSite: SelectedSite,
) {
    companion object {
        const val PAGE_SIZE = 25
    }

    suspend fun fetchBookings(tab: BookingTab, page: Int): Result<FetchBookingsResult> {
        val (filters, order) = buildFiltersForTab(tab)
        val response = restClient.fetchBookings(
            site = selectedSite.get(),
            perPage = PAGE_SIZE,
            page = page,
            query = null,
            filters = filters,
            order = order,
        )
        return if (response.isError) {
            Result.failure(Throwable(response.error?.message ?: "Unknown error"))
        } else {
            val bookings = response.result?.toList() ?: emptyList()
            val filtered = if (tab == BookingTab.Canceled) {
                bookings.filter { it.status == BookingEntity.Status.Cancelled.key }
            } else {
                bookings
            }
            Result.success(
                FetchBookingsResult(
                    bookings = filtered,
                    hasMorePages = bookings.size >= PAGE_SIZE,
                )
            )
        }
    }

    suspend fun fetchBooking(bookingId: Long): Result<BookingDto> {
        val response = restClient.fetchBooking(
            site = selectedSite.get(),
            bookingId = bookingId,
        )
        return if (response.isError) {
            Result.failure(Throwable(response.error?.message ?: "Unknown error"))
        } else {
            response.result?.let { Result.success(it) }
                ?: Result.failure(Throwable("Booking not found"))
        }
    }

    suspend fun cancelBooking(bookingId: Long): Result<BookingDto> {
        return updateBooking(
            bookingId,
            BookingUpdatePayload(status = BookingEntity.Status.Cancelled)
        )
    }

    suspend fun updateAttendanceStatus(
        bookingId: Long,
        status: BookingEntity.AttendanceStatus
    ): Result<BookingDto> {
        return updateBooking(
            bookingId,
            BookingUpdatePayload(attendanceStatus = status)
        )
    }

    suspend fun markAsPaid(bookingId: Long): Result<BookingDto> {
        return updateBooking(
            bookingId,
            BookingUpdatePayload(status = BookingEntity.Status.Paid)
        )
    }

    private suspend fun updateBooking(
        bookingId: Long,
        payload: BookingUpdatePayload
    ): Result<BookingDto> {
        val response = restClient.updateBooking(
            site = selectedSite.get(),
            bookingId = bookingId,
            payload = payload,
        )
        return if (response.isError) {
            Result.failure(Throwable(response.error?.message ?: "Unknown error"))
        } else {
            response.result?.let { Result.success(it) }
                ?: Result.failure(Throwable("Update failed"))
        }
    }

    private fun buildFiltersForTab(tab: BookingTab): Pair<BookingFilters?, BookingsOrderOption> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfDay = today.atStartOfDay(zone).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()

        return when (tab) {
            BookingTab.Today -> {
                val filters = BookingFilters(
                    dateRange = BookingsFilterOption.DateRange(
                        after = startOfDay,
                        before = endOfDay,
                    )
                )
                filters to BookingsOrderOption.ASC
            }
            BookingTab.Upcoming -> {
                val filters = BookingFilters(
                    dateRange = BookingsFilterOption.DateRange(
                        before = null,
                        after = endOfDay,
                    )
                )
                filters to BookingsOrderOption.ASC
            }
            BookingTab.Canceled -> {
                null to BookingsOrderOption.DESC
            }
            BookingTab.All -> {
                null to BookingsOrderOption.DESC
            }
        }
    }
}

data class FetchBookingsResult(
    val bookings: List<BookingDto>,
    val hasMorePages: Boolean,
)

enum class BookingTab {
    Today, Upcoming, Canceled, All
}
