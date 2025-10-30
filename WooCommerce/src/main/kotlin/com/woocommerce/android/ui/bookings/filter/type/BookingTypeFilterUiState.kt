package com.woocommerce.android.ui.bookings.filter.type

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.filter.BookingFilterListItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.BookingType

data class BookingTypeFilterUiState(
    val selectedType: BookingType = BookingType.Any,
    val onTypeSelected: (BookingType) -> Unit = {},
) {
    val items: List<BookingFilterListItem> = availableBookingTypes().map { type ->
        BookingFilterListItem(
            title = type.titleRes,
            value = type.filterValue,
            onClick = { onTypeSelected(type) }
        )
    }

    val BookingType.titleRes: Int
        @StringRes get() = when (this) {
            BookingType.Any -> R.string.bookings_filter_default
            BookingType.Service -> R.string.bookings_filter_type_service
            BookingType.Event -> R.string.bookings_filter_type_event
        }

    private fun availableBookingTypes(): List<BookingType> = listOf(
        BookingType.Any,
        BookingType.Service,
        BookingType.Event,
    )
}

val BookingType.filterValue: String?
    // TODO Update this with actual endpoint values
    get() = when (this) {
        BookingType.Service -> "service"
        BookingType.Event -> "event"
        BookingType.Any -> null
    }

// TODO After refactoring navigation for the filter screens, replace this with a ViewModel-backed UI state.
val DUMMY_BOOKING_TYPE_FILTER_UI_STATE = BookingTypeFilterUiState(
    selectedType = BookingFilterType.Any,
    onTypeSelected = {}
)
