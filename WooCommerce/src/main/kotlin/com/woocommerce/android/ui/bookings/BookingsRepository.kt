package com.woocommerce.android.ui.bookings

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsStore
import org.wordpress.android.fluxc.persistence.entity.BookingEntity
import javax.inject.Inject

class BookingsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val bookingsStore: BookingsStore
) {
    suspend fun fetchBookings(
        page: Int,
        perPage: Int,
        filters: List<BookingsFilterOption> = emptyList()
    ): Result<Boolean> {
        val result = bookingsStore.fetchBookings(
            site = selectedSite.get(),
            perPage = perPage,
            page = page,
            filters = filters
        )
        return if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(result.model!!)
        }
    }

    fun observeBookings(
        limit: Int? = null,
        filters: List<BookingsFilterOption> = emptyList()
    ): Flow<List<Booking>> =
        bookingsStore.observeBookings(
            site = selectedSite.get(),
            limit = limit,
            filters = filters
        )
}

typealias Booking = BookingEntity
typealias BookingStatus = BookingEntity.Status
