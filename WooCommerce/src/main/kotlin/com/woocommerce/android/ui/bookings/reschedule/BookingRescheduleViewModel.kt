package com.woocommerce.android.ui.bookings.reschedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.clock
import com.woocommerce.android.tools.SelectedSite
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingAvailabilityDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingRescheduleViewModel @Inject constructor(
    private val bookingsRepository: BookingsRepository,
    private val clock: Clock,
    selectedSite: SelectedSite,
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {

    private val storeZoneId = selectedSite.get().clock.zone

    private val navArgs: BookingRescheduleFragmentArgs by savedState.navArgs()

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
        .withZone(ZoneOffset.UTC)

    private val teamMemberIdOverride = MutableStateFlow<Long?>(null)
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val availabilityState = MutableStateFlow<BookingRescheduleState.AvailabilityState>(
        BookingRescheduleState.AvailabilityState.Loading
    )
    private val datePickerState = MutableStateFlow<BookingRescheduleState.DatePickerState?>(null)

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

    private val effectiveDate: Flow<LocalDate> = combine(
        booking,
        selectedDate,
    ) { booking, override ->
        override ?: booking.start.atOffset(ZoneOffset.UTC).toLocalDate()
    }.distinctUntilChanged()

    private val dateUiState = combine(
        effectiveDate,
        datePickerState,
    ) { date, picker ->
        date to picker
    }

    val state: LiveData<BookingRescheduleState> = combine(
        booking.map { it.productId }.distinctUntilChanged(),
        effectiveResourceId,
        teamMember,
        availabilityState,
        dateUiState,
    ) { productId, resourceId, member, availability, (date, picker) ->
        BookingRescheduleState(
            teamMemberId = resourceId,
            teamMemberName = member?.name,
            productId = productId,
            availabilityState = availability,
            formattedDate = formatDate(date),
            datePickerState = picker,
        )
    }.asLiveData()

    init {
        launch {
            combine(
                booking.map { it.productId },
                effectiveResourceId,
                effectiveDate,
            ) { productId, resourceId, date ->
                AvailabilityFetchKey(
                    productId = productId,
                    resourceId = resourceId,
                    month = YearMonth.from(date),
                )
            }.distinctUntilChanged()
                .collectLatest { key ->
                    loadAvailability(
                        productId = key.productId,
                        resourceId = key.resourceId,
                        dateRange = buildDateRange(key.month),
                    )
                }
        }
        launch {
            booking.collect { booking ->
                if (selectedDate.value == null) {
                    val initialDate = booking.start.atOffset(ZoneOffset.UTC).toLocalDate()
                    selectedDate.value = initialDate
                    datePickerState.value = buildDatePickerState(initialDate)
                }
            }
        }
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onDateRowClicked() {
        datePickerState.value = buildDatePickerState(selectedDate.value)
    }

    private fun onDateSelected(dateMillis: Long) {
        datePickerState.value = null
        selectedDate.value = Instant.ofEpochMilli(dateMillis)
            .atOffset(ZoneOffset.UTC)
            .toLocalDate()
    }

    private fun onDatePickerDismissed() {
        datePickerState.value = null
    }

    private fun storeToday(): LocalDate = Instant.now(clock).atZone(storeZoneId).toLocalDate()

    private fun storeNow(): LocalDateTime = Instant.now(clock).atZone(storeZoneId).toLocalDateTime()

    private fun buildDatePickerState(currentDate: LocalDate?): BookingRescheduleState.DatePickerState {
        val today = storeToday()
        return BookingRescheduleState.DatePickerState(
            currentDateMillis = currentDate?.toStartOfDayUtcMillis(),
            minDateMillis = today.toStartOfDayUtcMillis(),
            onDateSelected = ::onDateSelected,
            onDismiss = ::onDatePickerDismissed,
        )
    }

    private fun LocalDate.toStartOfDayUtcMillis(): Long =
        atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun formatDate(date: LocalDate): String {
        return dateFormatter.format(date.atStartOfDay(ZoneOffset.UTC).toInstant())
    }

    fun onTeamMemberChanged(newResourceId: Long) {
        teamMemberIdOverride.value = newResourceId
    }

    private suspend fun loadAvailability(
        productId: Long,
        resourceId: Long,
        dateRange: Pair<LocalDateTime, LocalDateTime>,
    ) {
        availabilityState.value = BookingRescheduleState.AvailabilityState.Loading
        bookingsRepository.fetchProductAvailability(
            productId = productId,
            startDate = dateRange.first,
            endDate = dateRange.second,
            resourceId = resourceId,
        ).onSuccess { availability ->
            availabilityState.value = BookingRescheduleState.AvailabilityState.Loaded(availability)
        }.onFailure {
            availabilityState.value = BookingRescheduleState.AvailabilityState.Error
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.error_generic))
        }
    }

    private fun buildDateRange(month: YearMonth): Pair<LocalDateTime, LocalDateTime> {
        val today = storeToday()
        val effectiveMonth = maxOf(month, YearMonth.from(today))
        val rangeStartDate = maxOf(effectiveMonth.atDay(1), today)
        val startDate = if (rangeStartDate == today) {
            storeNow()
        } else {
            rangeStartDate.atStartOfDay()
        }
        val endDate = effectiveMonth.atEndOfMonth().atTime(LocalTime.MAX)
        return startDate to endDate
    }

    private data class AvailabilityFetchKey(
        val productId: Long,
        val resourceId: Long,
        val month: YearMonth,
    )
}

data class BookingRescheduleState(
    val teamMemberId: Long = 0L,
    val teamMemberName: String? = null,
    val productId: Long = 0L,
    val availabilityState: AvailabilityState = AvailabilityState.Loading,
    val formattedDate: String = "",
    val datePickerState: DatePickerState? = null,
) {
    sealed interface AvailabilityState {
        data object Loading : AvailabilityState
        data object Error : AvailabilityState
        data class Loaded(val availability: BookingAvailabilityDto) : AvailabilityState
    }

    data class DatePickerState(
        val currentDateMillis: Long?,
        val minDateMillis: Long?,
        val onDateSelected: (Long) -> Unit,
        val onDismiss: () -> Unit,
    )
}
