package com.woocommerce.android.ui.bookings.filter.datetime

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.bookings.filter.datetime.PickerDialogState.DateDialog
import com.woocommerce.android.ui.bookings.filter.datetime.PickerDialogState.TimeDialog
import com.woocommerce.android.ui.compose.component.Time
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
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@HiltViewModel(assistedFactory = DateTimeFilterViewModel.Factory::class)
class DateTimeFilterViewModel @AssistedInject constructor(
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
    @Assisted private val initialRange: BookingsFilterOption.DateRange? = null,
    @Assisted private val onTypeFilterChanged: (BookingsFilterOption.DateRange) -> Unit,
) : ScopedViewModel(savedStateHandle) {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    private val _uiState = MutableStateFlow(
        DateTimeFilterUiState(
            onDateClick = { openDateDialog(it) },
            onTimeClick = { openTimeDialog(it) },
        )
    )
    val uiState: LiveData<DateTimeFilterUiState?> = _uiState.asLiveData()

    init {
        initialRange?.let { range ->
            _uiState.update { currentState ->
                val fromDateTime = range.after?.atZone(ZoneOffset.UTC)?.toLocalDateTime()
                val toDateTime = range.before?.atZone(ZoneOffset.UTC)?.toLocalDateTime()
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
            val dialogState = when (dateBoundary) {
                DateBoundary.FROM -> DateDialog(
                    date = currentState.fromDateTime?.atZone(zone)?.toInstant()?.toEpochMilli(),
                    onDismiss = ::dismissPickerDialog,
                    onDateSelected = { commitSelectedDate(dateBoundary, it) },
                )

                DateBoundary.TO -> DateDialog(
                    date = currentState.toDateTime?.atZone(zone)?.toInstant()?.toEpochMilli(),
                    onDismiss = ::dismissPickerDialog,
                    onDateSelected = { commitSelectedDate(dateBoundary, it) },
                )
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

    private fun dismissPickerDialog() {
        _uiState.update { it.copy(pickerDialogState = null) }
    }

    private fun commitSelectedDate(dateBoundary: DateBoundary, date: Long) {
        _uiState.update { current ->
            when (dateBoundary) {
                DateBoundary.FROM -> {
                    val picked = Instant.ofEpochMilli(date).atZone(zone).toLocalDateTime()
                    val newDateTime = (current.fromDateTime ?: picked)
                        .withYear(picked.year)
                        .withMonth(picked.monthValue)
                        .withDayOfMonth(picked.dayOfMonth)
                    current.copy(
                        fromDateTime = newDateTime,
                        formattedFromDate = newDateTime.formatDate(),
                        pickerDialogState = null,
                    )
                }

                DateBoundary.TO -> {
                    val picked = Instant.ofEpochMilli(date).atZone(zone).toLocalDateTime()
                    val newDateTime = (current.toDateTime ?: picked)
                        .withYear(picked.year)
                        .withMonth(picked.monthValue)
                        .withDayOfMonth(picked.dayOfMonth)
                    current.copy(
                        toDateTime = newDateTime,
                        formattedToDate = newDateTime.formatDate(),
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
                    current.copy(
                        fromDateTime = newDateTime,
                        formattedFromTime = newDateTime.formatTime(),
                        formattedFromDate = newDateTime.formatDate(),
                        pickerDialogState = null,
                    )
                }

                DateBoundary.TO -> {
                    val newDateTime = (current.toDateTime ?: LocalDateTime.now(clock)).withTime(time = time)
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

    private fun emitTypeFilterChange() {
        onTypeFilterChanged(
            BookingsFilterOption.DateRange(
                after = _uiState.value.fromDateTime?.toInstant(ZoneOffset.UTC),
                before = _uiState.value.toDateTime?.toInstant(ZoneOffset.UTC),
            )
        )
    }

    private fun LocalDateTime?.formatDate(): String = this?.let { dateFormatter.format(it) }.orEmpty()
    private fun LocalDateTime?.formatTime(): String = this?.let { timeFormatter.format(it) }.orEmpty()

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
