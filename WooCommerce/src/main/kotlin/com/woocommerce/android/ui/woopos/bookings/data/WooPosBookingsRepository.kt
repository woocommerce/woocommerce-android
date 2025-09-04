package com.woocommerce.android.ui.woopos.bookings.data

import com.woocommerce.android.ui.woopos.bookings.WooPosBooking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosBookingsRepository @Inject constructor(
    private val dataSource: WooPosBookingsDataSource
) {
    
    fun loadBookingsForWeek(startDate: LocalDate, endDate: LocalDate): Flow<WooPosBookingsDataSource.BookingsResult> {
        return dataSource.loadBookingsForWeekStream(startDate, endDate).flowOn(Dispatchers.IO)
    }
    
    suspend fun getBookingById(bookingId: Long): Result<WooPosBooking?> = withContext(Dispatchers.IO) {
        dataSource.fetchBookingById(bookingId)
    }
    
    suspend fun confirmBooking(bookingId: Long): Result<WooPosBooking> = withContext(Dispatchers.IO) {
        dataSource.confirmBooking(bookingId)
    }
}