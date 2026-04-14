package com.woocommerce.android.ui.bookings.reschedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.Booking
import com.woocommerce.android.ui.bookings.BookingResource
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingAvailabilityDto
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingRescheduleViewModel @Inject constructor(
    private val bookingsRepository: BookingsRepository,
    private val clock: Clock,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {

    private val navArgs: BookingRescheduleFragmentArgs by savedState.navArgs()

    private val _state = MutableStateFlow(BookingRescheduleState())
    val state: LiveData<BookingRescheduleState> = _state.asLiveData()

    private val teamMemberIdOverride = MutableStateFlow<Long?>(null)

    private val booking: Flow<Booking> = flow {
        emit(bookingsRepository.getBooking(navArgs.bookingId))
    }.onEach { booking ->
        if (booking == null || booking.productId == 0L) {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic))
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }.filterNotNull()
        .filter { it.productId != 0L }
        .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    private val effectiveResourceId: Flow<Long> = combine(
        booking,
        teamMemberIdOverride
    ) { booking, override ->
        override ?: booking.resourceId
    }.distinctUntilChanged()

    private val teamMember: Flow<BookingResource?> = effectiveResourceId
        .flatMapLatest { bookingsRepository.observeResource(it) }

    init {
        launch {
            combine(booking, effectiveResourceId) { booking, resourceId ->
                AvailabilityFetchParams(
                    productId = booking.productId,
                    resourceId = resourceId,
                    dateRange = buildDateRange(booking),
                )
            }.collectLatest { params ->
                _state.update {
                    it.copy(teamMemberId = params.resourceId, productId = params.productId)
                }
                loadAvailability(params)
            }
        }
        launch {
            teamMember.collect { member ->
                _state.update { it.copy(teamMemberName = member?.name) }
            }
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private suspend fun loadAvailability(params: AvailabilityFetchParams) {
        _state.update { it.copy(availabilityState = BookingRescheduleState.AvailabilityState.Loading) }
        bookingsRepository.fetchProductAvailability(
            productId = params.productId,
            startDate = params.dateRange.first,
            endDate = params.dateRange.second,
            resourceId = params.resourceId,
        ).onSuccess { availability ->
            _state.update {
                it.copy(availabilityState = BookingRescheduleState.AvailabilityState.Loaded(availability))
            }
        }.onFailure {
            _state.update { it.copy(availabilityState = BookingRescheduleState.AvailabilityState.Error) }
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic))
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

    private data class AvailabilityFetchParams(
        val productId: Long,
        val resourceId: Long,
        val dateRange: Pair<LocalDateTime, LocalDateTime>,
    )
}

data class BookingRescheduleState(
    val teamMemberId: Long = 0L,
    val teamMemberName: String? = null,
    val productId: Long = 0L,
    val availabilityState: AvailabilityState = AvailabilityState.Loading,
) {
    sealed interface AvailabilityState {
        data object Loading : AvailabilityState
        data object Error : AvailabilityState
        data class Loaded(val availability: BookingAvailabilityDto) : AvailabilityState
    }
}
