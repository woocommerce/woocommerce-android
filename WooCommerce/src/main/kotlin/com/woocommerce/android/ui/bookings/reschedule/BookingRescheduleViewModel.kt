package com.woocommerce.android.ui.bookings.reschedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingAvailabilityDto
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class BookingRescheduleViewModel @Inject constructor(
    private val bookingsRepository: BookingsRepository,
    private val clock: Clock,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {

    private val navArgs: BookingRescheduleFragmentArgs by savedState.navArgs()

    private val _state = MutableStateFlow<BookingRescheduleState>(BookingRescheduleState.Loading)
    val state: LiveData<BookingRescheduleState> = _state.asLiveData()

    init {
        loadAvailability()
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun loadAvailability() {
        launch {
            _state.update { BookingRescheduleState.Loading }

            val booking = bookingsRepository.getBooking(navArgs.bookingId)
            if (booking == null) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic))
                triggerEvent(MultiLiveEvent.Event.Exit)
                return@launch
            }

            val productId = booking.productId
            if (productId == 0L) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic))
                triggerEvent(MultiLiveEvent.Event.Exit)
                return@launch
            }

            val resourceId = booking.resourceId
            val (startDate, endDate) = buildDateRange(booking)

            bookingsRepository.fetchProductAvailability(
                productId = productId,
                startDate = startDate,
                endDate = endDate,
                resourceId = resourceId,
            ).onSuccess { availability ->
                _state.update {
                    BookingRescheduleState.Content(availability = availability)
                }
            }.onFailure {
                _state.update { BookingRescheduleState.Error }
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic))
            }
        }
    }

    private fun buildDateRange(booking: Booking): Pair<LocalDateTime, LocalDateTime> {
        val today = LocalDate.now(clock)
        val bookingDate = booking.start.atOffset(ZoneOffset.UTC).toLocalDate()
        val effectiveDate = maxOf(bookingDate, today)
        val monthStart = effectiveDate.withDayOfMonth(1)
        val rangeStartDate = maxOf(monthStart, today)
        val startDate = if (rangeStartDate == today) {
            LocalDateTime.now(clock)
        } else {
            rangeStartDate.atStartOfDay()
        }
        val endDate = effectiveDate.withDayOfMonth(effectiveDate.lengthOfMonth())
            .atTime(LocalTime.MAX)
        return startDate to endDate
    }
}

sealed interface BookingRescheduleState {
    data object Loading : BookingRescheduleState
    data object Error : BookingRescheduleState
    data class Content(
        val availability: BookingAvailabilityDto,
    ) : BookingRescheduleState
}
