package com.woocommerce.android.ui.bookings

import com.automattic.eventhorizon.BookingAttendanceValue
import com.automattic.eventhorizon.BookingSortValue
import com.automattic.eventhorizon.BookingTabValue
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.list.BookingListSortOption
import com.woocommerce.android.ui.bookings.list.BookingListTab

fun BookingListTab.toEventHorizonValue(): BookingTabValue = when (this) {
    BookingListTab.Today -> BookingTabValue.Today
    BookingListTab.Upcoming -> BookingTabValue.Upcoming
    BookingListTab.All -> BookingTabValue.All
}

fun BookingListSortOption.toEventHorizonValue(): BookingSortValue = when (this) {
    BookingListSortOption.NewestToOldest -> BookingSortValue.NewestFirst
    BookingListSortOption.OldestToNewest -> BookingSortValue.OldestFirst
}

fun BookingAttendanceStatus.toEventHorizonValue(): BookingAttendanceValue = when (this) {
    BookingAttendanceStatus.Attended -> BookingAttendanceValue.Attended
    BookingAttendanceStatus.Unattended -> BookingAttendanceValue.Unattended
}
