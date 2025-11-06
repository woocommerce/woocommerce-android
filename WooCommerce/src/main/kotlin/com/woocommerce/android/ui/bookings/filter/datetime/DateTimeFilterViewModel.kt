package com.woocommerce.android.ui.bookings.filter.datetime

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.bookings.filter.datetime.PickerDialogState.DateDialog
import com.woocommerce.android.ui.bookings.filter.datetime.PickerDialogState.TimeDialog
import com.woocommerce.android.ui.compose.component.Time
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class DateTimeFilterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clock: Clock,
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
                        formattedFromDate = dateFormatter.format(newDateTime),
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
                        formattedToDate = dateFormatter.format(newDateTime),
                        pickerDialogState = null,
                    )
                }
            }
        }
    }

    private fun commitSelectedTime(dateBoundary: DateBoundary, time: Time) {
        _uiState.update { current ->
            when (dateBoundary) {
                DateBoundary.FROM -> {
                    val newDateTime = (current.fromDateTime ?: LocalDateTime.now(clock)).withTime(time)
                    current.copy(
                        fromDateTime = newDateTime,
                        formattedFromTime = timeFormatter.format(newDateTime),
                        formattedFromDate = dateFormatter.format(newDateTime),
                        pickerDialogState = null,
                    )
                }

                DateBoundary.TO -> {
                    val newDateTime = (current.toDateTime ?: LocalDateTime.now(clock)).withTime(time = time)
                    current.copy(
                        toDateTime = newDateTime,
                        formattedToDate = dateFormatter.format(newDateTime),
                        formattedToTime = timeFormatter.format(newDateTime),
                        pickerDialogState = null,
                    )
                }
            }
        }
    }
}

private fun LocalDateTime.withTime(time: Time): LocalDateTime = this
    .withHour(time.hour)
    .withMinute(time.minute)
    .withSecond(0)
    .withNano(0)
