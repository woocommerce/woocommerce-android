package com.woocommerce.android.ui.bookings

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsStore
import javax.inject.Inject

class BookingsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val bookingsStore: BookingsStore
) {
    suspend fun fetchBookings(
        page: Int,
        perPage: Int
    ): Result<Boolean> {
        val result = bookingsStore.fetchBookings(
            site = selectedSite.get(),
            page = page,
            perPage = perPage
        )
        return if (result.isError) {
            Result.failure(WooException(result.error))
        } else {
            Result.success(result.model!!)
        }
    }

    fun observeBookings(limit: Int? = null): Flow<List<Booking>> =
        bookingsStore.observeBookings(selectedSite.get(), limit)
}

typealias Booking = org.wordpress.android.fluxc.persistence.entity.BookingEntity
