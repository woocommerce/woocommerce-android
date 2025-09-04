package com.woocommerce.android.ui.woopos.bookings.data

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
        return apiService.fetchBookings(page = page, perPage = perPage)
    }
    
    suspend fun fetchBookingSlots(
        productIds: List<Int>,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<BookingSlot>> {
        return apiService.fetchBookingSlots(productIds, startDate, endDate)
    }
    
    suspend fun fetchBookingById(bookingId: Long): Result<WooPosBooking?> {
        return try {
            var currentPage = 1
            val perPage = 100
            
            do {
                val bookingsResult = fetchBookings(page = currentPage, perPage = perPage).getOrThrow()
                
                val foundBooking = bookingsResult.find { it.id == bookingId }
                if (foundBooking != null) {
                    return Result.success(foundBooking)
                }
                
                currentPage++
            } while (bookingsResult.size == perPage)
            
            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun confirmBooking(bookingId: Long): Result<WooPosBooking> {
        return fetchBookingById(bookingId).mapCatching { booking ->
            booking ?: throw Exception("Booking not found")
        }
    }
    
    fun loadBookingsForWeekStream(startDate: LocalDate, endDate: LocalDate): Flow<BookingsResult> = flow {
        emit(BookingsResult.Loading)
        
        try {
            val allBookings = mutableListOf<WooPosBooking>()
            var currentPage = 1
            val perPage = 100
            
            do {
                val bookingsResult = fetchBookings(page = currentPage, perPage = perPage).getOrThrow()
                allBookings.addAll(bookingsResult)
                currentPage++
            } while (bookingsResult.size == perPage)
            
            val weekBookings = allBookings.filter { booking ->
                val bookingDate = booking.startDateTime.toLocalDate()
                bookingDate in startDate..endDate
            }
            
            val slots = fetchBookingSlots(
                productIds = listOf(1), 
                startDate = startDate, 
                endDate = endDate
            ).getOrNull() ?: emptyList()
            
            emit(BookingsResult.Success(weekBookings, slots))
        } catch (e: Exception) {
            emit(BookingsResult.Error(e))
        }
    }
    
    sealed class BookingsResult {
        data object Loading : BookingsResult()
        data class Success(val bookings: List<WooPosBooking>, val slots: List<BookingSlot>) : BookingsResult()
        data class Error(val exception: Exception) : BookingsResult()
    }
}