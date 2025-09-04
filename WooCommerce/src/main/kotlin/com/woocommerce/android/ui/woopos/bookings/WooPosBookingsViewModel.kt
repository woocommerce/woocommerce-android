package com.woocommerce.android.ui.woopos.bookings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.bookings.data.WooPosBookingsDataSource
import com.woocommerce.android.ui.woopos.bookings.data.WooPosBookingsRepository
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WooPosBookingsViewModel @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val bookingsRepository: WooPosBookingsRepository,
) : ViewModel() {

    private val _selectedWeekStart = MutableStateFlow(LocalDate.now().with(java.time.DayOfWeek.MONDAY))
    val selectedWeekStart: StateFlow<LocalDate> = _selectedWeekStart.asStateFlow()
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val bookingsForWeek: Flow<WooPosBookingsDataSource.BookingsResult> = selectedWeekStart
        .flatMapLatest { weekStart ->
            val weekEnd = weekStart.plusDays(6)
            Log.d(TAG, "bookingsForWeek flatMapLatest: weekStart=$weekStart, weekEnd=$weekEnd")
            bookingsRepository.loadBookingsForWeek(weekStart, weekEnd)
        }


    fun onWeekChanged(weekStart: LocalDate) {
        val newWeekStart = weekStart.with(java.time.DayOfWeek.MONDAY)
        Log.d(TAG, "onWeekChanged: from ${_selectedWeekStart.value} to $newWeekStart")
        _selectedWeekStart.value = newWeekStart
    }

    fun onBookingPaymentClick(booking: WooPosBooking) {
        Log.d(TAG, "onBookingPaymentClick: bookingId=${booking.id}, productId=${booking.productId}")
        viewModelScope.launch {
            val bookingItem = WooPosItemsViewModel.ItemClickedData.Product.Simple(booking.productId.toLong())
            
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = bookingItem,
                    eventForTracking = null
                )
            )
        }
    }

    suspend fun confirmBooking(bookingId: Long): Result<WooPosBooking> {
        Log.d(TAG, "confirmBooking: bookingId=$bookingId")
        return bookingsRepository.confirmBooking(bookingId)
    }
    
    companion object {
        private const val TAG = "WooPosBookingsViewModel"
    }
}
