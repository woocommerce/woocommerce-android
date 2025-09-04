package com.woocommerce.android.ui.woopos.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.bookings.data.WooPosBookingsDataSource
import com.woocommerce.android.ui.woopos.bookings.data.WooPosBookingsRepository
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun loadBookingsForWeek(weekStart: LocalDate): Flow<WooPosBookingsDataSource.BookingsResult> {
        val weekEnd = weekStart.plusDays(6)
        return bookingsRepository.loadBookingsForWeek(weekStart, weekEnd)
    }

    fun onWeekChanged(weekStart: LocalDate) {
        _selectedWeekStart.value = weekStart.with(java.time.DayOfWeek.MONDAY)
    }

    fun onBookingPaymentClick(booking: WooPosBooking) {
        viewModelScope.launch {
            val bookingItem = WooPosItemsViewModel.ItemClickedData.Product.Simple(booking.id)
            
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInItemsList(
                    itemData = bookingItem,
                    eventForTracking = null
                )
            )
        }
    }

    suspend fun confirmBooking(bookingId: Long): Result<WooPosBooking> {
        return bookingsRepository.confirmBooking(bookingId)
    }
}
