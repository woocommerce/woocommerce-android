package com.woocommerce.android.ui.bookings.filter.datetime

import com.woocommerce.android.ui.compose.component.Time
import java.time.LocalDateTime

data class DateTimeFilterUiState(
    val fromDateTime: LocalDateTime? = null,
    val toDateTime: LocalDateTime? = null,
    val formattedFromDate: String = "",
    val formattedFromTime: String = "",
    val formattedToDate: String = "",
    val formattedToTime: String = "",
    val pickerDialogState: PickerDialogState? = null,
    val onTimeClick: (DateBoundary) -> Unit = {},
    val onDateClick: (DateBoundary) -> Unit = {},
    val onClearClick: (DateBoundary) -> Unit = {},
)

sealed interface PickerDialogState {
    data class DateDialog(
        val date: Long?,
        val minDate: Long? = null,
        val maxDate: Long? = null,
        val onDateSelected: (Long) -> Unit,
        val onDismiss: () -> Unit,
    ) : PickerDialogState

    data class TimeDialog(
        val time: Time?,
        val onTimeSelected: (Time) -> Unit,
        val onDismiss: () -> Unit,
    ) : PickerDialogState
}

enum class DateBoundary {
    FROM, TO
}
