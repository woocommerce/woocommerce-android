package com.woocommerce.android.ui.bookings.filter.datetime

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.filter.datetime.PickerDialogState.DateDialog
import com.woocommerce.android.ui.bookings.filter.datetime.PickerDialogState.TimeDialog
import com.woocommerce.android.ui.compose.component.Time
import com.woocommerce.android.util.DateFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@HiltViewModel(assistedFactory = DateTimeFilterViewModel.Factory::class)
class DateTimeFilterViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
    private val dateFormatter: DateFormatter,
    @Assisted private val initialRange: BookingsFilterOption.DateRange? = null,
    @Assisted private val onTypeFilterChanged: (BookingsFilterOption.DateRange) -> Unit,
) : ScopedViewModel(savedStateHandle) {

    private val zone: ZoneId = ZoneOffset.UTC

    private val _uiState = MutableStateFlow(
        DateTimeFilterUiState(
            onDateClick = { openDateDialog(it) },
            onTimeClick = { openTimeDialog(it) },
            onClearClick = { clearSelectedDate(it) },
        )
    )
    val uiState: LiveData<DateTimeFilterUiState?> = _uiState.asLiveData()

    init {
        initialRange?.let { range ->
            _uiState.update { currentState ->
                val fromDateTime = range.after?.atZone(zone)?.toLocalDateTime()
                val toDateTime = range.before?.atZone(zone)?.toLocalDateTime()
                currentState.copy(
                    fromDateTime = fromDateTime,
                    formattedFromDate = fromDateTime.formatDate(),
                    formattedFromTime = fromDateTime.formatTime(),
                    toDateTime = toDateTime,
                    formattedToDate = toDateTime.formatDate(),
                    formattedToTime = toDateTime.formatTime(),
                )
            }
        }
    }

    private fun openDateDialog(dateBoundary: DateBoundary) {
        _uiState.update { currentState ->
            val fromDate = currentState.fromDateTime
                ?.toLocalDate()
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
            val toDate = currentState.toDateTime
                ?.toLocalDate()
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()

            val dialogState = when (dateBoundary) {
                DateBoundary.FROM -> {
                    DateDialog(
                        // Convert to UTC for the Date picker
                        date = fromDate?.toEpochMilli(),
                        maxDate = toDate?.toEpochMilli(),
                        onDismiss = ::dismissPickerDialog,
                        onDateSelected = { commitSelectedDate(dateBoundary, it) },
                    )
                }

                DateBoundary.TO -> {
                    DateDialog(
                        // Convert to UTC for the Date picker
                        date = toDate?.toEpochMilli(),
                        minDate = fromDate?.toEpochMilli(),
                        onDismiss = ::dismissPickerDialog,
                        onDateSelected = { commitSelectedDate(dateBoundary, it) },
                    )
                }
            }
            currentState.copy(
                pickerDialogState = dialogState
            )
        }
    }

    private fun openTimeDialog(dateBoundary: DateBoundary) {
        _uiState.update { currentState ->
            val localDateTime = when (dateBoundary) {
                DateBoundary.FROM -> currentState.fromDateTime
                DateBoundary.TO -> currentState.toDateTime
            }
            val dialogState = TimeDialog(
                time = localDateTime?.let {
                    Time(
                        hour = it.hour,
                        minute = it.minute,
                    )
                },
                onDismiss = ::dismissPickerDialog,
                onTimeSelected = {
                    commitSelectedTime(dateBoundary, it)
                }
            )
            currentState.copy(pickerDialogState = dialogState)
        }
    }

    private fun clearSelectedDate(dateBoundary: DateBoundary) {
        _uiState.update { currentState ->
            when (dateBoundary) {
                DateBoundary.FROM -> {
                    currentState.copy(
                        fromDateTime = null,
                        formattedFromDate = "",
                        formattedFromTime = "",
                    )
                }

                DateBoundary.TO -> {
                    currentState.copy(
                        toDateTime = null,
                        formattedToDate = "",
                        formattedToTime = "",
                    )
                }
            }
        }
        emitTypeFilterChange()
    }

    private fun dismissPickerDialog() {
        _uiState.update { it.copy(pickerDialogState = null) }
    }

    private fun commitSelectedDate(dateBoundary: DateBoundary, date: Long) {
        _uiState.update { current ->
            when (dateBoundary) {
                DateBoundary.FROM -> {
                    // Convert from UTC to local zone
                    val picked = Instant.ofEpochMilli(date).atZone(ZoneOffset.UTC).toLocalDate()
                    val newDateTime = (current.fromDateTime ?: picked.atStartOfDay())
                        .withYear(picked.year)
                        .withMonth(picked.monthValue)
                        .withDayOfMonth(picked.dayOfMonth)
                    current.copy(
                        fromDateTime = newDateTime,
                        formattedFromDate = newDateTime.formatDate(),
                        formattedFromTime = newDateTime.formatTime(),
                        pickerDialogState = null,
                    )
                }

                DateBoundary.TO -> {
                    // Convert from UTC to local zone
                    val picked = Instant.ofEpochMilli(date).atZone(ZoneOffset.UTC).toLocalDate()
                    val newDateTime = (current.toDateTime ?: picked.atTime(LocalTime.MAX))
                        .withYear(picked.year)
                        .withMonth(picked.monthValue)
                        .withDayOfMonth(picked.dayOfMonth)
                    current.copy(
                        toDateTime = newDateTime,
                        formattedToDate = newDateTime.formatDate(),
                        formattedToTime = newDateTime.formatTime(),
                        pickerDialogState = null,
                    )
                }
            }
        }
        emitTypeFilterChange()
    }

    private fun commitSelectedTime(dateBoundary: DateBoundary, time: Time) {
        _uiState.update { current ->
            when (dateBoundary) {
                DateBoundary.FROM -> {
                    val newDateTime = (current.fromDateTime ?: LocalDateTime.now(clock)).withTime(time)
                    val to = current.toDateTime
                    if (to != null && newDateTime.isAfter(to)) {
                        // If the new FROM is after TO, swap them
                        triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_filter_date_time_swap_info))
                        current.copyWithDates(
                            fromDate = to,
                            toDate = newDateTime,
                        )
                    } else {
                        current.copyWithDates(
                            fromDate = newDateTime,
                            toDate = to,
                        )
                    }
                }

                DateBoundary.TO -> {
                    val newDateTime = (current.toDateTime ?: LocalDateTime.now(clock)).withTime(time = time)
                    val from = current.fromDateTime
                    if (from != null && newDateTime.isBefore(from)) {
                        // If the new TO is before FROM, swap them
                        triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.booking_filter_date_time_swap_info))
                        current.copyWithDates(
                            fromDate = newDateTime,
                            toDate = from,
                        )
                    } else {
                        current.copyWithDates(
                            fromDate = from,
                            toDate = newDateTime,
                        )
                    }
                }
            }
        }
        emitTypeFilterChange()
    }

    private fun emitTypeFilterChange() {
        onTypeFilterChanged(
            BookingsFilterOption.DateRange(
                after = _uiState.value.fromDateTime?.atZone(zone)?.toInstant(),
                before = _uiState.value.toDateTime?.atZone(zone)?.toInstant(),
            )
        )
    }

    private fun DateTimeFilterUiState.copyWithDates(
        fromDate: LocalDateTime?,
        toDate: LocalDateTime?
    ): DateTimeFilterUiState {
        return copy(
            fromDateTime = fromDate,
            formattedFromDate = fromDate.formatDate(),
            formattedFromTime = fromDate.formatTime(),
            toDateTime = toDate,
            formattedToDate = toDate.formatDate(),
            formattedToTime = toDate.formatTime(),
            pickerDialogState = null,
        )
    }

    private fun LocalDateTime?.formatDate(): String = this?.let { dateFormatter.formatDate(it) }.orEmpty()
    private fun LocalDateTime?.formatTime(): String = this?.let { dateFormatter.formatTime(it) }.orEmpty()

    @AssistedFactory
    interface Factory {
        fun create(
            onTypeFilterChanged: (BookingsFilterOption.DateRange) -> Unit,
            initialRange: BookingsFilterOption.DateRange? = null,
        ): DateTimeFilterViewModel
    }
}

private fun LocalDateTime.withTime(time: Time): LocalDateTime = this
    .withHour(time.hour)
    .withMinute(time.minute)
    .withSecond(0)
    .withNano(0)
