package com.woocommerce.android.ui.bookings.filter.type

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.filter.BookingFilterListItem

sealed interface BookingFilterType {
    data object Any : BookingFilterType
    data object Service : BookingFilterType
    data object Event : BookingFilterType
}

data class BookingTypeFilterUiState(
    val selectedType: BookingFilterType = BookingFilterType.Any,
    val onTypeSelected: (BookingFilterType) -> Unit = {},
) {
    val items: List<BookingFilterListItem> = availableBookingTypes().map { type ->
        BookingFilterListItem(
            title = type.titleRes,
            value = type.filterValue,
            onClick = { onTypeSelected(type) }
        )
    }

    val BookingFilterType.titleRes: Int
        @StringRes get() = when (this) {
            BookingFilterType.Any -> R.string.bookings_filter_default
            BookingFilterType.Service -> R.string.bookings_filter_type_service
            BookingFilterType.Event -> R.string.bookings_filter_type_event
        }

    private fun availableBookingTypes(): List<BookingFilterType> = listOf(
        BookingFilterType.Any,
        BookingFilterType.Service,
        BookingFilterType.Event,
    )
}

val BookingFilterType.filterValue: String?
    // TODO Update this with actual endpoint values
    get() = when (this) {
        BookingFilterType.Service -> "service"
        BookingFilterType.Event -> "event"
        BookingFilterType.Any -> null
    }
