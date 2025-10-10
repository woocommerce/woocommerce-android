package com.woocommerce.android.ui.bookings.filter

import androidx.annotation.StringRes
import com.woocommerce.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

sealed interface BookingFilterPage {
    data object List : BookingFilterPage
    data object DateTimePicker : BookingFilterPage
}

data class BookingFilterListUiState(
    val initialBookingFilters: BookingFilters? = null,
    val newBookingFilters: Set<BookingsFilterOption> = emptySet(),
    val currentPage: BookingFilterPage = BookingFilterPage.List,
    val onClose: () -> Unit = {},
    val onShowBookings: () -> Unit = {},
    val openPage: (BookingFilterPage) -> Unit = {},
) {

    val items: List<BookingFilterListItem> = initialBookingFilters.defaultBookingFilters().map { option ->
        BookingFilterListItem(
            title = option.titleRes(),
            value = option.value,
            onClick = { openPage(option.page) },
        )
    }
}

data class BookingFilterListItem(
    @StringRes val title: Int,
    val value: String? = null,
    val onClick: () -> Unit = {}
)

@StringRes
fun BookingsFilterOption.titleRes(): Int = when (this) {
    BookingsFilterOption.TeamMember -> R.string.bookings_filter_title_team_member
    BookingsFilterOption.AttendanceStatus -> R.string.bookings_filter_title_attendance_status
    BookingsFilterOption.PaymentStatus -> R.string.bookings_filter_title_payment_status
    BookingsFilterOption.BookingType -> R.string.bookings_filter_title_type
    is BookingsFilterOption.Customer -> R.string.bookings_filter_customer_name
    BookingsFilterOption.Category -> R.string.bookings_filter_category
    is BookingsFilterOption.DateRange -> R.string.bookings_filter_title_date
    BookingsFilterOption.ServiceEvent -> R.string.bookings_filter_title_service_event
}

private val BookingsFilterOption.value: String?
    get() = when (this) {
        BookingsFilterOption.TeamMember,
        BookingsFilterOption.AttendanceStatus,
        BookingsFilterOption.PaymentStatus,
        BookingsFilterOption.BookingType,
        BookingsFilterOption.Category,
        BookingsFilterOption.ServiceEvent,
        is BookingsFilterOption.Customer,
        is BookingsFilterOption.DateRange -> null
    }

private fun BookingFilters?.defaultBookingFilters(): List<BookingsFilterOption> = listOf(
    BookingsFilterOption.TeamMember,
    BookingsFilterOption.AttendanceStatus,
    BookingsFilterOption.PaymentStatus,
    BookingsFilterOption.BookingType,
    BookingsFilterOption.Customer(customerId = this?.customer?.customerId, customerName = this?.customer?.customerName),
    BookingsFilterOption.Category,
    BookingsFilterOption.DateRange(before = this?.dateRange?.before, after = this?.dateRange?.after),
    BookingsFilterOption.ServiceEvent,
)

// TODO map to new pages as we add them
private val BookingsFilterOption.page: BookingFilterPage
    get() = when (this) {
        BookingsFilterOption.TeamMember,
        BookingsFilterOption.AttendanceStatus,
        BookingsFilterOption.PaymentStatus,
        BookingsFilterOption.BookingType,
        BookingsFilterOption.Category,
        BookingsFilterOption.ServiceEvent,
        is BookingsFilterOption.Customer,
        is BookingsFilterOption.DateRange -> BookingFilterPage.DateTimePicker
    }
