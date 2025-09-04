package com.woocommerce.android.ui.woopos.bookings.data

import android.util.Log
import com.woocommerce.android.ui.woopos.bookings.WooPosBooking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosBookingsDataSource @Inject constructor(
    private val apiService: WooPosBookingsApiService
) {
    
    suspend fun fetchBookings(page: Int = 1, perPage: Int = 50): Result<List<WooPosBooking>> {
        Log.d(TAG, "fetchBookings: page=$page, perPage=$perPage")
        val result = apiService.fetchBookings(page = page, perPage = perPage)
        Log.d(TAG, "fetchBookings result: success=${result.isSuccess}, size=${result.getOrNull()?.size}")
        return result
    }
    
    suspend fun fetchBookingSlots(
        productIds: List<Int>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<BookingSlot>> {
        return apiService.fetchBookingSlots(productIds, startDate, endDate)
    }
    
    suspend fun fetchBookingById(bookingId: Long): Result<WooPosBooking?> {
        Log.d(TAG, "fetchBookingById: bookingId=$bookingId")
        return try {
            var currentPage = 1
            val perPage = 100
            
            do {
                Log.d(TAG, "fetchBookingById: fetching page $currentPage")
                val bookingsResult = fetchBookings(page = currentPage, perPage = perPage).getOrThrow()
                
                val foundBooking = bookingsResult.find { it.id == bookingId }
                if (foundBooking != null) {
                    Log.d(TAG, "fetchBookingById: found booking with id $bookingId")
                    return Result.success(foundBooking)
                }
                
                currentPage++
            } while (bookingsResult.size == perPage)
            
            Log.d(TAG, "fetchBookingById: booking with id $bookingId not found")
            Result.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "fetchBookingById: error fetching booking $bookingId", e)
            Result.failure(e)
        }
    }
    
    suspend fun confirmBooking(bookingId: Long): Result<WooPosBooking> {
        Log.d(TAG, "confirmBooking: bookingId=$bookingId")
        return try {
            // Call the API to confirm the booking
            val result = apiService.confirmBooking(bookingId)
            if (result.isSuccess) {
                val confirmedBooking = result.getOrThrow()
                Log.d(TAG, "confirmBooking: Successfully confirmed booking $bookingId, new status: ${confirmedBooking.status}")
                
                // Update the booking in cache if it exists
                // Note: In a full implementation, we might want to update the cache here
                // For now, the next data refresh will pick up the updated status
                
                Result.success(confirmedBooking)
            } else {
                Log.e(TAG, "confirmBooking: Failed to confirm booking $bookingId")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "confirmBooking: Exception confirming booking $bookingId", e)
            Result.failure(e)
        }
    }
    
    fun loadBookingsForWeekStream(startDate: LocalDate, endDate: LocalDate): Flow<BookingsResult> = flow {
        val flowId = System.currentTimeMillis()
        Log.d(TAG, "loadBookingsForWeekStream [$flowId]: START - startDate=$startDate, endDate=$endDate")
        emit(BookingsResult.Loading)
        
        try {
            val allBookings = mutableListOf<WooPosBooking>()
            var currentPage = 1
            val perPage = 100
            
            Log.d(TAG, "loadBookingsForWeekStream [$flowId]: Starting to fetch all bookings")
            do {
                Log.d(TAG, "loadBookingsForWeekStream [$flowId]: Fetching page $currentPage")
                val bookingsResult = fetchBookings(page = currentPage, perPage = perPage).getOrThrow()
                allBookings.addAll(bookingsResult)
                Log.d(TAG, "loadBookingsForWeekStream [$flowId]: Page $currentPage returned ${bookingsResult.size} bookings, total so far: ${allBookings.size}")
                currentPage++
            } while (bookingsResult.size == perPage)
            
            Log.d(TAG, "loadBookingsForWeekStream [$flowId]: Finished fetching all bookings, total: ${allBookings.size}")
            
            val weekBookings = allBookings.filter { booking ->
                val bookingDate = booking.startDateTime.toLocalDate()
                bookingDate in startDate..endDate
            }
            
            Log.d(TAG, "loadBookingsForWeekStream [$flowId]: Filtered to ${weekBookings.size} bookings for the week")
            
            Log.d(TAG, "loadBookingsForWeekStream [$flowId]: Fetching booking slots")
            val slots = fetchBookingSlots(
                productIds = listOf(1), 
                startDate = startDate, 
                endDate = endDate
            ).getOrNull() ?: emptyList()
            
            Log.d(TAG, "loadBookingsForWeekStream [$flowId]: Got ${slots.size} slots")
            
            Log.d(TAG, "loadBookingsForWeekStream [$flowId]: Emitting SUCCESS result")
            emit(BookingsResult.Success(weekBookings, slots))
            Log.d(TAG, "loadBookingsForWeekStream [$flowId]: END")
        } catch (e: Exception) {
            Log.e(TAG, "loadBookingsForWeekStream [$flowId]: ERROR", e)
            emit(BookingsResult.Error(e))
        }
    }
    
    sealed class BookingsResult {
        data object Loading : BookingsResult()
        data class Success(val bookings: List<WooPosBooking>, val slots: List<BookingSlot>) : BookingsResult()
        data class Error(val exception: Exception) : BookingsResult()
    }
    
    companion object {
        private const val TAG = "WooPosBookingsDataSource"
    }
}