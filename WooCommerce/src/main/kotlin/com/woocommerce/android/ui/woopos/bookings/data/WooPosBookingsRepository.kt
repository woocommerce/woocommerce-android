package com.woocommerce.android.ui.woopos.bookings.data

import android.util.Log
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
        Log.d(TAG, "loadBookingsForWeek: startDate=$startDate, endDate=$endDate")
        return dataSource.loadBookingsForWeekStream(startDate, endDate).flowOn(Dispatchers.IO)
    }
    
    suspend fun getBookingById(bookingId: Long): Result<WooPosBooking?> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getBookingById: bookingId=$bookingId")
        dataSource.fetchBookingById(bookingId)
    }
    
    suspend fun confirmBooking(bookingId: Long): Result<WooPosBooking> = withContext(Dispatchers.IO) {
        Log.d(TAG, "confirmBooking: bookingId=$bookingId")
        dataSource.confirmBooking(bookingId)
    }
    
    companion object {
        private const val TAG = "WooPosBookingsRepository"
    }
}