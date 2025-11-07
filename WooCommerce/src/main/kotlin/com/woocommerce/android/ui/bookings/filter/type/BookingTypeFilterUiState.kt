package com.woocommerce.android.ui.bookings.filter.type

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.filter.BookingFilterListItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.BookingType

data class BookingTypeFilterUiState(
    val selectedType: BookingType = BookingType.DEFAULT,
    val onTypeSelected: (BookingType) -> Unit = {},
) {
    val items: List<BookingFilterListItem> = availableBookingTypes().map { type ->
        BookingFilterListItem(
            title = type.titleRes,
            selected = type == selectedType,
            onClick = { onTypeSelected(type) }
        )
    }

    private fun availableBookingTypes(): List<BookingType> = listOf(
        BookingType(BookingType.Type.ANY),
        BookingType(BookingType.Type.SERVICE),
        BookingType(BookingType.Type.EVENT),
    )
}

val BookingType.titleRes: Int
    @StringRes get() = when (this.value) {
        BookingType.Type.ANY -> R.string.bookings_filter_default
        BookingType.Type.SERVICE -> R.string.bookings_filter_type_service
        BookingType.Type.EVENT -> R.string.bookings_filter_type_event
    }
