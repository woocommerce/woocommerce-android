package com.woocommerce.android.ui.bookings.filter

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.filter.type.titleRes
import com.woocommerce.android.ui.compose.DialogState
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

enum class BookingFilterPage {
    List,
    DateTime,
    TeamMember,
    AttendanceStatus,
    PaymentStatus,
    BookingType,
    Customer,
    ServiceEvent,
    Location,
}

data class BookingFilterListUiState(
    val initialBookingFilters: BookingFilters? = null,
    val newBookingFilters: Set<BookingsFilterOption> = emptySet(),
    val currentPage: BookingFilterPage = BookingFilterPage.List,
    val dialogState: DialogState? = null,
    val onClose: () -> Unit = {},
    val onShowBookings: () -> Unit = {},
    val openPage: (BookingFilterPage) -> Unit = {},
    val onUpdateFilterOption: (BookingsFilterOption) -> Unit = {}
) {

    val items: List<BookingFilterListItem> = availableBookingFilters().map { page ->
        BookingFilterListItem(
            title = page.titleRes,
            subtitle = page.filterValue,
            onClick = { openPage(page) },
        )
    }

    val currentBookingType: BookingsFilterOption.BookingType
        get() = newBookingFilters.getOrDefault<BookingsFilterOption.BookingType>(
            initialBookingFilters?.bookingType
        ) ?: BookingsFilterOption.BookingType(BookingsFilterOption.BookingType.Type.ANY)

    val updatedBookingFilters: BookingFilters
        get() {
            val initial = initialBookingFilters ?: BookingFilters()
            return BookingFilters(
                dateRange = newBookingFilters.getOrDefault(initial.dateRange),
                customer = newBookingFilters.getOrDefault(initial.customer),
                teamMember = newBookingFilters.getOrDefault(initial.teamMember),
                attendanceStatus = newBookingFilters.getOrDefault(initial.attendanceStatus),
                paymentStatus = newBookingFilters.getOrDefault(initial.paymentStatus),
                bookingType = newBookingFilters.getOrDefault(initial.bookingType),
                location = newBookingFilters.getOrDefault(initial.location),
                serviceEvent = newBookingFilters.getOrDefault(initial.serviceEvent),
            )
        }

    val updatedBookingFiltersCount = updatedBookingFilters.enabledFiltersCount

    @DrawableRes
    val navigationIcon: Int = when (currentPage) {
        BookingFilterPage.List -> R.drawable.ic_gridicons_cross_24dp
        else -> R.drawable.ic_back_24dp
    }

    val BookingFilterPage.filterValue: UiString?
        get() = when (this) {
            BookingFilterPage.Customer -> {
                newBookingFilters.getOrDefault<BookingsFilterOption.Customer>(
                    initialBookingFilters?.customer
                )?.customerName?.let { name -> UiString.UiStringText(name) }
            }

            BookingFilterPage.BookingType -> {
                newBookingFilters.getOrDefault<BookingsFilterOption.BookingType>(
                    initialBookingFilters?.bookingType
                )?.titleRes?.let { res -> UiString.UiStringRes(res) }
            }

            BookingFilterPage.DateTime,
            BookingFilterPage.Location,
            BookingFilterPage.AttendanceStatus,
            BookingFilterPage.PaymentStatus,
            BookingFilterPage.ServiceEvent,
            BookingFilterPage.TeamMember,
            BookingFilterPage.List -> null
        }

    val title: UiString
        get() = if (currentPage != BookingFilterPage.List) {
            UiString.UiStringRes(currentPage.titleRes)
        } else if (updatedBookingFiltersCount > 0) {
            UiString.UiStringRes(
                stringRes = R.string.bookings_filters_title_with_count,
                params = listOf(UiString.UiStringText(updatedBookingFiltersCount.toString()))
            )
        } else {
            UiString.UiStringRes(R.string.bookings_filters_default_title)
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
