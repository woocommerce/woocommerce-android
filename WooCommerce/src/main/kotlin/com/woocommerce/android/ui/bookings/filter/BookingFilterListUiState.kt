package com.woocommerce.android.ui.bookings.filter

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

sealed interface BookingFilterPage {
    data object List : BookingFilterPage
    data object DateTime : BookingFilterPage
    data object TeamMember : BookingFilterPage
    data object AttendanceStatus : BookingFilterPage
    data object PaymentStatus : BookingFilterPage
    data object BookingType : BookingFilterPage
    data object Customer : BookingFilterPage
    data object ServiceEvent : BookingFilterPage
    data object Location : BookingFilterPage
}

data class BookingFilterListUiState(
    val initialBookingFilters: BookingFilters? = null,
    val newBookingFilters: Set<BookingsFilterOption> = emptySet(),
    val currentPage: BookingFilterPage = BookingFilterPage.List,
    val onClose: () -> Unit = {},
    val onShowBookings: () -> Unit = {},
    val openPage: (BookingFilterPage) -> Unit = {},
) {

    val items: List<BookingFilterListItem> = availableBookingFilters().map { page ->
        BookingFilterListItem(
            title = page.titleRes,
            value = page.filterValue,
            onClick = { openPage(page) },
        )
    }

    @DrawableRes
    val navigationIcon: Int = when (currentPage) {
        is BookingFilterPage.List -> R.drawable.ic_gridicons_cross_24dp
        else -> R.drawable.ic_back_24dp
    }

    val BookingFilterPage.filterValue: String?
        get() = when (this) {
            BookingFilterPage.Customer -> {
                newBookingFilters.getOrDefault<BookingsFilterOption.Customer>(
                    initialBookingFilters?.customer
                )?.customerName
            }

            BookingFilterPage.DateTime,
            BookingFilterPage.Location,
            BookingFilterPage.AttendanceStatus,
            BookingFilterPage.BookingType,
            BookingFilterPage.PaymentStatus,
            BookingFilterPage.ServiceEvent,
            BookingFilterPage.TeamMember,
            BookingFilterPage.List -> null
        }
}

val BookingFilterPage.titleRes: Int
    @StringRes get() = when (this) {
        BookingFilterPage.TeamMember -> R.string.bookings_filter_title_team_member
        BookingFilterPage.AttendanceStatus -> R.string.bookings_filter_title_attendance_status
        BookingFilterPage.PaymentStatus -> R.string.bookings_filter_title_payment_status
        BookingFilterPage.BookingType -> R.string.bookings_filter_title_type
        BookingFilterPage.Customer -> R.string.bookings_filter_customer_name
        BookingFilterPage.Location -> R.string.bookings_filter_location
        BookingFilterPage.DateTime -> R.string.bookings_filter_title_date
        BookingFilterPage.ServiceEvent -> R.string.bookings_filter_title_service_event
        BookingFilterPage.List -> R.string.bookings_filters_default_title
    }

private fun availableBookingFilters(): List<BookingFilterPage> = listOf(
    BookingFilterPage.TeamMember,
    BookingFilterPage.BookingType,
    BookingFilterPage.ServiceEvent,
    BookingFilterPage.AttendanceStatus,
    BookingFilterPage.PaymentStatus,
    BookingFilterPage.Customer,
    BookingFilterPage.DateTime,
    BookingFilterPage.Location,
)

inline fun <reified T> Set<BookingsFilterOption>.getOrDefault(default: T?): T? {
    return this.filterIsInstance<T>().firstOrNull() ?: default
}
