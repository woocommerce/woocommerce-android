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

/**
 * UI state for the Bookings Filters screen.
 *
 * @property initialBookingFilters The stored filters loaded from storage DataStore and shown when the screen opens.
 * @property updatedBookingFilters The in-memory working copy reflecting user changes; not persisted until the user
 * saves.
 */
data class BookingFilterListUiState(
    val initialBookingFilters: BookingFilters = BookingFilters(),
    val updatedBookingFilters: BookingFilters = initialBookingFilters,
    val currentPage: BookingFilterPage = BookingFilterPage.List,
    val dialogState: DialogState? = null,
    val onClose: () -> Unit = {},
    val onShowBookings: () -> Unit = {},
    val openPage: (BookingFilterPage) -> Unit = {},
    val onUpdateFilterOption: (BookingsFilterOption) -> Unit = {},
    val onClear: () -> Unit = {},
) {

    val items: List<BookingFilterListItem> = availableBookingFilters().map { page ->
        BookingFilterListItem(
            title = page.titleRes,
            subtitle = page.filterValue,
            onClick = { openPage(page) },
        )
    }

    val updatedBookingFiltersCount = updatedBookingFilters.enabledFiltersCount

    val showClearButton: Boolean
        get() = currentPage == BookingFilterPage.List && updatedBookingFiltersCount > 0

    @DrawableRes
    val navigationIcon: Int = when (currentPage) {
        BookingFilterPage.List -> R.drawable.ic_gridicons_cross_24dp
        else -> R.drawable.ic_back_24dp
    }

    val BookingFilterPage.filterValue: UiString?
        get() = when (this) {
            BookingFilterPage.Customer -> updatedBookingFilters.customer?.customerName?.let { name ->
                UiString.UiStringText(name)
            }

            BookingFilterPage.BookingType -> {
                updatedBookingFilters.bookingType?.titleRes?.let { UiString.UiStringRes(it) }
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
        BookingFilterPage.Customer -> R.string.bookings_filter_customer
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

/**
 * Update and return a copy of this BookingFilters by setting the field that corresponds to the type of
 * [bookingsFilterOption].
 */
fun BookingFilters.updateFilterOption(bookingsFilterOption: BookingsFilterOption): BookingFilters =
    when (bookingsFilterOption) {
        is BookingsFilterOption.DateRange -> copy(dateRange = bookingsFilterOption)
        is BookingsFilterOption.Customer -> copy(customer = bookingsFilterOption)
        is BookingsFilterOption.TeamMember -> copy(teamMember = bookingsFilterOption)
        is BookingsFilterOption.AttendanceStatus -> copy(attendanceStatus = bookingsFilterOption)
        is BookingsFilterOption.PaymentStatus -> copy(paymentStatus = bookingsFilterOption)
        is BookingsFilterOption.BookingType -> copy(bookingType = bookingsFilterOption)
        is BookingsFilterOption.Location -> copy(location = bookingsFilterOption)
        is BookingsFilterOption.ServiceEvent -> copy(serviceEvent = bookingsFilterOption)
    }
